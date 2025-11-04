package org.idb.ui

import org.idb.database.AppDatabase
import org.idb.database.Competition
import org.idb.database.CompetitionDao

class CompetitionViewModel : BaseViewModel<CompetitionDao, Competition>() {
    override fun getDao(db: AppDatabase): CompetitionDao = db.getCompetitionDao()
}