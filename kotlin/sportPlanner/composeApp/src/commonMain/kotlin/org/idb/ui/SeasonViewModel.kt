package org.idb.ui

import org.idb.database.AppDatabase
import org.idb.database.Season
import org.idb.database.SeasonDao

class SeasonViewModel : BaseViewModel<SeasonDao, Season>() {
    override fun getDao(db: AppDatabase): SeasonDao = db.getSeasonDao()
}