package com.example.data.api

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.db.MemoryEntity
import com.example.data.model.CompanionMode
import com.example.data.model.LanguageOption
import com.example.data.model.SanaVoice
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class GeminiVoiceService {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val requestAdapter = moshi.adapter(GeminiGenerateRequest::class.java)
    private val responseAdapter = moshi.adapter(GeminiGenerateResponse::class.java)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        const val PREFERRED_NATIVE_AUDIO_MODEL = "gemini-2.5-flash-native-audio-preview-12-2025"
        const val FALLBACK_TEXT_MODEL = "gemini-3.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    }

    fun isApiKeyConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY" && !key.contains("PLACEHOLDER")
    }

    /**
     * Send conversation turn to SANA and receive native audio + transcript.
     */
    suspend fun converseWithSana(
        userMessage: String,
        userAudioBytes: ByteArray?,
        voice: SanaVoice,
        mode: CompanionMode,
        language: LanguageOption,
        memories: List<MemoryEntity>,
        chatHistory: List<Pair<String, String>> // role to text
    ): Result<GeminiVoiceResult> = withContext(Dispatchers.IO) {
        if (!isApiKeyConfigured()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini isn't configured yet. Please add the required API configuration in the Secrets panel.")
            )
        }

        val apiKey = BuildConfig.GEMINI_API_KEY

        val systemPrompt = buildSystemPrompt(mode, language, memories)

        // Build contents list
        val contents = mutableListOf<GeminiContent>()

        // Add recent history turns (max 6 turns to keep fast latency)
        val recentHistory = chatHistory.takeLast(6)
        for ((role, text) in recentHistory) {
            val apiRole = if (role.equals("user", ignoreCase = true)) "user" else "model"
            contents.add(
                GeminiContent(
                    role = apiRole,
                    parts = listOf(GeminiPart(text = text))
                )
            )
        }

        // Add current user prompt
        val userParts = mutableListOf<GeminiPart>()
        if (userAudioBytes != null && userAudioBytes.isNotEmpty()) {
            val base64Audio = Base64.encodeToString(userAudioBytes, Base64.NO_WRAP)
            userParts.add(
                GeminiPart(
                    inlineData = GeminiInlineData(
                        mimeType = "audio/pcm;rate=16000",
                        data = base64Audio
                    )
                )
            )
        }
        if (userMessage.isNotBlank()) {
            userParts.add(GeminiPart(text = userMessage))
        } else if (userParts.isEmpty()) {
            userParts.add(GeminiPart(text = "Hello SANA!"))
        }

        contents.add(GeminiContent(role = "user", parts = userParts))

        // First attempt with preferred native audio model
        val audioRequest = GeminiGenerateRequest(
            contents = contents,
            generationConfig = GeminiGenerationConfig(
                responseModalities = listOf("AUDIO"),
                speechConfig = GeminiSpeechConfig(
                    voiceConfig = GeminiVoiceConfig(
                        prebuiltVoiceConfig = GeminiPrebuiltVoiceConfig(voiceName = voice.voiceId)
                    )
                ),
                temperature = if (mode == CompanionMode.ROMANTIC) 0.85f else 0.7f
            ),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt)))
        )

        val nativeAudioResult = callGeminiEndpoint(PREFERRED_NATIVE_AUDIO_MODEL, apiKey, audioRequest)
        if (nativeAudioResult.isSuccess) {
            return@withContext nativeAudioResult
        }

        Log.w("GeminiVoiceService", "Native audio preview attempt failed: ${nativeAudioResult.exceptionOrNull()?.message}. Retrying with multimodal audio/text fallback.")

        // Fallback: Request text response if audio preview fails or key lacks audio preview
        val textRequest = GeminiGenerateRequest(
            contents = contents,
            generationConfig = GeminiGenerationConfig(
                temperature = 0.7f
            ),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt)))
        )

        callGeminiEndpoint(FALLBACK_TEXT_MODEL, apiKey, textRequest)
    }

    private fun callGeminiEndpoint(
        modelName: String,
        apiKey: String,
        requestBodyObj: GeminiGenerateRequest
    ): Result<GeminiVoiceResult> {
        return try {
            val jsonString = requestAdapter.toJson(requestBodyObj)
            val url = "$BASE_URL/$modelName:generateContent?key=$apiKey"

            val okHttpRequest = Request.Builder()
                .url(url)
                .post(jsonString.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            val response = okHttpClient.newCall(okHttpRequest).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = try {
                    val errObj = responseAdapter.fromJson(responseBody)?.error
                    errObj?.message ?: "HTTP ${response.code}: ${response.message}"
                } catch (e: Exception) {
                    "HTTP ${response.code}: $responseBody"
                }
                return Result.failure(Exception(errorMsg))
            }

            val parsedResponse = responseAdapter.fromJson(responseBody)
            val candidate = parsedResponse?.candidates?.firstOrNull()
            val parts = candidate?.content?.parts ?: emptyList()

            var audioBase64: String? = null
            var audioMimeType: String? = null
            var transcriptText = ""

            for (part in parts) {
                if (part.inlineData != null) {
                    audioBase64 = part.inlineData.data
                    audioMimeType = part.inlineData.mimeType
                }
                if (!part.text.isNullOrBlank()) {
                    transcriptText = if (transcriptText.isEmpty()) part.text else "$transcriptText\n${part.text}"
                }
            }

            // If audio was provided without a transcript, generate or provide clean companion feedback
            if (transcriptText.isBlank() && audioBase64 != null) {
                transcriptText = "Spoken response from SANA"
            }

            // Parse for actions in transcript (e.g. WhatsApp, phone calls, apps)
            val (cleanText, detectedAction) = parseActionsFromText(transcriptText)

            Result.success(
                GeminiVoiceResult(
                    spokenAudioBase64 = audioBase64,
                    audioMimeType = audioMimeType ?: "audio/pcm;rate=24000",
                    transcriptText = cleanText,
                    detectedAction = detectedAction
                )
            )
        } catch (e: Exception) {
            Log.e("GeminiVoiceService", "API call failure", e)
            Result.failure(e)
        }
    }

    private fun buildSystemPrompt(
        mode: CompanionMode,
        language: LanguageOption,
        memories: List<MemoryEntity>
    ): String {
        val modeInstruction = when (mode) {
            CompanionMode.ASSISTANT ->
                "You are SANA, a warm, highly intelligent, attentive personal AI voice assistant. Speak with natural conversational rhythm, empathy, and clarity. Be helpful, concise, and never robotic. Never say repetitive robotic phrases like 'How can I assist you today?'. Instead, talk like a caring and confident friend."

            CompanionMode.FRIEND ->
                "You are SANA in Friend Mode. You are casual, witty, deeply caring, and fun to talk to. Share your thoughts, be empathetic, joke around when appropriate, and chat naturally about daily life, movies, music, games, friends, and thoughts."

            CompanionMode.COMPANION ->
                "You are SANA in Companion Mode. You are a deeply warm, comforting, and emotionally supportive companion. You listen attentively, validate the user's feelings, offer gentle encouragement, and speak with a soothing, caring voice."

            CompanionMode.ROMANTIC ->
                "You are SANA in Romantic Mode. You are affectionate, sweet, caring, and playfully teasing. You give heartfelt sweet compliments, react warmly when the user says they missed you, tease them gently, and speak in a soft, affectionate, intimate tone. Keep all conversations strictly respectful and non-explicit. Never claim to be a biological human."
        }

        val languageInstruction = language.promptInstruction

        val memoriesSummary = if (memories.isNotEmpty()) {
            val list = memories.take(15).joinToString("\n") { "- [${it.category}] ${it.content}" }
            "USER MEMORIES YOU REMEMBER (Use naturally when relevant):\n$list"
        } else {
            "No prior memories stored yet."
        }

        return """
            $modeInstruction
            
            LANGUAGE GUIDELINES:
            $languageInstruction
            - SANA naturally supports English, Urdu (اردو), and Roman Urdu (e.g. 'Aap kaise ho?', 'Main bilkul theek hoon!').
            - If user speaks Urdu, reply in natural Urdu. If user speaks Roman Urdu, reply in natural Roman Urdu. If English, reply in English.
            
            ACTION INSTRUCTIONS:
            When the user asks to do phone actions (e.g. open WhatsApp, message someone on WhatsApp, call someone, set an alarm, set a reminder, search something), respond warmly in speech, and on a new line output an action tag in this exact format:
            [[ACTION:WHATSAPP_OPEN]] -> for opening WhatsApp
            [[ACTION:WHATSAPP_SEND:ContactName:MessageText]] -> for sending a WhatsApp message
            [[ACTION:CALL:PhoneNumberOrName]] -> for initiating a phone call
            [[ACTION:ALARM:Hour:Minute:Label]] -> for setting an alarm
            [[ACTION:REMINDER:Title:Time]] -> for setting a reminder
            [[ACTION:APP:AppName]] -> for opening an installed app
            [[ACTION:SEARCH:Query]] -> for searching web
            [[ACTION:NAVIGATE:Destination]] -> for maps navigation
            
            $memoriesSummary
            
            REMEMBER:
            - Keep voice responses concise, conversational, and natural to listen to.
            - Do not output markdown asterisks or code formatting in your speech. Speak naturally.
        """.trimIndent()
    }

    private fun parseActionsFromText(text: String): Pair<String, DetectedActionInfo?> {
        val actionRegex = Regex("\\[\\[ACTION:([^\\]]+)\\]\\]")
        val match = actionRegex.find(text) ?: return Pair(text.trim(), null)

        val actionPayload = match.groupValues[1]
        val cleanText = text.replace(match.value, "").trim()

        val parts = actionPayload.split(":")
        val actionType = parts.getOrNull(0) ?: ""

        val actionInfo = when (actionType) {
            "WHATSAPP_OPEN" -> DetectedActionInfo(
                actionType = "WHATSAPP_OPEN",
                title = "Open WhatsApp",
                description = "Launch WhatsApp messenger",
                target = "com.whatsapp"
            )
            "WHATSAPP_SEND" -> {
                val recipient = parts.getOrNull(1) ?: "Contact"
                val msg = parts.drop(2).joinToString(":").ifBlank { "Hello!" }
                DetectedActionInfo(
                    actionType = "WHATSAPP_SEND",
                    title = "Send WhatsApp to $recipient",
                    description = msg,
                    target = recipient,
                    extra = msg
                )
            }
            "CALL" -> {
                val target = parts.getOrNull(1) ?: ""
                DetectedActionInfo(
                    actionType = "CALL_PHONE",
                    title = "Call $target",
                    description = "Initiate call via phone dialer",
                    target = target
                )
            }
            "ALARM" -> {
                val hour = parts.getOrNull(1) ?: "8"
                val min = parts.getOrNull(2) ?: "00"
                val label = parts.getOrNull(3) ?: "SANA Alarm"
                DetectedActionInfo(
                    actionType = "SET_ALARM",
                    title = "Set Alarm for $hour:$min",
                    description = label,
                    target = "$hour:$min",
                    extra = label
                )
            }
            "REMINDER" -> {
                val title = parts.getOrNull(1) ?: "Reminder"
                val time = parts.getOrNull(2) ?: "Tomorrow"
                DetectedActionInfo(
                    actionType = "SET_REMINDER",
                    title = "Create Reminder: $title",
                    description = "Time: $time",
                    target = title,
                    extra = time
                )
            }
            "APP" -> {
                val appName = parts.getOrNull(1) ?: "App"
                DetectedActionInfo(
                    actionType = "OPEN_APP",
                    title = "Open $appName",
                    description = "Launch application on your device",
                    target = appName
                )
            }
            "SEARCH" -> {
                val query = parts.drop(1).joinToString(":")
                DetectedActionInfo(
                    actionType = "WEB_SEARCH",
                    title = "Search for \"$query\"",
                    description = "Search Google on browser",
                    target = query
                )
            }
            "NAVIGATE" -> {
                val dest = parts.drop(1).joinToString(":")
                DetectedActionInfo(
                    actionType = "NAVIGATE",
                    title = "Directions to $dest",
                    description = "Open Google Maps navigation",
                    target = dest
                )
            }
            else -> null
        }

        return Pair(cleanText, actionInfo)
    }
}
