package io.github.iandbrown.sportplanner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.iandbrown.sportplanner.database.AppDatabase
import io.github.iandbrown.sportplanner.database.ReadonlyDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject

abstract class ReadonlyViewModel<DAO : ReadonlyDao<ENTITY>, ENTITY> : ViewModel {
    private val dao : DAO
    private val _uiState = MutableStateFlow(UiState<ENTITY>(true))
    val uiState = _uiState.asStateFlow()

    constructor() {
        val database : AppDatabase by inject(AppDatabase::class.java)
        dao = getDao(database)
        viewModelScope.launch {
            flow {
                emit(dao.getAll())
            }.collect {
                _uiState.value = UiState(data = it, isLoading = false)
            }
        }
    }

    abstract fun getDao(db : AppDatabase) : DAO
}
