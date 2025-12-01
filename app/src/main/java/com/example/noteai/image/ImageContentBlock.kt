package com.example.noteai.image

import com.example.noteai.markdown.ContentBlock

/**
 * 图片内容块 - 用于在Markdown渲染中表示图片
 */
sealed class ImageContentBlock {
    // 图片块数据类
    data class ImageBlock(
        val imagePath: String,
        val altText: String = "",
        val id: String = System.nanoTime().toString()
    )
    
    // 图片渲染状态
    enum class ImageStatus {
        LOADING,
        LOADED,
        ERROR
    }
}

// 扩展函数：将ImageBlock转换为ContentBlock.TextBlock
fun ImageContentBlock.ImageBlock.toContentBlock(): ContentBlock.TextBlock {
    return ContentBlock.TextBlock("![$altText]($imagePath)", id)
}

// 扩展函数：从ContentBlock中提取可能的图片信息
fun ContentBlock.extractImageInfo(): Pair<String, String>? {
    if (this is ContentBlock.TextBlock) {
        val match = Regex("!\\[([^\\]]*)\\]\\(([^\\)]*)\\)").find(text.toString())
        if (match != null && match.groupValues.size >= 3) {
            return Pair(match.groupValues[2], match.groupValues[1]) // (path, altText)
        }
    }
    return null
}
