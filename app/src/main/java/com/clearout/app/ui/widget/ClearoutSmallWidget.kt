package com.clearout.app.ui.widget

import android.content.Context
import android.os.Environment
import android.os.StatFs
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
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

// DataStore key constants (must match GamificationDataStore.Keys)
private val KEY_TOTAL_BYTES = longPreferencesKey("total_bytes_freed")

/**
 * Small (2x1) ClearOut Home Screen Widget.
 *
 * Displays:
 *   - App name + orange accent
 *   - Used storage percentage (read live from StatFs)
 *   - Tap anywhere → opens MainActivity (app launches directly)
 *
 * Uses Jetpack Glance for modern Compose-based widget rendering.
 * Data is refreshed every 30 minutes via the updatePeriodMillis in widget XML.
 */
class ClearoutSmallWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Read storage from StatFs (this runs in a coroutine, so it is safe)
        val usedPercent = getUsedStoragePercent()

        provideContent {
            SmallWidgetContent(usedPercent = usedPercent)
        }
    }
}

@Composable
private fun SmallWidgetContent(usedPercent: Int) {
    val orange = ColorProvider(Color(0xFFFF5C1A))
    val white = ColorProvider(Color.White)
    val dark = ColorProvider(Color(0xFF1A1410))
    val muted = ColorProvider(Color(0xFF9B9187))

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFF1E1C1A)))
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App name row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Clear",
                    style = TextStyle(
                        color = white,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "out",
                    style = TextStyle(
                        color = orange,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(GlanceModifier.height(4.dp))

            // Storage usage percentage — large and prominent
            Text(
                text = "$usedPercent%",
                style = TextStyle(
                    color = orange,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Text(
                text = "used",
                style = TextStyle(
                    color = muted,
                    fontSize = 11.sp
                )
            )
        }
    }
}

internal fun getUsedStoragePercent(): Int {
    return try {
        val stat = StatFs(Environment.getDataDirectory().path)
        val total = stat.totalBytes
        val free = stat.freeBytes
        if (total > 0) ((total - free) * 100 / total).toInt() else 0
    } catch (e: Exception) {
        0
    }
}
