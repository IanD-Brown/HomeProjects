package io.github.iandbrown.sportplanner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.iandbrown.sportplanner.database.AppDatabase
import io.github.iandbrown.sportplanner.database.BaseSeasonDao
import kotlin.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject

abstract class BaseSeasonViewModel<SEASON_DAO : BaseSeasonDao<SEASON_ENTITY>, SEASON_ENTITY> : ViewModel  {
    val dao : SEASON_DAO
    private val _uiState = MutableStateFlow(UiState<SEASON_ENTITY>(true))
    val uiState = _uiState.asStateFlow()
    private val coroutineScope = viewModelScope
    private val _seasonId : Short

    constructor(seasonId : Short) {
        _seasonId = seasonId
        val database : AppDatabase by inject(AppDatabase::class.java)
        dao = getDao(database)
        readAll()
    }

    private fun readAll() {
        _uiState.value = UiState(isLoading = true)
        viewModelScope.launch {
            flow {
                emit(dao.get(_seasonId))
            }.collect {
                _uiState.value = UiState(data = it, isLoading = false)
            }
        }
    }

    abstract fun getDao(db : AppDatabase) : SEASON_DAO

    fun insert(entity : SEASON_ENTITY) {
        coroutineScope.launch {
            dao.insert(entity)
            readAll()
        }
    }

    fun update(entity : SEASON_ENTITY) {
        coroutineScope.launch {
            dao.update(entity)
            readAll()
        }
    }

    fun delete(entity : SEASON_ENTITY) {
        coroutineScope.launch {
            dao.delete(entity)
            readAll()
        }
    }
}
