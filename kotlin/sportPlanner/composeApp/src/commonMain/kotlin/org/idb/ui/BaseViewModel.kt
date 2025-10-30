package org.idb.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import org.idb.database.AppDatabase
import org.idb.database.BaseDao
import org.koin.java.KoinJavaComponent.inject

abstract class BaseViewModel<DAO : BaseDao<ENTITY>, ENTITY> : ViewModel {
    private val dao : DAO
    private val _uiState = MutableStateFlow(UiState<ENTITY>(true))
    val uiState = _uiState.asStateFlow()

    constructor() {
        val database : AppDatabase by inject(AppDatabase::class.java)
        dao = getDao(database)
        readAll()
    }
    
    abstract fun getDao(db : AppDatabase) : DAO

    private fun readAll() {
        _uiState.value = UiState(isLoading = true)
        viewModelScope.launch {
            getAll().collect {
                _uiState.value = UiState(data = it, isLoading = false)
            }
        }
    }

    suspend fun update(entity : ENTITY) {
        dao.update(entity)
        readAll()
    }

    suspend fun insert(entity : ENTITY) {
        dao.insert(entity)
        readAll()
    }

    suspend fun delete(entity : ENTITY) {
        dao.delete(entity)
        readAll()
    }

    private fun getAll(): Flow<List<ENTITY>> =
        flow {
            emit(dao.getAll())
        }

}

data class UiState<ENTITY>(
    val isLoading: Boolean,
    val data: List<ENTITY>? = null,
    val error: String? = null,
)
