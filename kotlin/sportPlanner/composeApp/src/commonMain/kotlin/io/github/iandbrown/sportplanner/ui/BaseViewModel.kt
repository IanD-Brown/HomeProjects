package io.github.iandbrown.sportplanner.ui

import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.shivathapaa.logger.api.LoggerFactory
import io.github.iandbrown.sportplanner.database.BaseSeasonCompReadDao
import io.github.iandbrown.sportplanner.database.BaseSeasonReadDao
import io.github.iandbrown.sportplanner.database.BaseWriteDao
import io.github.iandbrown.sportplanner.database.CompetitionId
import io.github.iandbrown.sportplanner.database.ConfigReadDao
import io.github.iandbrown.sportplanner.database.ReadDao
import io.github.iandbrown.sportplanner.database.SeasonId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ViewModelState<out ENTITY> {
    data class Success<ENTITY>(val data: ImmutableList<ENTITY>) : ViewModelState<ENTITY>
    data class Error(val message: String, val reset: () -> Unit) : ViewModelState<Nothing>
    object Loading : ViewModelState<Nothing>
    object Uninitialized : ViewModelState<Nothing>

    fun values() : ImmutableList<ENTITY> =
        when (this) {
            is Success -> data
            else -> persistentListOf()
        }
}

internal fun<ENTITY> State<ViewModelState<ENTITY>>.values() : ImmutableList<ENTITY> {
    return when (val value = this.value) {
        is ViewModelState.Success -> value.data
        else -> persistentListOf()
    }
}

private class ReadDelegate<ENTITY>(val viewModelScope: CoroutineScope, val reader: suspend() -> List<ENTITY>) {
    private val _state = MutableStateFlow<ViewModelState<ENTITY>>(ViewModelState.Uninitialized)
    val uiState: StateFlow<ViewModelState<ENTITY>> = _state.asStateFlow()

    init {
        readAll()
    }

    fun readAll() {
        viewModelScope.launch {
            _state.update { ViewModelState.Loading }
            try {
                _state.update { ViewModelState.Success(reader().toImmutableList()) }
            } catch (e: Exception) {
                handleException(e)
            }
        }
    }

    fun handleException(exception: Exception) {
        logException(javaClass.simpleName, exception, "operation failed:")
        if (exception is CancellationException) {
            throw exception
        }
        _state.update { ViewModelState.Error(exception.message ?: exception.javaClass.simpleName, ::readAll) }
    }
}

fun logException(className: String, exception: Exception, context: String) {
    val logger = LoggerFactory.get(className)
    logger.error(exception) { "$context ${exception.message}" }
}


open class BaseConfigReadViewModel<DAO, ENTITY>(val dao: DAO) : ViewModel() where DAO : ConfigReadDao<ENTITY> {
    private val readDelegate = ReadDelegate(viewModelScope) { dao.get() }

    fun getState() : StateFlow<ViewModelState<ENTITY>> = readDelegate.uiState

    fun readAll() = readDelegate.readAll()

    fun handleException(exception: Exception) = readDelegate.handleException(exception)
}

open class BaseSeasonReadViewModel<DAO, ENTITY>(private var seasonId: SeasonId, val dao: DAO) : ViewModel()
        where DAO : BaseSeasonReadDao<ENTITY> {
    private val readDelegate = ReadDelegate(viewModelScope) { dao.get(seasonId) }

    fun getState() : StateFlow<ViewModelState<ENTITY>> = readDelegate.uiState
}

open class BaseSeasonCompReadViewModel<DAO, ENTITY>(private val seasonId: SeasonId,
                                                    private val competitionId: CompetitionId,
                                                    val dao: DAO) : ViewModel()
        where DAO : BaseSeasonCompReadDao<ENTITY> {
    private val readDelegate = ReadDelegate(viewModelScope) { dao.get(seasonId, competitionId) }

    fun getState() : StateFlow<ViewModelState<ENTITY>> = readDelegate.uiState
}

open class BaseConfigCRUDViewModel<DAO, ENTITY>(dao : DAO) : BaseCRUDViewModel<DAO, ENTITY>(dao, { it.get() })
        where DAO : ConfigReadDao<ENTITY>, DAO : BaseWriteDao<ENTITY>

open class BaseSeasonCompCRUDViewModel<DAO, ENTITY>(val seasonId : SeasonId,
                                                    val competitionId : CompetitionId,
                                                    dao : DAO) : BaseCRUDViewModel<DAO, ENTITY>(dao, {it.get(seasonId, competitionId)})
        where DAO : BaseSeasonCompReadDao<ENTITY>, DAO : BaseWriteDao<ENTITY>

open class BaseSeasonCRUDViewModel<DAO, ENTITY>(val seasonId : SeasonId, dao : DAO) : BaseCRUDViewModel<DAO, ENTITY>(dao, {it.get(seasonId)})
        where DAO : BaseSeasonReadDao<ENTITY>, DAO : BaseWriteDao<ENTITY>

abstract class BaseCRUDViewModel<DAO, ENTITY>(val dao : DAO, reader: suspend (DAO) -> List<ENTITY>) : ViewModel()
        where DAO : ReadDao<ENTITY>, DAO : BaseWriteDao<ENTITY> {
    private val readDelegate = ReadDelegate(viewModelScope) {reader(dao)}

    fun getState() : StateFlow<ViewModelState<ENTITY>> = readDelegate.uiState

    fun readAll() = readDelegate.readAll()

    fun insert(entity: ENTITY) {
        runInCoroutine { dao.insert(entity) }
    }

    fun update(entity: ENTITY) {
       runInCoroutine {  dao.update(entity) }
    }

    fun delete(entity: ENTITY) {
        runInCoroutine { dao.delete(entity) }
    }

    private fun runInCoroutine(operation: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                operation()
                readAll()
            } catch (e: Exception) {
                handleException(e)
            }
        }
    }

    fun handleException(exception: Exception) = readDelegate.handleException(exception)
}
