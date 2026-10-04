package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayerManager
import com.example.audio.AudioRecorderManager
import com.example.data.api.GeminiVoiceService
import com.example.data.db.ChatMessageEntity
import com.example.data.db.MemoryEntity
import com.example.data.db.SanaDatabase
import com.example.data.model.ActionCardData
import com.example.data.model.ChatMessage
import com.example.data.model.CompanionMode
import com.example.data.model.LanguageOption
import com.example.data.model.SanaVoice
import com.example.data.model.VoiceState
import com.example.phone.PhoneActionHandler
import com.example.service.SanaVoiceService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SanaViewModel(application: Application) : AndroidViewModel(application) {

    private val db = SanaDatabase.getDatabase(application)
    private val dao = db.sanaDao()

    private val voiceService = GeminiVoiceService()
    val audioPlayer = AudioPlayerManager(application)
    val audioRecorder = AudioRecorderManager(application, viewModelScope)
    val phoneHandler = PhoneActionHandler(application)

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _selectedVoice = MutableStateFlow(SanaVoice.KORE)
    val selectedVoice: StateFlow<SanaVoice> = _selectedVoice.asStateFlow()

    private val _companionMode = MutableStateFlow(CompanionMode.ASSISTANT)
    val companionMode: StateFlow<CompanionMode> = _companionMode.asStateFlow()

    private val _languageOption = MutableStateFlow(LanguageOption.AUTO)
    val languageOption: StateFlow<LanguageOption> = _languageOption.asStateFlow()

    private val _isMemoryEnabled = MutableStateFlow(true)
    val isMemoryEnabled: StateFlow<Boolean> = _isMemoryEnabled.asStateFlow()

    private val _isBackgroundVoiceEnabled = MutableStateFlow(false)
    val isBackgroundVoiceEnabled: StateFlow<Boolean> = _isBackgroundVoiceEnabled.asStateFlow()

    private val _statusText = MutableStateFlow("Tap the mic to talk with SANA")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _memories = MutableStateFlow<List<MemoryEntity>>(emptyList())
    val memories: StateFlow<List<MemoryEntity>> = _memories.asStateFlow()

    private val _activeActionCard = MutableStateFlow<ActionCardData?>(null)
    val activeActionCard: StateFlow<ActionCardData?> = _activeActionCard.asStateFlow()

    private val _combinedAmplitude = MutableStateFlow(0f)
    val combinedAmplitude: StateFlow<Float> = _combinedAmplitude.asStateFlow()

    private var activeApiJob: Job? = null

    init {
        // Collect messages from DB
        viewModelScope.launch {
            dao.getAllMessages().collectLatest { entities ->
                val list = entities.map { it.toChatMessage() }
                _messages.value = list
                if (list.isEmpty()) {
                    // Seed initial welcome message from SANA
                    val welcome = ChatMessage(
                        isUser = false,
                        text = "Hello! I'm SANA, your AI voice companion. You can talk to me anytime, or switch my voice, mode, and language from the top bar.",
                        mode = CompanionMode.ASSISTANT
                    )
                    dao.insertMessage(ChatMessageEntity.fromChatMessage(welcome))
                }
            }
        }

        // Collect memories from DB
        viewModelScope.launch {
            dao.getAllMemories().collectLatest {
                _memories.value = it
            }
        }

        // Combine live microphone and playback amplitudes for the animated voice orb
        viewModelScope.launch {
            audioRecorder.micAmplitude.collectLatest { micRms ->
                if (_voiceState.value == VoiceState.LISTENING) {
                    _combinedAmplitude.value = micRms
                }
            }
        }
        viewModelScope.launch {
            audioPlayer.audioAmplitude.collectLatest { playRms ->
                if (_voiceState.value == VoiceState.SPEAKING) {
                    _combinedAmplitude.value = playRms
                }
            }
        }

        checkApiKeyStatus()
    }

    fun checkApiKeyStatus() {
        if (!voiceService.isApiKeyConfigured()) {
            _errorMessage.value = "Gemini isn't configured yet. Please add the required API configuration."
            _statusText.value = "Gemini isn't configured yet. Please add the required API configuration."
        } else {
            _errorMessage.value = null
            _statusText.value = "SANA is ready. Tap to speak."
        }
    }

    /**
     * Primary action triggered when tapping the mic or animated orb.
     */
    fun onOrbOrMicClicked() {
        when (_voiceState.value) {
            VoiceState.SPEAKING -> {
                // Interruption! Stop playback immediately and start listening
                interruptSpeech()
                startListening()
            }
            VoiceState.LISTENING -> {
                // Manually finish listening
                audioRecorder.stopListening(triggerCallback = true)
            }
            VoiceState.THINKING -> {
                // Cancel current request and reset to idle
                activeApiJob?.cancel()
                _voiceState.value = VoiceState.IDLE
                _statusText.value = "Cancelled. Ready when you are."
            }
            VoiceState.IDLE, VoiceState.ERROR -> {
                startListening()
            }
        }
    }

    fun startListening() {
        if (!audioRecorder.hasRecordPermission()) {
            _statusText.value = "Microphone permission required"
            return
        }

        interruptSpeech()
        _errorMessage.value = null
        _voiceState.value = VoiceState.LISTENING
        _statusText.value = "SANA is listening..."

        audioRecorder.startListening { audioBytes, recognizedText ->
            onSpeechReceived(audioBytes, recognizedText)
        }
    }

    private fun onSpeechReceived(audioBytes: ByteArray, recognizedText: String) {
        if (audioBytes.isEmpty() && recognizedText.isBlank()) {
            _voiceState.value = VoiceState.IDLE
            _statusText.value = "Didn't catch that. Tap to talk again."
            return
        }

        val userText = if (recognizedText.isNotBlank()) recognizedText else "Spoken message"
        processUserTurn(userText, audioBytes)
    }

    fun sendTextMessage(text: String) {
        if (text.isBlank()) return
        interruptSpeech()
        processUserTurn(text, null)
    }

    private fun processUserTurn(userText: String, audioBytes: ByteArray?) {
        _voiceState.value = VoiceState.THINKING
        _statusText.value = "SANA is thinking..."

        activeApiJob = viewModelScope.launch {
            // Save user message to DB
            val userMsg = ChatMessage(
                isUser = true,
                text = userText,
                mode = _companionMode.value
            )
            dao.insertMessage(ChatMessageEntity.fromChatMessage(userMsg))

            // Check for direct voice or mode switch commands locally
            val lower = userText.lowercase()
            if (lower.contains("change voice to aoede") || lower.contains("switch to aoede")) {
                setVoice(SanaVoice.AOEDE)
            } else if (lower.contains("change voice to kore") || lower.contains("switch to kore") || lower.contains("voice back to kore")) {
                setVoice(SanaVoice.KORE)
            }

            // Extract conversation history
            val history = _messages.value.takeLast(6).map {
                (if (it.isUser) "user" else "model") to it.text
            }

            val activeMemories = if (_isMemoryEnabled.value) {
                dao.getMemoriesList()
            } else emptyList()

            val result = voiceService.converseWithSana(
                userMessage = userText,
                userAudioBytes = audioBytes,
                voice = _selectedVoice.value,
                mode = _companionMode.value,
                language = _languageOption.value,
                memories = activeMemories,
                chatHistory = history
            )

            result.fold(
                onSuccess = { voiceResult ->
                    handleSanaResponse(voiceResult)
                },
                onFailure = { error ->
                    Log.e("SanaViewModel", "Error from Gemini API", error)
                    _voiceState.value = VoiceState.ERROR
                    val msg = error.message ?: "Failed to connect to SANA"
                    _errorMessage.value = msg
                    _statusText.value = if (msg.contains("configured", ignoreCase = true)) {
                        "Gemini isn't configured yet. Please add the required API configuration."
                    } else {
                        "Connection issue: $msg"
                    }
                }
            )
        }
    }

    private suspend fun handleSanaResponse(result: com.example.data.api.GeminiVoiceResult) = withContext(Dispatchers.Main) {
        val detectedAction = result.detectedAction
        val actionCard = if (detectedAction != null) {
            val card = ActionCardData(
                type = try {
                    com.example.data.model.ActionType.valueOf(detectedAction.actionType)
                } catch (e: Exception) {
                    com.example.data.model.ActionType.OPEN_APP
                },
                title = detectedAction.title,
                description = detectedAction.description,
                target = detectedAction.target,
                extra = detectedAction.extra
            )
            _activeActionCard.value = card
            card
        } else null

        // Check if memory should be created from user preferences or facts
        checkAndSaveAutomaticMemory(result.transcriptText)

        // Save SANA message to DB
        val sanaMsg = ChatMessage(
            isUser = false,
            text = result.transcriptText,
            actionCard = actionCard,
            mode = _companionMode.value
        )
        dao.insertMessage(ChatMessageEntity.fromChatMessage(sanaMsg))

        // Play audio if available
        if (!result.spokenAudioBase64.isNullOrEmpty()) {
            _voiceState.value = VoiceState.SPEAKING
            _statusText.value = "SANA is speaking..."

            audioPlayer.playBase64Audio(
                base64Data = result.spokenAudioBase64,
                mimeType = result.audioMimeType ?: "audio/pcm;rate=24000",
                onPlaybackFinished = {
                    _voiceState.value = VoiceState.IDLE
                    _statusText.value = "Tap the mic to talk with SANA"
                }
            )
        } else {
            // Text only response
            _voiceState.value = VoiceState.IDLE
            _statusText.value = "Tap the mic to talk with SANA"
        }
    }

    fun interruptSpeech() {
        if (_voiceState.value == VoiceState.SPEAKING) {
            audioPlayer.stopPlayback()
            _voiceState.value = VoiceState.IDLE
            _statusText.value = "Interrupted. Tap to speak."
        }
    }

    fun testVoice(voice: SanaVoice) {
        interruptSpeech()
        _voiceState.value = VoiceState.THINKING
        _statusText.value = "Testing ${voice.displayName} voice..."

        viewModelScope.launch {
            val samplePrompt = when (_companionMode.value) {
                CompanionMode.ROMANTIC -> "Say affectionately in a warm gentle voice: Hello sweetheart! I'm SANA. It's so nice to talk with you."
                CompanionMode.FRIEND -> "Say casually and warmly: Hey there! I'm SANA. How is your day going?"
                CompanionMode.COMPANION -> "Say in a comforting, gentle tone: Hello. I'm SANA, and I'm right here with you."
                CompanionMode.ASSISTANT -> "Say cheerfully: Hello! I'm SANA, your AI voice assistant. How can I help you today?"
            }

            val result = voiceService.converseWithSana(
                userMessage = samplePrompt,
                userAudioBytes = null,
                voice = voice,
                mode = _companionMode.value,
                language = _languageOption.value,
                memories = emptyList(),
                chatHistory = emptyList()
            )

            result.fold(
                onSuccess = { res ->
                    if (!res.spokenAudioBase64.isNullOrEmpty()) {
                        _voiceState.value = VoiceState.SPEAKING
                        _statusText.value = "Playing ${voice.displayName} sample..."
                        audioPlayer.playBase64Audio(
                            res.spokenAudioBase64,
                            res.audioMimeType ?: "audio/pcm;rate=24000"
                        ) {
                            _voiceState.value = VoiceState.IDLE
                            _statusText.value = "Voice test finished"
                        }
                    } else {
                        _voiceState.value = VoiceState.IDLE
                        _statusText.value = "Tested ${voice.displayName} (Text response)"
                    }
                },
                onFailure = { err ->
                    _voiceState.value = VoiceState.ERROR
                    _errorMessage.value = err.message
                    _statusText.value = "Voice test failed: ${err.message}"
                }
            )
        }
    }

    fun setVoice(voice: SanaVoice) {
        _selectedVoice.value = voice
    }

    fun setCompanionMode(mode: CompanionMode) {
        _companionMode.value = mode
    }

    fun setLanguage(lang: LanguageOption) {
        _languageOption.value = lang
    }

    fun toggleMemory(enabled: Boolean) {
        _isMemoryEnabled.value = enabled
    }

    fun toggleBackgroundVoice(enabled: Boolean) {
        _isBackgroundVoiceEnabled.value = enabled
        val context = getApplication<Application>()
        if (enabled) {
            SanaVoiceService.startService(context, "SANA is active in background")
        } else {
            SanaVoiceService.stopService(context)
        }
    }

    fun executeAction(card: ActionCardData) {
        phoneHandler.executeAction(card)
        _activeActionCard.value = null
    }

    fun dismissActionCard() {
        _activeActionCard.value = null
    }

    fun addManualMemory(category: String, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            dao.insertMemory(
                MemoryEntity(
                    category = category.ifBlank { "Preference" },
                    content = content.trim()
                )
            )
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            dao.deleteMemory(id)
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            dao.clearAllMemories()
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            dao.clearAllMessages()
            interruptSpeech()
            _voiceState.value = VoiceState.IDLE
            _statusText.value = "Chat cleared. Ready for a new conversation."
        }
    }

    private fun checkAndSaveAutomaticMemory(sanaResponse: String) {
        // Safe memory parsing: if SANA detects a user fact, save it
        // Never store sensitive keywords
        val lower = sanaResponse.lowercase()
        val forbidden = listOf("password", "api_key", "secret", "pin", "otp", "token")
        if (forbidden.any { lower.contains(it) }) return

        val memoryRegex = Regex("\\[\\[REMEMBER:([^:]+):([^\\]]+)\\]\\]")
        val match = memoryRegex.find(sanaResponse)
        if (match != null) {
            val cat = match.groupValues[1].trim()
            val fact = match.groupValues[2].trim()
            viewModelScope.launch {
                dao.insertMemory(MemoryEntity(category = cat, content = fact))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stopPlayback()
        audioRecorder.stopListening(triggerCallback = false)
    }
}
