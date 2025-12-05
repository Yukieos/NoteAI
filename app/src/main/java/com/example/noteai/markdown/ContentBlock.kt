package com.example.noteai.markdown

// Markdown 内容的各种块类型
// 把文档分成一个个独立的块，这样 RecyclerView 可以按需加载
// 比起一次性加载整个文本，块级加载能大幅提升性能（特别是长文档）
sealed class ContentBlock {
    // 普通的文本块，就是一行或几行的内容
    data class TextBlock(
        val text: CharSequence,
        val id: String = System.nanoTime().toString()
    ) : ContentBlock()

    // 标题块，有不同的级别（1-3 对应 # ## ###）
    data class HeadingBlock(
        val text: String,
        val level: Int,  // 1 = #   2 = ##   3 = ###
        val id: String = System.nanoTime().toString()
    ) : ContentBlock()

    // 代码块，通常是用 ``` 包裹的内容
    data class CodeBlock(
        val code: String,
        val language: String = "",  //可能会注明编程语言，比如 python、java
        val id: String = System.nanoTime().toString()
    ) : ContentBlock()

    // 列表块，把连续的列表项合并成一个块
    data class ListItemBlock(
        val items: List<String>,
        val id: String = System.nanoTime().toString()
    ) : ContentBlock()

    // 引用块，用 > 开头的内容
    data class QuoteBlock(
        val text: String,
        val id: String = System.nanoTime().toString()
    ) : ContentBlock()

    // 占位符块，用来处理异步加载的内容，比如图片
    // 等图片加载完了再替换这个占位符
    data class PlaceholderBlock(
        val id: String = System.nanoTime().toString()
    ) : ContentBlock()
}

