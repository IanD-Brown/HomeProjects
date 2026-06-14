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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

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

abstract class BaseReadViewModel<DAO: ReadDao<ENTITY>, ENTITY>(val dao: DAO, val reader: suspend (DAO) -> List<ENTITY> ) : ViewModel() {
    private val _state = MutableStateFlow<ViewModelState<ENTITY>>(ViewModelState.Uninitialized)
    val uiState: StateFlow<ViewModelState<ENTITY>> = _state.asStateFlow()

    init {
        readAll()
    }

    fun readAll() {
        viewModelScope.launch {
            _state.update { ViewModelState.Loading }
            try {
                _state.update { ViewModelState.Success(reader(dao).toImmutableList()) }
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


open class BaseConfigReadViewModel<DAO, ENTITY>(dao: DAO) : BaseReadViewModel<DAO, ENTITY>(dao, {it.get()})
        where DAO : ConfigReadDao<ENTITY>

open class BaseSeasonReadViewModel<DAO, ENTITY>(private var seasonId: SeasonId, dao: DAO) : BaseReadViewModel<DAO, ENTITY>(dao, {it.get(seasonId)})
        where DAO : BaseSeasonReadDao<ENTITY>

open class BaseSeasonCompReadViewModel<DAO, ENTITY>(private val seasonId: SeasonId,
                                                    private val competitionId: CompetitionId,
                                                    dao: DAO) : BaseReadViewModel<DAO, ENTITY>(dao, {it.get(seasonId, competitionId)})
        where DAO : BaseSeasonCompReadDao<ENTITY>

open class BaseConfigCRUDViewModel<DAO, ENTITY>(dao : DAO) : BaseCRUDViewModel<DAO, ENTITY>(dao, {it.get()})
        where DAO : ConfigReadDao<ENTITY>, DAO : BaseWriteDao<ENTITY>

open class BaseSeasonCompCRUDViewModel<DAO, ENTITY>(val seasonId : SeasonId,
                                                    val competitionId : CompetitionId,
                                                    dao : DAO) : BaseCRUDViewModel<DAO, ENTITY>(dao, {it.get(seasonId, competitionId)})
        where DAO : BaseSeasonCompReadDao<ENTITY>, DAO : BaseWriteDao<ENTITY>

open class BaseSeasonCRUDViewModel<DAO, ENTITY>(val seasonId : SeasonId, dao : DAO) : BaseCRUDViewModel<DAO, ENTITY>(dao, {it.get(seasonId)})
        where DAO : BaseSeasonReadDao<ENTITY>, DAO : BaseWriteDao<ENTITY>

abstract class BaseCRUDViewModel<DAO, ENTITY>(dao : DAO, reader: suspend (DAO) -> List<ENTITY>) : BaseReadViewModel<DAO, ENTITY>(dao, reader)
        where DAO : ReadDao<ENTITY>, DAO : BaseWriteDao<ENTITY> {

    fun insert(entity: ENTITY) : Long {
        var result = 0L
        viewModelScope.launch {
            try {
                result = dao.insert(entity)
                readAll()
            } catch (e: Exception) {
                handleException(e)
            }
        }
        return result
    }

    fun update(entity: ENTITY) {
        viewModelScope.launch {
            try {
                dao.update(entity)
                readAll()
            } catch (e: Exception) {
                handleException(e)
            }
        }
    }

    fun delete(entity: ENTITY) {
        viewModelScope.launch {
            try {
                dao.delete(entity)
                readAll()
            } catch (e: Exception) {
                handleException(e)
            }
        }
    }
}
