package com.onlyflows.wear.data

import android.content.Context
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

/** Request/response to the phone over the Wear Data Layer (MessageClient).
 *  Same transport Tasks.org uses under its gRPC wrapper. Returns null if no phone reachable. */
object WearBridge {

    suspend fun request(context: Context, path: String, body: String = ""): String? {
        val node = bestNode(context) ?: return null
        return runCatching {
            val resp = Wearable.getMessageClient(context)
                .sendRequest(node, path, body.toByteArray(Charsets.UTF_8))
                .await()
            resp?.toString(Charsets.UTF_8)
        }.getOrNull()
    }

    private suspend fun bestNode(context: Context): String? {
        val nodes = runCatching { Wearable.getNodeClient(context).connectedNodes.await() }.getOrNull()
            ?: return null
        return nodes.firstOrNull { it.isNearby }?.id ?: nodes.firstOrNull()?.id
    }
}
