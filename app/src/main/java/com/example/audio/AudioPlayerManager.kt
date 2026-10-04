package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

class AudioPlayerManager(private val context: Context) {

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    @Volatile
    private var isInterrupted = false

    private var currentAudioTrack: AudioTrack? = null
    private var currentMediaPlayer: MediaPlayer? = null
    private var playbackJob: Job? = null

    /**
     * Stop and immediately interrupt any currently playing audio.
     */
    fun stopPlayback() {
        isInterrupted = true
        _isPlaying.value = false
        _audioAmplitude.value = 0f

        try {
            currentAudioTrack?.apply {
                if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                    pause()
                    flush()
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.w("AudioPlayer", "Error stopping AudioTrack: ${e.message}")
        } finally {
            currentAudioTrack = null
        }

        try {
            currentMediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.w("AudioPlayer", "Error stopping MediaPlayer: ${e.message}")
        } finally {
            currentMediaPlayer = null
        }
    }

    /**
     * Play base64 audio returned by Gemini native audio engine.
     */
    suspend fun playBase64Audio(
        base64Data: String,
        mimeType: String,
        onPlaybackFinished: () -> Unit
    ) = withContext(Dispatchers.IO) {
        stopPlayback()
        isInterrupted = false

        try {
            val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
            if (audioBytes == null || audioBytes.isEmpty()) {
                withContext(Dispatchers.Main) { onPlaybackFinished() }
                return@withContext
            }

            val isPcm = mimeType.contains("pcm", ignoreCase = true) ||
                    (!audioBytes.hasWavHeader() && !audioBytes.hasMp3Header())

            if (isPcm) {
                // Determine sample rate from mimeType e.g. "audio/pcm;rate=24000"
                val sampleRate = extractSampleRate(mimeType, default = 24000)
                playPcmStream(audioBytes, sampleRate, onPlaybackFinished)
            } else {
                playMediaFile(audioBytes, onPlaybackFinished)
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Playback error", e)
            _isPlaying.value = false
            _audioAmplitude.value = 0f
            withContext(Dispatchers.Main) { onPlaybackFinished() }
        }
    }

    private suspend fun playPcmStream(
        pcmData: ByteArray,
        sampleRate: Int,
        onPlaybackFinished: () -> Unit
    ) = withContext(Dispatchers.IO) {
        val channelConfig = AudioFormat.CHANNEL_OUT_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = maxOf(minBufferSize * 2, 4096)

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(audioFormat)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        currentAudioTrack = audioTrack
        _isPlaying.value = true

        try {
            audioTrack.play()
            val chunkSize = 2048
            var offset = 0

            while (offset < pcmData.size && !isInterrupted) {
                val length = minOf(chunkSize, pcmData.size - offset)
                val written = audioTrack.write(pcmData, offset, length)
                if (written > 0) {
                    // Compute RMS amplitude of chunk for the reactive orb visualizer
                    val rms = calculateRms(pcmData, offset, written)
                    _audioAmplitude.value = rms.coerceIn(0f, 1f)
                    offset += written
                } else {
                    break
                }
            }

            // Allow the last samples to drain if not interrupted
            if (!isInterrupted) {
                // Short wait to ensure audio completes playback
                kotlinx.coroutines.delay(200)
            }
        } finally {
            _isPlaying.value = false
            _audioAmplitude.value = 0f
            try {
                if (audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    audioTrack.stop()
                }
                audioTrack.release()
            } catch (ignored: Exception) {}
            currentAudioTrack = null
            withContext(Dispatchers.Main) {
                onPlaybackFinished()
            }
        }
    }

    private suspend fun playMediaFile(
        audioBytes: ByteArray,
        onPlaybackFinished: () -> Unit
    ) = withContext(Dispatchers.IO) {
        val tempFile = File.createTempFile("sana_speech_", ".audio", context.cacheDir)
        try {
            FileOutputStream(tempFile).use { it.write(audioBytes) }

            val mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
            }
            currentMediaPlayer = mediaPlayer
            _isPlaying.value = true

            mediaPlayer.setOnCompletionListener {
                _isPlaying.value = false
                _audioAmplitude.value = 0f
                mediaPlayer.release()
                currentMediaPlayer = null
                tempFile.delete()
                onPlaybackFinished()
            }

            mediaPlayer.start()

            // Amplitude simulation for MediaPlayer while playing
            while (mediaPlayer.isPlaying && !isInterrupted) {
                _audioAmplitude.value = (0.3f + (Math.random().toFloat() * 0.5f))
                kotlinx.coroutines.delay(80)
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "MediaPlayer failed", e)
            _isPlaying.value = false
            _audioAmplitude.value = 0f
            tempFile.delete()
            withContext(Dispatchers.Main) { onPlaybackFinished() }
        }
    }

    private fun calculateRms(data: ByteArray, offset: Int, length: Int): Float {
        var sumSquares = 0.0
        val shortCount = length / 2
        if (shortCount == 0) return 0f

        val byteBuffer = ByteBuffer.wrap(data, offset, length).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until shortCount) {
            val sample = byteBuffer.short / 32768.0
            sumSquares += sample * sample
        }
        val rms = sqrt(sumSquares / shortCount).toFloat()
        // Boost for visualizer sensitivity
        return (rms * 3.5f).coerceIn(0f, 1f)
    }

    private fun extractSampleRate(mimeType: String, default: Int): Int {
        val rateRegex = Regex("rate=(\\d+)")
        val match = rateRegex.find(mimeType)
        return match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: default
    }

    private fun ByteArray.hasWavHeader(): Boolean {
        if (size < 12) return false
        val riff = String(copyOfRange(0, 4))
        val wave = String(copyOfRange(8, 12))
        return riff == "RIFF" && wave == "WAVE"
    }

    private fun ByteArray.hasMp3Header(): Boolean {
        if (size < 3) return false
        return (this[0].toInt() == 0x49 && this[1].toInt() == 0x44 && this[2].toInt() == 0x33) // ID3
    }
}
