package io.github.iandbrown.reconciler.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.shivathapaa.logger.api.LoggerFactory
import io.github.iandbrown.reconciler.database.BaseReadDao
import io.github.iandbrown.reconciler.database.BaseWriteDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ViewModelState<out ENTITY> {
    data class Success<ENTITY>(val data: List<ENTITY>) : ViewModelState<ENTITY>
    data class Error(val message: String, val reset: () -> Unit) : ViewModelState<Nothing>
    object Loading : ViewModelState<Nothing>
    object Uninitialized : ViewModelState<Nothing>

    fun values() : List<ENTITY> =
        when (this) {
            is Success -> data
            else -> emptyList()
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
                dao.get().collect {data -> _state.update { ViewModelState.Success(data) } }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    protected fun handleError(exception: Exception) {
        val logger = LoggerFactory.get(javaClass.simpleName)
        logger.error(exception) {"operation failed: ${exception.message}"}
        _state.update { ViewModelState.Error(exception.message ?: exception.javaClass.simpleName, ::readAll) }
    }
}

open class BaseConfigCRUDViewModel<DAO, ENTITY>(dao : DAO) : BaseReadViewModel<DAO, ENTITY>(dao)
        where DAO : BaseReadDao<ENTITY>, DAO : BaseWriteDao<ENTITY>
{
    val coroutineScope = viewModelScope

    fun insert(entity: ENTITY) : Long {
        var result = 0L
        coroutineScope.launch {
            result = dao.insert(entity)
            readAll()
        }
        return result
    }

    fun update(entity: ENTITY) {
        coroutineScope.launch {
            dao.update(entity)
            readAll()
        }
    }

    fun delete(entity: ENTITY) {
        coroutineScope.launch {
            dao.delete(entity)
            readAll()
        }
    }
}
