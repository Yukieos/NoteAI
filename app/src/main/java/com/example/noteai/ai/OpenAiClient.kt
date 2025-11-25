package com.example.noteai.ai

import com.example.noteai.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * 用 OkHttp + Chat Completions 调用 OpenAI。
 * 只实现 AiClient 接口，不依赖任何 Activity / Room。
 */
class OpenAiClient(
    private val client: OkHttpClient = OkHttpClient()
) : AiClient {

    private val baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"

    private val model = "qwen-plus"
// 你可以根据自己账户情况换成别的

    override suspend fun summarizeNote(title: String, content: String): String =
        withContext(Dispatchers.IO) {
            val prompt = buildString {
                appendLine("You are a note-taking assistant.")
                appendLine("Summarize the following note in 1-2 sentences in the SAME language as the note.")
                appendLine()
                appendLine("Title: ${title.ifBlank { "(no title)" }}")
                appendLine("Content:")
                append(content.ifBlank { "(empty)" })
            }

            val requestJson = buildChatRequest(prompt)

            val responseBody = postJson(requestJson)
            parseChatFirstMessage(responseBody)
        }

    override suspend fun suggestTopics(title: String, content: String): List<String> =
        withContext(Dispatchers.IO) {
            val prompt = buildString {
                appendLine("Read this note and output 3-5 short topic tags, 1-3 words each.")
                appendLine("Return ONLY the tags separated by commas, no explanations.")
                appendLine()
                appendLine("Title: ${title.ifBlank { "(no title)" }}")
                appendLine("Content:")
                append(content.ifBlank { "(empty)" })
            }

            val requestJson = buildChatRequest(prompt)

            val responseBody = postJson(requestJson)
            val text = parseChatFirstMessage(responseBody)

            // 按中英文逗号切分，做一点清洗
            text.split(',', '，')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }

    /**
     * 构造 chat/completions 请求体
     */
    private fun buildChatRequest(userPrompt: String): JSONObject {
        return JSONObject().apply {
            put("model", model)
            put(
                "messages",
                JSONArray().apply {
                    put(
                        JSONObject().apply {
                            put("role", "system")
                            put("content", "You are a helpful assistant for summarizing and tagging user notes.")
                        }
                    )
                    put(
                        JSONObject().apply {
                            put("role", "user")
                            put("content", userPrompt)
                        }
                    )
                }
            )
        }
    }

    /**
     * 发起 HTTP POST 请求
     */
    private fun postJson(json: JSONObject): String {
        val apiKey = BuildConfig.OPENAI_API_KEY
        if (BuildConfig.OPENAI_API_KEY.isBlank()) {
            throw IllegalStateException("OPENAI_API_KEY is empty. Please set it in local.properties.")
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(baseUrl)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                throw IOException("OpenAI API error ${response.code}: $errorBody")
            }
            return response.body?.string()
                ?: throw IOException("Empty response body from OpenAI")
        }
    }

    /**
     * 从 chat/completions 响应中取出第一条 message.content
     */
    private fun parseChatFirstMessage(responseBody: String): String {
        val root = JSONObject(responseBody)
        val choices = root.getJSONArray("choices")
        if (choices.length() == 0) return ""
        val first = choices.getJSONObject(0)
        val message = first.getJSONObject("message")
        return message.getString("content")
    }
}
