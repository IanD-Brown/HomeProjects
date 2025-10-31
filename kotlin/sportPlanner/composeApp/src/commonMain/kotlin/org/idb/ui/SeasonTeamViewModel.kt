package org.idb.ui

import org.idb.database.AppDatabase
import org.idb.database.SeasonTeam
import org.idb.database.SeasonTeamDao

class SeasonTeamViewModel : BaseViewModel<SeasonTeamDao, SeasonTeam>() {
    override fun getDao(db: AppDatabase): SeasonTeamDao = db.getSeasonTeamDao()
}