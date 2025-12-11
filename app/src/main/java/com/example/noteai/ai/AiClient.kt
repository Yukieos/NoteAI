package com.example.noteai.ai

/**
 * 对外暴露的 AI 能力接口。
 * 不依赖 Android UI / Room，只用普通字符串，这样就和其他功能解耦。
 */
interface AiClient {

    //用AI给笔记生成 1–2 句摘要，保持原文语言
    suspend fun summarizeNote(
        title: String,
        content: String
    ): String

    //根据笔记内容生成主题标签（1–3 个词），返回的是纯文本标签列表。
    suspend fun suggestTopics(
        title: String,
        content: String
    ): List<String>
}
