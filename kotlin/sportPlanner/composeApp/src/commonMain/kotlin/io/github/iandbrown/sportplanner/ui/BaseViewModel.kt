package io.github.iandbrown.sportplanner.ui

import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.shivathapaa.logger.api.LoggerFactory
import io.github.iandbrown.sportplanner.database.BaseWriteDao
import io.github.iandbrown.sportplanner.database.ReadDao
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

open class BaseReadViewModel<DAO, ENTITY>(val dao : DAO, reader: suspend (DAO) -> List<ENTITY>) : ViewModel() where DAO : ReadDao<ENTITY> {
    private val readDelegate = ReadDelegate(viewModelScope) {reader(dao)}

    fun getState() : StateFlow<ViewModelState<ENTITY>> = readDelegate.uiState

    fun readAll() = readDelegate.readAll()

    fun handleException(exception: Exception) = readDelegate.handleException(exception)
}

open class BaseCRUDViewModel<DAO, ENTITY>(dao : DAO, reader: suspend (DAO) -> List<ENTITY>) : BaseReadViewModel<DAO, ENTITY>(dao, reader)
        where DAO : ReadDao<ENTITY>, DAO : BaseWriteDao<ENTITY> {
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
}
