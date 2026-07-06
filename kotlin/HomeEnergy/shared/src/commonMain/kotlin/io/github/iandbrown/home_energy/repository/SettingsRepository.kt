package io.github.iandbrown.home_energy.repository

import io.github.iandbrown.home_energy.database.Setting
import io.github.iandbrown.home_energy.database.SettingDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsRepository(
    private val settingDao: SettingDao,
    private val scope: CoroutineScope
) {
    private val _settings = MutableStateFlow<Setting?>(null)
    val settings: StateFlow<Setting?> = _settings.asStateFlow()

    init {
        scope.launch(Dispatchers.Default) {
            refresh()
        }
    }

    suspend fun refresh() {
        val list = settingDao.get()
        _settings.value = list.firstOrNull()
    }

    suspend fun save(setting: Setting) {
        settingDao.insert(setting)
        _settings.value = setting
    }
}
