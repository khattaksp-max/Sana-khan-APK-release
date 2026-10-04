package com.example.audio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

class AudioRecorderManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _micAmplitude = MutableStateFlow(0f)
    val micAmplitude: StateFlow<Float> = _micAmplitude.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var speechRecognizer: SpeechRecognizer? = null

    private var onSpeechCompletedCallback: ((ByteArray, String) -> Unit)? = null
    private val capturedAudioBuffer = ByteArrayOutputStream()
    private var lastRecognizedText = ""

    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val SILENCE_THRESHOLD_RMS = 0.04f
        private const val SILENCE_DURATION_MS = 1400L // 1.4 seconds of silence triggers end of speech
    }

    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Start listening to user voice.
     */
    fun startListening(
        onSpeechCompleted: (audioBytes: ByteArray, text: String) -> Unit
    ) {
        if (!hasRecordPermission()) {
            Log.w("AudioRecorder", "Record audio permission not granted")
            return
        }

        stopListening()
        this.onSpeechCompletedCallback = onSpeechCompleted
        _partialTranscript.value = ""
        lastRecognizedText = ""
        capturedAudioBuffer.reset()
        _isRecording.value = true

        // Start PCM AudioRecord in background coroutine
        recordingJob = coroutineScope.launch(Dispatchers.IO) {
            recordAudioLoop()
        }

        // Also start Android SpeechRecognizer on main thread for real-time live text display
        mainHandler.post {
            startSpeechRecognizer()
        }
    }

    fun stopListening(triggerCallback: Boolean = true) {
        if (!_isRecording.value) return
        _isRecording.value = false
        _micAmplitude.value = 0f

        recordingJob?.cancel()
        recordingJob = null

        try {
            audioRecord?.apply {
                if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.w("AudioRecorder", "Error stopping AudioRecord", e)
        } finally {
            audioRecord = null
        }

        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
            } catch (ignored: Exception) {}
            speechRecognizer = null
        }

        if (triggerCallback) {
            val audioBytes = capturedAudioBuffer.toByteArray()
            val text = lastRecognizedText.trim()
            if (audioBytes.isNotEmpty() || text.isNotEmpty()) {
                mainHandler.post {
                    onSpeechCompletedCallback?.invoke(audioBytes, text)
                }
            }
        }
    }

    private suspend fun recordAudioLoop() = withContext(Dispatchers.IO) {
        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = maxOf(minBufferSize * 2, 4096)

        try {
            val record = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("AudioRecorder", "AudioRecord initialization failed")
                return@withContext
            }

            audioRecord = record
            record.startRecording()

            val buffer = ByteArray(2048)
            var speechDetected = false
            var lastSpeechTime = 0L

            while (isActive && _isRecording.value) {
                val readBytes = record.read(buffer, 0, buffer.size)
                if (readBytes > 0) {
                    capturedAudioBuffer.write(buffer, 0, readBytes)

                    // Calculate amplitude
                    val rms = calculateRms(buffer, readBytes)
                    _micAmplitude.value = rms

                    val now = System.currentTimeMillis()
                    if (rms > SILENCE_THRESHOLD_RMS) {
                        speechDetected = true
                        lastSpeechTime = now
                    } else if (speechDetected && (now - lastSpeechTime > SILENCE_DURATION_MS)) {
                        // User was speaking and has now paused/finished!
                        Log.d("AudioRecorder", "Silence detected after speech -> auto-sending")
                        mainHandler.post {
                            stopListening(triggerCallback = true)
                        }
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error during audio recording", e)
        }
    }

    private fun startSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.d("AudioRecorder", "SpeechRecognizer not available on device")
            return
        }

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {
                        // Supplement amplitude if needed
                        if (_micAmplitude.value < 0.1f && rmsdB > 0) {
                            _micAmplitude.value = (rmsdB / 10f).coerceIn(0f, 1f)
                        }
                    }
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        Log.d("AudioRecorder", "SpeechRecognizer code: $error")
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            lastRecognizedText = text
                            _partialTranscript.value = text
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            lastRecognizedText = text
                            _partialTranscript.value = text
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                startListening(intent)
            }
        } catch (e: Exception) {
            Log.w("AudioRecorder", "SpeechRecognizer start failed", e)
        }
    }

    private fun calculateRms(buffer: ByteArray, length: Int): Float {
        var sum = 0.0
        val shortCount = length / 2
        if (shortCount == 0) return 0f
        val bb = ByteBuffer.wrap(buffer, 0, length).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until shortCount) {
            val sample = bb.short / 32768.0
            sum += sample * sample
        }
        val rms = sqrt(sum / shortCount).toFloat()
        return (rms * 4.5f).coerceIn(0f, 1f)
    }
}
