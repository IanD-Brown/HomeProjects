package io.github.iandbrown.reconciler.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.iandbrown.reconciler.database.BaseReadDao
import io.github.iandbrown.reconciler.database.BaseWriteDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

open class BaseConfigCRUDViewModel<DAO, ENTITY>(dao : DAO) : BaseCRUDViewModel<DAO, ENTITY>(dao)
        where DAO : BaseReadDao<ENTITY>, DAO : BaseWriteDao<ENTITY>
{
    var uiState: StateFlow<List<ENTITY>> = read()

    override fun read(): StateFlow<List<ENTITY>> = dao.get()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

}

abstract class BaseCRUDViewModel<DAO: BaseWriteDao<ENTITY>, ENTITY>(protected val dao : DAO) : ViewModel() {
    protected abstract fun read(): StateFlow<List<ENTITY>>
    val coroutineScope = viewModelScope

    fun insert(entity: ENTITY) : Long {
        var result = 0L
        coroutineScope.launch {
            result = dao.insert(entity)
            read()
        }
        return result
    }

    fun update(entity: ENTITY) {
        coroutineScope.launch {
            dao.update(entity)
            read()
        }
    }

    fun delete(entity: ENTITY) {
        coroutineScope.launch {
            dao.delete(entity)
            read()
        }
    }
}
