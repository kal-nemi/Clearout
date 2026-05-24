package com.clearout.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

data class GamificationState(
    val streak: Int,
    val xpTotal: Int,
    val totalDeleted: Int,
    val totalBytesFreed: Long,
    val onboardingCompleted: Boolean
) {
    val levelName: String
        get() = when {
            xpTotal < 100 -> "Hoarder"
            xpTotal < 500 -> "Casual Cleaner"
            xpTotal < 1500 -> "Space Purger"
            xpTotal < 4000 -> "Minimalist"
            xpTotal < 10000 -> "Zen Master"
            else -> "Digital Monk"
        }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gamification")

@Singleton
class GamificationDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    private object Keys {
        val STREAK = intPreferencesKey("streak")
        val XP_TOTAL = intPreferencesKey("xp_total")
        val LAST_ACTIVE = longPreferencesKey("last_active_date")
        val TOTAL_DELETED = intPreferencesKey("total_deleted")
        val TOTAL_BYTES = longPreferencesKey("total_bytes_freed")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val gamificationFlow: Flow<GamificationState> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            GamificationState(
                streak = prefs[Keys.STREAK] ?: 0,
                xpTotal = prefs[Keys.XP_TOTAL] ?: 0,
                totalDeleted = prefs[Keys.TOTAL_DELETED] ?: 0,
                totalBytesFreed = prefs[Keys.TOTAL_BYTES] ?: 0L,
                onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false
            )
        }

    suspend fun recordSession(deletedCount: Int, bytesFreed: Long) {
        dataStore.edit { prefs ->
            val today = LocalDate.now().toEpochDay()
            val lastActive = prefs[Keys.LAST_ACTIVE] ?: 0L
            val currentStreak = prefs[Keys.STREAK] ?: 0

            // Streak calculations
            val newStreak = when (today - lastActive) {
                0L -> currentStreak          // Same day, streak doesn't increase but is preserved
                1L -> currentStreak + 1      // Consecutive day, increment streak
                else -> 1                     // Broken streak, reset to 1
            }

            // XP calculations
            // Formula: +1 XP per deletion +5 XP per MB freed + 50 XP streak bonus if day is consecutive
            val mbFreed = bytesFreed / 1_048_576.0
            val xpGained = deletedCount + (mbFreed * 5).toInt() + (if (newStreak > currentStreak) 50 else 0)

            prefs[Keys.STREAK] = newStreak
            prefs[Keys.LAST_ACTIVE] = today
            prefs[Keys.XP_TOTAL] = (prefs[Keys.XP_TOTAL] ?: 0) + xpGained
            prefs[Keys.TOTAL_DELETED] = (prefs[Keys.TOTAL_DELETED] ?: 0) + deletedCount
            prefs[Keys.TOTAL_BYTES] = (prefs[Keys.TOTAL_BYTES] ?: 0L) + bytesFreed
        }
    }

    suspend fun completeOnboarding() {
        dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETED] = true
        }
    }

    /**
     * Resets all gamification stats (streak, XP, totals) but preserves onboarding state.
     * Called from the Settings overflow menu on the Home screen.
     */
    suspend fun resetGamification() {
        dataStore.edit { prefs ->
            prefs[Keys.STREAK] = 0
            prefs[Keys.XP_TOTAL] = 0
            prefs[Keys.LAST_ACTIVE] = 0L
            prefs[Keys.TOTAL_DELETED] = 0
            prefs[Keys.TOTAL_BYTES] = 0L
            // Intentionally NOT resetting ONBOARDING_COMPLETED
        }
    }
}
