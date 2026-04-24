package com.onlyflows.ui.widgets.quickadd

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.onlyflows.MainActivity
import com.onlyflows.R
import com.onlyflows.ui.widgets.common.WidgetColors

class QuickAddWidgetProvider : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(DpSize(110.dp, 110.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {

        fun addIntent(destination: String) = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(MainActivity.EXTRA_DESTINATION, destination)

        provideContent {
            GlanceTheme {
                Box(
                    modifier         = GlanceModifier
                        .fillMaxSize()
                        .background(WidgetColors.DarkBg)
                        .padding(12.dp),
                    contentAlignment = Alignment.TopStart,
                ) {
                    Column(modifier = GlanceModifier.fillMaxSize()) {
                        Text(
                            text  = "Add a task",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize   = 14.sp,
                                color      = ColorProvider(WidgetColors.TextPrimary),
                            ),
                        )
                        Spacer(GlanceModifier.defaultWeight())

                        // Today shortcut
                        Row(
                            modifier          = GlanceModifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable(actionStartActivity(addIntent(MainActivity.DESTINATION_TODAY))),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(
                                provider           = ImageProvider(R.drawable.ic_nav_today),
                                contentDescription = "Today",
                                modifier           = GlanceModifier.size(16.dp),
                            )
                            Spacer(GlanceModifier.width(6.dp))
                            Text(
                                text  = "Today",
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    color    = ColorProvider(WidgetColors.TextPrimary),
                                ),
                            )
                        }

                        // Inbox shortcut
                        Row(
                            modifier          = GlanceModifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable(actionStartActivity(addIntent(MainActivity.DESTINATION_INBOX))),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(
                                provider           = ImageProvider(R.drawable.ic_nav_inbox),
                                contentDescription = "Inbox",
                                modifier           = GlanceModifier.size(16.dp),
                            )
                            Spacer(GlanceModifier.width(6.dp))
                            Text(
                                text  = "Inbox",
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    color    = ColorProvider(WidgetColors.TextPrimary),
                                ),
                            )
                        }
                    }

                    // FAB bottom-end
                    Box(
                        modifier         = GlanceModifier
                            .fillMaxSize()
                            .padding(bottom = 2.dp, end = 2.dp),
                        contentAlignment = Alignment.BottomEnd,
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .size(36.dp)
                                .background(WidgetColors.AccentBlue)
                                .cornerRadius(18.dp)
                                .clickable(actionStartActivity(addIntent(MainActivity.DESTINATION_TODAY))),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text  = "+",
                                style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 22.sp,
                                    color      = ColorProvider(Color.White),
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}
