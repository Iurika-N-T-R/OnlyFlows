package com.onlyflows.ui.overlays

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Voice typing that never leaves the phone: Android's on-device recognizer (API 33+), no
 * Google dialog, no network. [available] is false when the device has no offline model, and the
 * mic is simply not shown then. Each final phrase goes to [onText].
 */
class Dictation(val available: Boolean, val listening: Boolean, val toggle: () -> Unit)

@Composable
fun rememberDictation(onText: (String) -> Unit): Dictation {
    val context = LocalContext.current
    val available = remember { Build.VERSION.SDK_INT >= 33 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context) }
    var listening by remember { mutableStateOf(false) }
    val latestOnText by rememberUpdatedState(onText)
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    fun start() {
        if (Build.VERSION.SDK_INT < 33) return
        val r = recognizer ?: SpeechRecognizer.createOnDeviceSpeechRecognizer(context).also { recognizer = it }
        r.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                listening = false
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    ?.takeIf { it.isNotBlank() }?.let(latestOnText)
            }
            override fun onError(error: Int) {
                listening = false
                if (error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED || error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE)
                    Toast.makeText(context, "No offline voice model for this language", Toast.LENGTH_LONG).show()
            }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        r.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true),
        )
        listening = true
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) start()
    }

    DisposableEffect(Unit) { onDispose { recognizer?.destroy() } }

    return Dictation(available, listening) {
        when {
            listening -> { recognizer?.stopListening(); listening = false }
            hasMic(context) -> start()
            else -> permission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}

private fun hasMic(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
