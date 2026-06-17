package io.github.iandbrown.reconciler.ui

import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.shivathapaa.logger.api.LoggerFactory
import io.github.iandbrown.reconciler.database.BaseReadDao
import io.github.iandbrown.reconciler.database.BaseWriteDao
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
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

open class BaseReadViewModel<DAO : BaseReadDao<ENTITY>, ENTITY>(val dao: DAO) : ViewModel() {
    private val _state = MutableStateFlow<ViewModelState<ENTITY>>(ViewModelState.Uninitialized)
    val uiState: StateFlow<ViewModelState<ENTITY>> = _state.asStateFlow()

    init {
        readAll()
    }

    fun readAll() {
        viewModelScope.launch {
            _state.update { ViewModelState.Loading }
            try {
                _state.update { ViewModelState.Success(dao.get().toImmutableList()) }
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

open class BaseConfigCRUDViewModel<DAO, ENTITY>(dao : DAO) : BaseReadViewModel<DAO, ENTITY>(dao)
        where DAO : BaseReadDao<ENTITY>, DAO : BaseWriteDao<ENTITY> {
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
