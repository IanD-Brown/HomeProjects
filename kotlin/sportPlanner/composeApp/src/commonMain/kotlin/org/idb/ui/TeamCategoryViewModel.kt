package org.idb.ui

import org.idb.database.AppDatabase
import org.idb.database.TeamCategory
import org.idb.database.TeamCategoryDao

class TeamCategoryViewModel : BaseViewModel<TeamCategoryDao, TeamCategory>() {
    override fun getDao(db: AppDatabase): TeamCategoryDao = db.getTeamCategoryDao()
}