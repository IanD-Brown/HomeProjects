package org.idb.ui

import org.idb.database.AppDatabase
import org.idb.database.Association
import org.idb.database.AssociationDao

class AssociationViewModel : BaseViewModel<AssociationDao, Association>() {
    override fun getDao(db: AppDatabase): AssociationDao = db.getAssociationDao()
}