package com.clearout.app.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.*
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.clearout.app.MainActivity

// DataStore keys — must match GamificationDataStore.Keys exactly
private val KEY_STREAK = intPreferencesKey("streak")
private val KEY_TOTAL_DELETED = intPreferencesKey("total_deleted")
private val KEY_TOTAL_BYTES = longPreferencesKey("total_bytes_freed")

/**
 * Large (4x2) ClearOut Home Screen Widget.
 *
 * Displays:
 *   - "ClearOut" wordmark (orange accent)
 *   - Used storage percentage ring-style progress bar
 *   - Current streak with 🔥 flame emoji
 *   - Total photos cleared
 *   - Tap anywhere → opens MainActivity
 *
 * Reads gamification data from the same DataStore used by the app,
 * so values are always in sync without any extra network calls.
 */
class ClearoutLargeWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val usedPercent = getUsedStoragePercent()

        provideContent {
            val prefs = currentState<Preferences>()
            val streak = prefs[KEY_STREAK] ?: 0
            val totalDeleted = prefs[KEY_TOTAL_DELETED] ?: 0
            val totalBytes = prefs[KEY_TOTAL_BYTES] ?: 0L
            val mbFreed = (totalBytes / 1_048_576.0).toInt()

            LargeWidgetContent(
                usedPercent = usedPercent,
                streak = streak,
                totalDeleted = totalDeleted,
                mbFreed = mbFreed
            )
        }
    }
}

@Composable
private fun LargeWidgetContent(
    usedPercent: Int,
    streak: Int,
    totalDeleted: Int,
    mbFreed: Int
) {
    val orange = ColorProvider(Color(0xFFFF5C1A))
    val white = ColorProvider(Color.White)
    val muted = ColorProvider(Color(0xFF9B9187))
    val border = ColorProvider(Color(0xFF2E2720))
    val ink = ColorProvider(Color(0xFF1E1C1A))

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ink)
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // ── Header: Wordmark + "Tap to clean" hint ──
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    Text(
                        text = "Clear",
                        style = TextStyle(color = white, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "out",
                        style = TextStyle(color = orange, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    text = "Tap to clean →",
                    style = TextStyle(color = muted, fontSize = 11.sp)
                )
            }

            Spacer(GlanceModifier.height(12.dp))

            // ── Main stats row ──
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Storage percentage — large number
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "$usedPercent%",
                        style = TextStyle(color = orange, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "storage used",
                        style = TextStyle(color = muted, fontSize = 11.sp)
                    )
                }

                // Vertical divider
                Box(
                    modifier = GlanceModifier
                        .width(1.dp)
                        .height(48.dp)
                        .background(border)
                ) { }
                Spacer(GlanceModifier.width(12.dp))

                // Streak column
                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (streak > 0) "🔥 ${streak}d" else "🔥 0d",
                        style = TextStyle(color = white, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "streak",
                        style = TextStyle(color = muted, fontSize = 11.sp)
                    )
                }

                Spacer(GlanceModifier.width(12.dp))

                // Photos cleared column
                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$totalDeleted",
                        style = TextStyle(color = white, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "cleared",
                        style = TextStyle(color = muted, fontSize = 11.sp)
                    )
                }
            }

            Spacer(GlanceModifier.height(10.dp))

            // ── Footer: MB freed ──
            Text(
                text = if (mbFreed > 0) "${mbFreed} MB freed total" else "Start swiping to free space!",
                style = TextStyle(color = muted, fontSize = 11.sp)
            )
        }
    }
}
