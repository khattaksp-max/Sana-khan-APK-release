package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiGenerateRequest(
    @param:Json(name = "contents") val contents: List<GeminiContent>,
    @param:Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null,
    @param:Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @param:Json(name = "role") val role: String? = null,
    @param:Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @param:Json(name = "text") val text: String? = null,
    @param:Json(name = "inlineData") val inlineData: GeminiInlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    @param:Json(name = "mimeType") val mimeType: String,
    @param:Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @param:Json(name = "responseModalities") val responseModalities: List<String>? = null,
    @param:Json(name = "speechConfig") val speechConfig: GeminiSpeechConfig? = null,
    @param:Json(name = "temperature") val temperature: Float? = null,
    @param:Json(name = "topP") val topP: Float? = null,
    @param:Json(name = "topK") val topK: Int? = null
)

@JsonClass(generateAdapter = true)
data class GeminiSpeechConfig(
    @param:Json(name = "voiceConfig") val voiceConfig: GeminiVoiceConfig
)

@JsonClass(generateAdapter = true)
data class GeminiVoiceConfig(
    @param:Json(name = "prebuiltVoiceConfig") val prebuiltVoiceConfig: GeminiPrebuiltVoiceConfig
)

@JsonClass(generateAdapter = true)
data class GeminiPrebuiltVoiceConfig(
    @param:Json(name = "voiceName") val voiceName: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateResponse(
    @param:Json(name = "candidates") val candidates: List<GeminiCandidate>? = null,
    @param:Json(name = "error") val error: GeminiErrorResponse? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @param:Json(name = "content") val content: GeminiContent? = null,
    @param:Json(name = "finishReason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiErrorResponse(
    @param:Json(name = "code") val code: Int? = null,
    @param:Json(name = "message") val message: String? = null,
    @param:Json(name = "status") val status: String? = null
)

data class GeminiVoiceResult(
    val spokenAudioBase64: String?,
    val audioMimeType: String?,
    val transcriptText: String,
    val detectedAction: DetectedActionInfo? = null
)

data class DetectedActionInfo(
    val actionType: String,
    val title: String,
    val description: String,
    val target: String,
    val extra: String? = null
)
