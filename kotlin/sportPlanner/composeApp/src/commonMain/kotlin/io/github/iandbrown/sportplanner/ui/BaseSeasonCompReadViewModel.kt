package io.github.iandbrown.sportplanner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.iandbrown.sportplanner.database.AppDatabase
import io.github.iandbrown.sportplanner.database.BaseSeasonCompReadDao
import io.github.iandbrown.sportplanner.database.CompetitionId
import io.github.iandbrown.sportplanner.database.SeasonId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject

abstract class BaseSeasonCompReadViewModel<DAO : BaseSeasonCompReadDao<ENTITY>, ENTITY> : ViewModel {
    val dao : DAO
    private val _uiState = MutableStateFlow(UiState<ENTITY>(true))
    val uiState = _uiState.asStateFlow()
    private val coroutineScope = viewModelScope
    private val _seasonId : SeasonId
    private val _competitionId : CompetitionId

    constructor(seasonId : SeasonId, competitionId : CompetitionId) {
        _seasonId = seasonId
        _competitionId = competitionId
        val database : AppDatabase by inject(AppDatabase::class.java)
        dao = getDao(database)
        readAll()
    }

    private fun readAll() {
        _uiState.value = UiState(isLoading = true)
        coroutineScope.launch {
            flow {
                emit(dao.get(_seasonId, _competitionId))
            }.collect {
                _uiState.value = UiState(data = it, isLoading = false)
            }
        }
    }

    abstract fun getDao(db : AppDatabase) : DAO
}
