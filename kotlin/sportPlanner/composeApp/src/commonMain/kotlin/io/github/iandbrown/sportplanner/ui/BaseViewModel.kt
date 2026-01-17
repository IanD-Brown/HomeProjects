package io.github.iandbrown.sportplanner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import io.github.iandbrown.sportplanner.database.AppDatabase
import io.github.iandbrown.sportplanner.database.BaseDao
import org.koin.java.KoinJavaComponent.inject

abstract class BaseViewModel<DAO : BaseDao<ENTITY>, ENTITY> : ViewModel {
    val dao : DAO
    internal val _uiState = MutableStateFlow(UiState<ENTITY>(true))
    val uiState = _uiState.asStateFlow()
    private val fullRead : Boolean
    private val coroutineScope = viewModelScope

    constructor(readAll : Boolean = true) {
        val database : AppDatabase by inject(AppDatabase::class.java)
        dao = getDao(database)
        fullRead = readAll
        readAll()
    }
    
    abstract fun getDao(db : AppDatabase) : DAO

    private fun readAll() {
        if (fullRead) {
            _uiState.value = UiState(isLoading = true)
            viewModelScope.launch {
                getAll().collect {
                    _uiState.value = UiState(data = it, isLoading = false)
                }
            }
        }
    }

    suspend fun update(entity : ENTITY) {
        dao.update(entity)
        readAll()
    }

    suspend fun insert(entity : ENTITY) : Long {
        val newId = dao.insert(entity)
        readAll()
        return newId
    }

    fun delete(entity : ENTITY) {
        _uiState.value = UiState(isLoading = true)
        coroutineScope.launch {
            dao.delete(entity)
            readAll()
        }
    }

    private fun getAll(): Flow<List<ENTITY>> =
        flow {
            emit(dao.getAll())
        }
}

interface BaseUiState {
    fun loadingInProgress() : Boolean
    fun hasData() : Boolean
}

class SimpleState : BaseUiState {
    override fun loadingInProgress(): Boolean = true

    override fun hasData(): Boolean = false
}

class MergedState(vararg val states : BaseUiState) : BaseUiState {
    override fun loadingInProgress(): Boolean = states.any {it.loadingInProgress()}
    override fun hasData(): Boolean = states.all { it.hasData()}
}

data class UiState<ENTITY>(
    val isLoading: Boolean,
    val data: List<ENTITY>? = null,
    val error: String? = null,
) : BaseUiState {
    override fun loadingInProgress(): Boolean = isLoading

    override fun hasData(): Boolean = data != null
}
