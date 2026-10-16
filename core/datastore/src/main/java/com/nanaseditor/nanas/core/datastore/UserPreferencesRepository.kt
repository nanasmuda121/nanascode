package com.nanaseditor.nanas.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nanas_user_prefs")

enum class AppThemeMode {
    NANAS_DARK,
    AMOLED,
    LIGHT
}

data class UserPreferences(
    val themeMode: AppThemeMode,
    val fontSize: Float,
    val language: String
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val KEY_THEME = stringPreferencesKey("app_theme")
    private val KEY_FONT_SIZE = floatPreferencesKey("editor_font_size")
    private val KEY_LANGUAGE = stringPreferencesKey("app_language")

    val preferencesFlow: Flow<UserPreferences> = dataStore.data.map { prefs ->
        val themeStr = prefs[KEY_THEME] ?: AppThemeMode.NANAS_DARK.name
        val theme = try {
            AppThemeMode.valueOf(themeStr)
        } catch (e: Exception) {
            AppThemeMode.NANAS_DARK
        }
        val fontSize = prefs[KEY_FONT_SIZE] ?: 14.0f
        val language = prefs[KEY_LANGUAGE] ?: "id"

        UserPreferences(theme, fontSize, language)
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        dataStore.edit { it[KEY_THEME] = mode.name }
    }

    suspend fun setFontSize(size: Float) {
        dataStore.edit { it[KEY_FONT_SIZE] = size }
    }

    suspend fun setLanguage(lang: String) {
        dataStore.edit { it[KEY_LANGUAGE] = lang }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }
}
