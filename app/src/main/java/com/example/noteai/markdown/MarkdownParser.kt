package com.example.noteai.markdown

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.RelativeSizeSpan
import android.text.style.TypefaceSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.graphics.Color

// 这个类负责把 Markdown 文本转换成 Android 能直接显示的富文本
// 核心思想就是：parse -> 识别各种 Markdown 语法 -> 转换成 SpannableString
class MarkdownParser {
    
    // Markdown 支持的各种元素类型，方便后面标记用
    private enum class ElementType {
        HEADING1, HEADING2, HEADING3,  // # ## ###
        BOLD,                          // **text** or __text__
        ITALIC,                        // *text* or _text_
        CODE_INLINE,                   // `code`
        CODE_BLOCK,                    // ``` code block ```
        LIST_ITEM,                     // - item or * item
        QUOTE                          // > quote
    }

    fun parse(markdown: String): CharSequence {
        if (markdown.isBlank()) return ""
        
        val result = SpannableStringBuilder()
        val lines = markdown.split("\n")
        var i = 0
        
        while (i < lines.size) {
            val line = lines[i]
            
            // 代码块特殊处理，因为它可能占多行
            if (line.startsWith("```")) {
                val codeLines = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].startsWith("```")) {
                    codeLines.add(lines[i])
                    i++
                }
                
                // 渲染成代码块样式
                if (result.isNotEmpty()) result.append("\n")
                val codeBlockStart = result.length
                result.append(codeLines.joinToString("\n"))
                
                // 整个代码块用 monospace 字体
                result.setSpan(
                    TypefaceSpan("monospace"),
                    codeBlockStart,
                    result.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                // 缩小一点字号，模仿代码块效果
                result.setSpan(
                    RelativeSizeSpan(0.9f),
                    codeBlockStart,
                    result.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                result.append("\n")
                i++
                continue
            }
            
            // 处理标题行 # ## ###
            when {
                line.startsWith("# ") -> {
                    if (result.isNotEmpty()) result.append("\n")
                    addHeading(result, line.substring(2), 1.4f)
                    result.append("\n")
                }
                line.startsWith("## ") -> {
                    if (result.isNotEmpty()) result.append("\n")
                    addHeading(result, line.substring(3), 1.2f)
                    result.append("\n")
                }
                line.startsWith("### ") -> {
                    if (result.isNotEmpty()) result.append("\n")
                    addHeading(result, line.substring(4), 1.1f)
                    result.append("\n")
                }
                // 处理列表 - 或 *
                line.startsWith("- ") || line.startsWith("* ") -> {
                    if (result.isNotEmpty()) result.append("\n")
                    val listText = "• " + line.substring(2)
                    result.append(listText)
                    result.append("\n")
                }
                // 处理引用 >
                line.startsWith("> ") -> {
                    if (result.isNotEmpty()) result.append("\n")
                    val quoteStart = result.length
                    result.append(line.substring(2))
                    // 引用用斜体表示
                    result.setSpan(
                        StyleSpan(Typeface.ITALIC),
                        quoteStart,
                        result.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    result.append("\n")
                }
                // 普通段落
                line.isNotBlank() -> {
                    if (result.isNotEmpty() && !result.endsWith("\n\n")) {
                        result.append("\n")
                    }
                    val paragraphStart = result.length
                    val processed = processInlineMarkdown(line)
                    result.append(processed)
                }
                // 空行，保留间距
                else -> {
                    if (result.isNotEmpty() && !result.endsWith("\n")) {
                        result.append("\n")
                    }
                }
            }
            i++
        }
        
        return result
    }

    // 处理行内的 Markdown：**加粗** *斜体* `代码`
    private fun processInlineMarkdown(text: String): SpannableStringBuilder {
        val result = SpannableStringBuilder()
        var i = 0
        
        while (i < text.length) {
            // 检查 **加粗** 模式
            if (i < text.length - 1 && text[i] == '*' && text[i + 1] == '*') {
                val endIndex = text.indexOf("**", i + 2)
                if (endIndex != -1) {
                    val boldText = text.substring(i + 2, endIndex)
                    val startPos = result.length
                    result.append(boldText)
                    result.setSpan(
                        StyleSpan(Typeface.BOLD),
                        startPos,
                        result.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    i = endIndex + 2
                    continue
                }
            }
            
            // 检查 *斜体* 模式（但要避免 ** 的干扰）
            if (i < text.length - 1 && text[i] == '*' && text[i + 1] != '*') {
                val endIndex = text.indexOf("*", i + 1)
                if (endIndex != -1 && (endIndex + 1 >= text.length || text[endIndex + 1] != '*')) {
                    val italicText = text.substring(i + 1, endIndex)
                    val startPos = result.length
                    result.append(italicText)
                    result.setSpan(
                        StyleSpan(Typeface.ITALIC),
                        startPos,
                        result.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    i = endIndex + 1
                    continue
                }
            }
            
            // 检查 `代码` 模式
            if (text[i] == '`') {
                val endIndex = text.indexOf("`", i + 1)
                if (endIndex != -1) {
                    val codeText = text.substring(i + 1, endIndex)
                    val startPos = result.length
                    result.append(codeText)
                    // 行内代码用 monospace 字体
                    result.setSpan(
                        TypefaceSpan("monospace"),
                        startPos,
                        result.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    // 浅蓝色字体
                    result.setSpan(
                        ForegroundColorSpan(Color.parseColor("#4A90E2")),
                        startPos,
                        result.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    // 浅灰色背景
                    result.setSpan(
                        BackgroundColorSpan(Color.parseColor("#F0F0F0")),
                        startPos,
                        result.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    i = endIndex + 1
                    continue
                }
            }
            
            // 普通字符，直接添加
            result.append(text[i])
            i++
        }
        
        return result
    }

    // 添加标题，并调整大小和粗体
    private fun addHeading(
        result: SpannableStringBuilder,
        text: String,
        sizeMultiplier: Float
    ) {
        val startPos = result.length
        result.append(text)
        
        // 加粗
        result.setSpan(
            StyleSpan(Typeface.BOLD),
            startPos,
            result.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        
        // 调整大小
        result.setSpan(
            RelativeSizeSpan(sizeMultiplier),
            startPos,
            result.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    // 这个函数用来把 Markdown 转换成纯文本，适合用在列表预览
    // 主要是去掉所有 Markdown 语法符号
    fun parseToPlainText(markdown: String): String {
        if (markdown.isBlank()) return ""
        
        var text = markdown
        
        // 移除标题符号
        text = text.replace(Regex("^#+\\s+"), "")
        
        // 移除加粗符号
        text = text.replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
        text = text.replace(Regex("__(.+?)__"), "$1")
        
        // 移除斜体符号
        text = text.replace(Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)"), "$1")
        text = text.replace(Regex("_(.+?)_"), "$1")
        
        // 移除行内代码符号
        text = text.replace(Regex("`(.+?)`"), "$1")
        
        // 移除代码块符号
        text = text.replace(Regex("```[\\s\\S]*?```"), "")
        
        // 移除列表符号
        text = text.replace(Regex("^[-*]\\s+", RegexOption.MULTILINE), "")
        
        // 移除引用符号
        text = text.replace(Regex("^>\\s+", RegexOption.MULTILINE), "")
        
        // 只取第一行或者前 100 个字符作为预览
        return text.split("\n").firstOrNull()?.take(100) ?: ""
    }
}

