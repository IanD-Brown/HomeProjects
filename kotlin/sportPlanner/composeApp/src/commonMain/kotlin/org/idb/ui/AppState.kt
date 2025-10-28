package org.idb.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.softartdev.theme.pref.LocalThemePrefs
import com.softartdev.theme.pref.PreferableMaterialTheme.themePrefs
import com.softartdev.theme.pref.ThemeEnum
import com.softartdev.theme.pref.ThemePrefs

object AppState {
    val isDarkTheme: Boolean
        @Composable
        @ReadOnlyComposable
        get() = when (themePrefs.darkThemeState.value) {
            ThemeEnum.Light -> false
            ThemeEnum.Dark -> true
            ThemeEnum.SystemDefault -> isSystemInDarkTheme()
        }

    val switchThemeCallback: () -> Unit
        @Composable
        @ReadOnlyComposable
        get() {
            val currentIsDark: Boolean = isDarkTheme
            val themePrefs: ThemePrefs = LocalThemePrefs.current
            val darkThemeState = themePrefs.darkThemeState
            val prefHelper = themePrefs.preferenceHelper
            return {
                darkThemeState.value = if (currentIsDark) ThemeEnum.Light else ThemeEnum.Dark
                prefHelper.themeEnum = darkThemeState.value
            }
        }

}