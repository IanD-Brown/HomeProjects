package org.idb.ui

import org.idb.database.AppDatabase
import org.idb.database.SeasonCompetition
import org.idb.database.SeasonCompetitionDao

class SeasonCompetitionViewModel: BaseViewModel<SeasonCompetitionDao, SeasonCompetition>() {
    override fun getDao(db: AppDatabase): SeasonCompetitionDao = db.getSeasonCompetitionDao()
}