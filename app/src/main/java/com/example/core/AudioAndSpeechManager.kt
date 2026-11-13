package com.example.core

import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.Locale

private const val TAG = "AudioAndSpeechManager"

class AudioAndSpeechManager(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var speechRecognizer: SpeechRecognizer? = null
    private var mediaRecorder: MediaRecorder? = null
    private var currentAudioFile: File? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _speakingRate = MutableStateFlow(0.95f)
    val speakingRate: StateFlow<Float> = _speakingRate.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private val _speechError = MutableStateFlow<String?>(null)
    val speechError: StateFlow<String?> = _speechError.asStateFlow()

    private val _isRecordingAudio = MutableStateFlow(false)
    val isRecordingAudio: StateFlow<Boolean> = _isRecordingAudio.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val res = tts?.setLanguage(Locale.US)
                ttsReady = res != TextToSpeech.LANG_MISSING_DATA &&
                    res != TextToSpeech.LANG_NOT_SUPPORTED
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                })
            }
        }
    }

    fun setSpeakingRate(rate: Float) {
        _speakingRate.value = rate.coerceIn(0.65f, 1.35f)
        tts?.setSpeechRate(_speakingRate.value)
    }

    fun speak(text: String) {
        if (!ttsReady || text.isBlank()) return
        tts?.setSpeechRate(_speakingRate.value)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "bayan_tts_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun startSpeechRecognition() {
        _speechError.value = null
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _speechError.value = "التعرف المباشر على الصوت غير متاح على هذا الجهاز؛ يمكنك استخدام التسجيل الصوتي أو الإدخال النصي."
            return
        }
        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        _isListening.value = false
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        _speechError.value = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "لم يتم التقاط كلام واضح، حاول التحدث بوضوح."
                            SpeechRecognizer.ERROR_AUDIO -> "تعذر الوصول إلى الميكروفون."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "يرجى منح إذن الميكروفون."
                            else -> "تعذر إتمام التعرف الصوتي (رمز $error)."
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val best = matches?.firstOrNull().orEmpty()
                        if (best.isNotBlank()) {
                            _recognizedText.value = best
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull().orEmpty()
                        if (partial.isNotBlank()) {
                            _recognizedText.value = partial
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "SpeechRecognizer start failed", e)
            _isListening.value = false
            _speechError.value = e.localizedMessage
        }
    }

    fun stopSpeechRecognition() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {
        }
        _isListening.value = false
    }

    fun startAudioRecording(): Boolean {
        _speechError.value = null
        return try {
            val file = File(context.cacheDir, "bayan_voice_${System.currentTimeMillis()}.m4a")
            currentAudioFile = file
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(16000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            _isRecordingAudio.value = true
            true
        } catch (e: Exception) {
            Log.e(TAG, "MediaRecorder failed", e)
            _isRecordingAudio.value = false
            _speechError.value = "تعذر بدء التسجيل الصوتي على هذا الجهاز."
            false
        }
    }

    fun stopAudioRecordingAndReadBytes(): ByteArray? {
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            _isRecordingAudio.value = false
            val bytes = currentAudioFile?.takeIf { it.exists() }?.readBytes()
            currentAudioFile?.delete()
            currentAudioFile = null
            bytes
        } catch (e: Exception) {
            Log.e(TAG, "Stop MediaRecorder failed", e)
            mediaRecorder?.release()
            mediaRecorder = null
            _isRecordingAudio.value = false
            null
        }
    }

    fun clearRecognizedText() {
        _recognizedText.value = ""
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        speechRecognizer?.destroy()
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {
        }
    }
}
