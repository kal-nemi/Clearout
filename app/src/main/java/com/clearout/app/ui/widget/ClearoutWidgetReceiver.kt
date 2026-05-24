package com.clearout.app.ui.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * BroadcastReceiver for the Small (2x1) ClearOut Widget.
 * Registered in AndroidManifest.xml.
 *
 * Android calls this receiver to update the widget data on schedule
 * or when the OS requests a refresh (e.g. after a device boot).
 */
class ClearoutSmallWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ClearoutSmallWidget()
}

/**
 * BroadcastReceiver for the Large (4x2) ClearOut Widget.
 * Registered in AndroidManifest.xml.
 */
class ClearoutLargeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ClearoutLargeWidget()
}
