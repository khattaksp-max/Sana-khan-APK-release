package com.example.data.model

enum class SanaVoice(val displayName: String, val voiceId: String, val description: String) {
    KORE("Kore", "Kore", "Primary female voice — Warm, clear & expressive"),
    AOEDE("Aoede", "Aoede", "Alternate female voice — Luminous, melodic & gentle")
}

enum class CompanionMode(val title: String, val subtitle: String, val badge: String) {
    ASSISTANT("Assistant", "Helpful, smart & efficient", "⚡ ASSISTANT"),
    FRIEND("Friend", "Casual, caring & witty", "✨ FRIEND"),
    COMPANION("Companion", "Warm, attentive & supportive", "💫 COMPANION"),
    ROMANTIC("Romantic", "Affectionate, sweet & playful", "💖 ROMANTIC")
}

enum class LanguageOption(val displayName: String, val promptInstruction: String) {
    AUTO("Auto Detect", "Detect the user's language automatically between English, Urdu, or Roman Urdu, and respond in the exact same language/dialect naturally."),
    ENGLISH("English", "Respond in fluent, natural English with appropriate conversational warmth."),
    URDU("اردو (Urdu)", "Respond in natural Urdu (using standard Urdu script) with warm conversational rhythm."),
    ROMAN_URDU("Roman Urdu", "Respond in casual Roman Urdu (Urdu written in English alphabet, e.g., 'Main theek hoon, aap bataiye') mixed naturally with English where appropriate.")
}

enum class VoiceState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    ERROR
}

data class ActionCardData(
    val type: ActionType,
    val title: String,
    val description: String,
    val target: String,
    val extra: String? = null
)

enum class ActionType {
    WHATSAPP_SEND,
    WHATSAPP_OPEN,
    CALL_PHONE,
    SET_ALARM,
    SET_REMINDER,
    OPEN_APP,
    WEB_SEARCH,
    NAVIGATE
}

data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionCard: ActionCardData? = null,
    val mode: CompanionMode = CompanionMode.ASSISTANT
)
