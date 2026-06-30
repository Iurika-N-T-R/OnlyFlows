package com.onlyflows.wear.complications

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.onlyflows.wear.R
import com.onlyflows.wear.data.Paths
import com.onlyflows.wear.data.WearBridge
import com.onlyflows.wear.data.countBody
import com.onlyflows.wear.data.parseCount
import com.onlyflows.wear.presentation.MainActivity

private fun SuspendingComplicationDataSourceService.openAppPI(): PendingIntent =
    PendingIntent.getActivity(
        this, 0,
        Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_IMMUTABLE,
    )

private fun text(s: String) = PlainComplicationText.Builder(s).build()

/** SHORT_TEXT: number of active tasks today. */
class TaskCountComplicationService : SuspendingComplicationDataSourceService() {
    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val c = parseCount(WearBridge.request(this, Paths.GET_COUNT, countBody()))
        return ShortTextComplicationData.Builder(text("${c.count}"), text("Tâches actives"))
            .setTapAction(openAppPI()).build()
    }
    override fun getPreviewData(type: ComplicationType): ComplicationData =
        ShortTextComplicationData.Builder(text("3"), text("Tâches actives")).build()
}

/** RANGED_VALUE: today's completion (completed / total). */
class TaskProgressComplicationService : SuspendingComplicationDataSourceService() {
    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val c = parseCount(WearBridge.request(this, Paths.GET_COUNT, countBody()))
        val total = (c.count + c.completed).coerceAtLeast(1)
        return RangedValueComplicationData.Builder(c.completed.toFloat(), 0f, total.toFloat(), text("Progression"))
            .setText(text("${c.completed}/$total")).setTapAction(openAppPI()).build()
    }
    override fun getPreviewData(type: ComplicationType): ComplicationData =
        RangedValueComplicationData.Builder(2f, 0f, 5f, text("Progression")).setText(text("2/5")).build()
}

/** MONOCHROMATIC_IMAGE: tap to open the app (voice add). */
class AddTaskComplicationService : SuspendingComplicationDataSourceService() {
    private fun image() = MonochromaticImage.Builder(
        Icon.createWithResource(this, R.drawable.ic_complication_add)
    ).build()

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData =
        MonochromaticImageComplicationData.Builder(image(), text("Ajouter une tâche"))
            .setTapAction(openAppPI()).build()

    override fun getPreviewData(type: ComplicationType): ComplicationData =
        MonochromaticImageComplicationData.Builder(image(), text("Ajouter une tâche")).build()
}
