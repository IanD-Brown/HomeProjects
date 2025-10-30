package org.idb.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import org.idb.database.AppDatabase
import org.idb.database.Association
import org.idb.database.AssociationDao
import org.koin.java.KoinJavaComponent.inject

class AssociationViewModel : ViewModel() {
    private val database : AppDatabase by inject(AppDatabase::class.java)
    private val _associations = MutableStateFlow(AssociationUiState(true))
    val associations = _associations.asStateFlow()

    init {
        readAll()
    }

    private fun dao() : AssociationDao = database.getAssociationDao()

    private fun readAll() {
        _associations.value = AssociationUiState(isLoading = true)
        viewModelScope.launch {
            getAll().collect {
                _associations.value = AssociationUiState(data = it, isLoading = false)
            }
        }
    }

    suspend fun rename(oldName : String, newName : String) {
        dao().rename(oldName, newName)
        readAll()
    }

    suspend fun insert(association : Association) {
        dao().insert(association)
        readAll()
    }

    suspend fun delete(association : Association) {
        dao().delete(association)
        readAll()
    }

    private suspend fun getAll(): Flow<List<Association>> {
        if (dao().count() == 0) {
            for (name in listOf("DONCASTER", "ROTHERHAM", " BARNSLEY", " LEEDS", " EAST RIDING", " SELBY", " HUDDERSFIELD")) {
                dao().insert(Association(name = name))
            }

        }

        return flow {
            emit(dao().getAll())
        }
    }
}

data class AssociationUiState(
    val isLoading: Boolean,
    val data: List<Association>? = null,
    val error: String? = null,
)