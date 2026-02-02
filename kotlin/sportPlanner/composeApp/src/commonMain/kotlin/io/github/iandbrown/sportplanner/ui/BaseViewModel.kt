package io.github.iandbrown.sportplanner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.iandbrown.sportplanner.database.BaseReadDao
import io.github.iandbrown.sportplanner.database.BaseSeasonCompReadDao
import io.github.iandbrown.sportplanner.database.BaseSeasonReadDao
import io.github.iandbrown.sportplanner.database.BaseWriteDao
import io.github.iandbrown.sportplanner.database.CompetitionId
import io.github.iandbrown.sportplanner.database.SeasonId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

open class BaseReadViewModel<DAO : BaseReadDao<ENTITY>, ENTITY>(val dao: DAO) : ViewModel() {
    val uiState = read()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun read() : Flow<List<ENTITY>> = dao.get()
}


open class BaseSeasonReadViewModel<DAO : BaseSeasonReadDao<ENTITY>, ENTITY>(private var seasonId: SeasonId, private val dao: DAO) : ViewModel() {
    var uiState : Flow<List<ENTITY>> = read()

    private fun read() : Flow<List<ENTITY>> =
        dao.get(seasonId)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )
}

open class BaseSeasonCompReadViewModel<DAO : BaseSeasonCompReadDao<ENTITY>, ENTITY> : ViewModel {
    private val _seasonId : SeasonId
    private val _competitionId : CompetitionId
    protected val dao : DAO
    var uiState : Flow<List<ENTITY>>

    constructor(seasonId: SeasonId, competitionId: CompetitionId, dataAccessObject: DAO) {
        _seasonId = seasonId
        _competitionId = competitionId
        dao = dataAccessObject
        uiState = read()
    }

    private fun read() : Flow<List<ENTITY>> =
        dao.get(_seasonId, _competitionId)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )
}

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

abstract class BaseSeasonCompCRUDViewModel<DAO, ENTITY>(val seasonId : SeasonId, val competitionId : CompetitionId, dao : DAO) : BaseCRUDViewModel<DAO, ENTITY>(dao)
        where DAO : BaseSeasonCompReadDao<ENTITY>, DAO : BaseWriteDao<ENTITY>
{
    val uiState: StateFlow<List<ENTITY>> = read()

    override fun read(): StateFlow<List<ENTITY>> =
        dao.get(seasonId, competitionId)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )
}

abstract class BaseSeasonCRUDViewModel<DAO, ENTITY>(val seasonId : SeasonId, dao : DAO) : BaseCRUDViewModel<DAO, ENTITY>(dao)
        where DAO : BaseSeasonReadDao<ENTITY>, DAO : BaseWriteDao<ENTITY>
{
    val uiState: StateFlow<List<ENTITY>> = read()

    override fun read(): StateFlow<List<ENTITY>> =
        dao.get(seasonId)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )
}

abstract class BaseCRUDViewModel<DAO: BaseWriteDao<ENTITY>, ENTITY>(protected val dao : DAO) : ViewModel() {
    protected abstract fun read(): StateFlow<List<ENTITY>>

    fun insert(entity: ENTITY) : Long {
        var result = 0L
        viewModelScope.launch {
            result = dao.insert(entity)
            read()
        }
        return result
    }

    fun update(entity: ENTITY) {
        viewModelScope.launch {
            dao.update(entity)
            read()
        }
    }

    fun delete(entity: ENTITY) {
        viewModelScope.launch {
            dao.delete(entity)
            read()
        }
    }
}
