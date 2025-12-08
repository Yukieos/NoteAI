package com.example.noteai.markdown

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.widget.TextView
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.ext.tables.TablePlugin
import org.commonmark.node.StrongEmphasis

class MarkdownHelper(context: Context) {

    // 初始化 Markwon，配置自定义插件来实现你的特殊样式需求
    private val markwon: Markwon = Markwon.builder(context)
        .usePlugin(TablePlugin.create(context))
        .usePlugin(object : AbstractMarkwonPlugin() {
            // 在这里拦截 Markdown 的渲染逻辑
            override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
                // 针对 Markdown 中的 **加粗** (StrongEmphasis) 进行特殊处理
                builder.setFactory(StrongEmphasis::class.java) { _, _ ->
                    arrayOf(
                        StyleSpan(Typeface.BOLD),           // 保持加粗
                        UnderlineSpan(),                    // 【新增】添加下划线
                        ForegroundColorSpan(Color.BLACK)    // 【新增】颜色改为深纯黑色
                    )
                }
            }
        })
        .build()

    /**
     * 核心功能：根据 ContentBlock 的类型，用不同的策略渲染到 TextView 上
     */
    fun renderBlock(textView: TextView, block: ContentBlock) {

        // 1. 先设置所有文字的“基准样式” (你的需求：普通文字也要加粗，且为浅灰色)
        textView.setTextColor(Color.parseColor("#808080")) // 浅灰色
        textView.typeface = Typeface.DEFAULT_BOLD          // 全局强制加粗

        // 2. 根据块类型进行渲染
        when (block) {
            is ContentBlock.TextBlock -> {
                // 普通文本
                markwon.setMarkdown(textView, block.text.toString())
            }

            is ContentBlock.HeadingBlock -> {
                // 标题块：标题通常需要比正文更黑，这里手动设为黑色，防止被全局浅灰覆盖
                textView.setTextColor(Color.BLACK)
                val prefix = "#".repeat(block.level)
                markwon.setMarkdown(textView, "$prefix ${block.text}")
            }

            is ContentBlock.CodeBlock -> {
                // 代码块：修复了之前的拼接错误，增加了开头的
                val markdownCode = "```${block.language}\n${block.code}\n```"
                markwon.setMarkdown(textView, markdownCode)
            }

            is ContentBlock.ListItemBlock -> {
                // 列表块
                val listContent = block.items.joinToString("\n") { "- $it" }
                markwon.setMarkdown(textView, listContent)
            }

            is ContentBlock.QuoteBlock -> {
                // 引用块
                markwon.setMarkdown(textView, "> ${block.text}")
            }

            is ContentBlock.PlaceholderBlock -> {
                textView.text = "Loading..."
            }
        }
    }
}