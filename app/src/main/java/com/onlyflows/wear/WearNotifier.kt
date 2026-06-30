package com.onlyflows.wear

import android.content.Context
import com.google.android.gms.wearable.Wearable

/** Pings connected watches to reload after any phone-side task change. Fire-and-forget. */
object WearNotifier {
    private const val REFRESH_PATH = "/onlyflows/refresh"

    fun notifyRefresh(context: Context) {
        val client = Wearable.getMessageClient(context)
        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                nodes.forEach { runCatching { client.sendMessage(it.id, REFRESH_PATH, ByteArray(0)) } }
            }
    }
}
