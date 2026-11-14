package com.example.okkhor.ime

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import com.example.okkhor.data.model.InputMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class VoiceInputState {
    data object Idle : VoiceInputState()
    data class Listening(
        val languageTag: String,
        val partialText: String = "",
        val rmsDb: Float = 0f
    ) : VoiceInputState()
    data object PermissionRequired : VoiceInputState()
    data class Error(val message: String) : VoiceInputState()
}

/**
 * Native Android SpeechRecognizer wrapper supporting language-aware Bangla (bn-BD)
 * and English (en-US) voice dictation with strict lifecycle cleanup.
 */
class VoiceInputController(
    private val context: Context,
    private val onTextRecognized: (String) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var currentLanguageTag: String = "bn-BD"

    private val _state = MutableStateFlow<VoiceInputState>(VoiceInputState.Idle)
    val state: StateFlow<VoiceInputState> = _state.asStateFlow()

    fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun startListening(mode: InputMode) {
        val targetLang = if (mode.isBangla) "bn-BD" else "en-US"
        currentLanguageTag = targetLang

        if (!hasAudioPermission()) {
            _state.value = VoiceInputState.PermissionRequired
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.value = VoiceInputState.Error("ভয়েস টাইপিং সার্ভিস এই ডিভাইসে পাওয়া যায়নি (Speech service unavailable)")
            return
        }

        mainHandler.post {
            try {
                destroyRecognizerInternal()
                val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizer = recognizer

                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _state.value = VoiceInputState.Listening(languageTag = targetLang)
                    }

                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {
                        val current = _state.value
                        if (current is VoiceInputState.Listening) {
                            _state.value = current.copy(rmsDb = rmsdB.coerceIn(0f, 12f))
                        }
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {}

                    override fun onError(error: Int) {
                        val msg = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "অডিও রেকর্ড সমস্যা (Audio error)"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                                _state.value = VoiceInputState.PermissionRequired
                                destroyRecognizerInternal()
                                return
                            }
                            SpeechRecognizer.ERROR_NETWORK,
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "নেটওয়ার্ক সংযোগ প্রয়োজন (Network error)"
                            SpeechRecognizer.ERROR_NO_MATCH,
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "কোনো কথা শোনা যায়নি (No speech detected)"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "স্পিচ সার্ভিস ব্যস্ত (Recognizer busy)"
                            else -> "ভয়েস ইনপুট ব্যর্থ হয়েছে ($error)"
                        }
                        _state.value = VoiceInputState.Error(msg)
                        destroyRecognizerInternal()
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim().orEmpty()
                        if (text.isNotEmpty()) {
                            onTextRecognized(text)
                        }
                        _state.value = VoiceInputState.Idle
                        destroyRecognizerInternal()
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partial = partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            ?.trim()
                            .orEmpty()
                        val current = _state.value
                        if (current is VoiceInputState.Listening && partial.isNotEmpty()) {
                            _state.value = current.copy(partialText = partial)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, targetLang)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, targetLang)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }

                _state.value = VoiceInputState.Listening(languageTag = targetLang)
                recognizer.startListening(intent)
            } catch (e: Exception) {
                _state.value = VoiceInputState.Error("ভয়েস টাইপিং চালু করা যায়নি")
                destroyRecognizerInternal()
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {
            }
            destroyRecognizerInternal()
            _state.value = VoiceInputState.Idle
        }
    }

    fun dismissNotice() {
        _state.value = VoiceInputState.Idle
    }

    fun destroy() {
        mainHandler.post {
            destroyRecognizerInternal()
            _state.value = VoiceInputState.Idle
        }
    }

    private fun destroyRecognizerInternal() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        } finally {
            speechRecognizer = null
        }
    }
}
