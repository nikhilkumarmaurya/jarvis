package com.jarvis.voice

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ChatMessage(val role: String, val text: String)

object GeminiApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"

    fun getApiKey(context: Context): String {
        val prefs = context.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)
        val saved = prefs.getString("gemini_api_key", "") ?: ""
        if (saved.isNotBlank()) return saved
        return BuildConfig.GEMINI_API_KEY
    }

    suspend fun chat(
        context: Context,
        history: List<ChatMessage>,
        userMessage: String,
        screenContext: String = ""
    ): String = withContext(Dispatchers.IO) {

        val apiKey = getApiKey(context)
        if (apiKey.isBlank()) return@withContext "⚠️ API key not set. Go to Settings and add your Gemini API key."

        val systemPrompt = """
You are Jarvis, an AI assistant that controls an Android phone.
You help users by:
1. Answering questions conversationally
2. Performing phone actions like opening apps, sending WhatsApp/SMS messages, setting alarms/timers

When user wants a PHONE ACTION, respond in this exact JSON format:
{"action": "ACTION_TYPE", "params": {...}, "reply": "What you say to user"}

Available actions:
- OPEN_APP: {"action": "OPEN_APP", "params": {"package": "com.whatsapp", "appName": "WhatsApp"}, "reply": "Opening WhatsApp"}
- SEND_WHATSAPP: {"action": "SEND_WHATSAPP", "params": {"contact": "name or number", "message": "text"}, "reply": "Sending message"}
- SEND_SMS: {"action": "SEND_SMS", "params": {"number": "phone number", "message": "text"}, "reply": "Sending SMS"}
- SET_ALARM: {"action": "SET_ALARM", "params": {"hour": 7, "minute": 30, "label": "Wake up"}, "reply": "Alarm set"}
- SET_TIMER: {"action": "SET_TIMER", "params": {"seconds": 300}, "reply": "Timer started"}
- CLICK: {"action": "CLICK", "params": {"text": "element text to click"}, "reply": "Clicking"}
- SCROLL_DOWN: {"action": "SCROLL_DOWN", "params": {}, "reply": "Scrolling down"}
- SCROLL_UP: {"action": "SCROLL_UP", "params": {}, "reply": "Scrolling up"}
- BACK: {"action": "BACK", "params": {}, "reply": "Going back"}
- HOME: {"action": "HOME", "params": {}, "reply": "Going to home screen"}
- TYPE_TEXT: {"action": "TYPE_TEXT", "params": {"text": "text to type"}, "reply": "Typing"}

For normal conversation, just reply in plain text without JSON.

${if (screenContext.isNotBlank()) "Current screen content: $screenContext" else ""}
""".trimIndent()

        val contents = JSONArray()
        history.takeLast(10).forEach { msg ->
            val part = JSONObject().put("text", msg.text)
            val content = JSONObject()
                .put("role", if (msg.role == "user") "user" else "model")
                .put("parts", JSONArray().put(part))
            contents.put(content)
        }
        val userPart = JSONObject().put("text", userMessage)
        val userContent = JSONObject().put("role", "user").put("parts", JSONArray().put(userPart))
        contents.put(userContent)
        val systemInstruction = JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
        val body = JSONObject().put("contents", contents).put("systemInstruction", systemInstruction).put("generationConfig", JSONObject().put("temperature", 0.7).put("maxOutputTokens", 1024))
        val request = Request.Builder().url("$BASE_URL?key=$apiKey").post(body.toString().toRequestBody("application/json".toMediaType())).build()
        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext "No response"
            if (!response.isSuccessful) { val err = JSONObject(responseBody); return@withContext "❌ ${err.optJSONObject("error")?.optString("message")}" }
            return@withContext JSONObject(responseBody).getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text").trim()
        } catch (e: Exception) { return@withContext "❌ ${e.message}" }
    }
}
