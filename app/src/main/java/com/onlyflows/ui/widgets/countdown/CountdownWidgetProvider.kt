package com.onlyflows.ui.widgets.countdown

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.onlyflows.data.preferences.WidgetPreferences
import com.onlyflows.ui.widgets.common.WidgetColors
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class CountdownWidgetProvider : GlanceAppWidget() {

    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val prefs = WidgetPreferences(context)
        val name = prefs.getCountdownName(appWidgetId)
        val dateStr = prefs.getCountdownDate(appWidgetId)

        val daysRemaining: Long? = if (dateStr.isNotBlank()) {
            runCatching {
                val target = LocalDate.parse(dateStr)
                ChronoUnit.DAYS.between(LocalDate.now(), target)
            }.getOrNull()
        } else null

        val configIntent = Intent(context, CountdownConfigActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }

        provideContent {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(WidgetColors.DarkBg)
                    .clickable(actionStartActivity(configIntent)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = GlanceModifier.fillMaxSize().padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (name.isBlank() || daysRemaining == null) {
                        Text(
                            text = "Tap to configure",
                            style = TextStyle(
                                color = ColorProvider(WidgetColors.TextSecondary),
                                fontSize = 14.sp,
                            ),
                        )
                    } else {
                        Text(
                            text = name,
                            style = TextStyle(
                                color = ColorProvider(WidgetColors.TextSecondary),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                            ),
                            maxLines = 1,
                        )
                        Spacer(GlanceModifier.height(4.dp))
                        Text(
                            text = if (daysRemaining >= 0) daysRemaining.toString() else (-daysRemaining).toString(),
                            style = TextStyle(
                                color = ColorProvider(WidgetColors.AccentBlue),
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            ),
                        )
                        Spacer(GlanceModifier.height(2.dp))
                        Text(
                            text = when {
                                daysRemaining > 1   -> "jours restants"
                                daysRemaining == 1L -> "jour restant"
                                daysRemaining == 0L -> "Aujourd'hui !"
                                else                -> "jours passés"
                            },
                            style = TextStyle(
                                color = ColorProvider(WidgetColors.TextSecondary),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                            ),
                        )
                    }
                }
            }
        }
    }
}
