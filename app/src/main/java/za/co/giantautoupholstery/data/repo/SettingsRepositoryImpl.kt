package za.co.giantautoupholstery.data.repo

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import za.co.giantautoupholstery.domain.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "device_settings")

/** Device-level preference (survives logout so the guest experience keeps the chosen theme). Holds no credentials. */
@Singleton
class SettingsRepositoryImpl @Inject constructor(@ApplicationContext private val context: Context) : SettingsRepository {
    private val THEME = stringPreferencesKey("theme_mode")
    override val themeMode: Flow<String> = context.dataStore.data.map { it[THEME] ?: "system" }
    override suspend fun setThemeMode(mode: String) { context.dataStore.edit { it[THEME] = mode } }
}
