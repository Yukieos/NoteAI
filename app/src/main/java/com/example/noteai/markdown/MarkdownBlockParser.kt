package com.example.noteai.markdown

// 高效的 Markdown 块解析器
// 这个解析器的思想很简单：把 Markdown 文本按块分割
// 每个块是一个独立的内容（文本、代码、标题等）
// 这样 RecyclerView 就能按块渲染，而不是一次性渲染整个文档
class MarkdownBlockParser {
    
    //用来处理行内元素的解析器（加粗、斜体、行内代码等）
    private val markdownParser = MarkdownParser()
    
    fun parseToBlocks(markdown: String): List<ContentBlock> {
        if (markdown.isBlank()) return emptyList()
        
        val blocks = mutableListOf<ContentBlock>()
        val lines = markdown.split("\n")
        var i = 0
        
        while (i < lines.size) {
            val line = lines[i]
            
            when {
                //代码块：从 ``` 开始，到下一个 ``` 结束
                //中间的所有行都属于这个代码块
                line.startsWith("```") -> {
                    val codeLines = mutableListOf<String>()
                    i++
                    while (i < lines.size && !lines[i].startsWith("```")) {
                        codeLines.add(lines[i])
                        i++
                    }
                    blocks.add(ContentBlock.CodeBlock(codeLines.joinToString("\n")))
                    i++
                }
                
                //标题：根据 # 的数量判断标题级别
                line.startsWith("# ") -> {
                    blocks.add(ContentBlock.HeadingBlock(line.substring(2), 1))
                    i++
                }
                line.startsWith("## ") -> {
                    blocks.add(ContentBlock.HeadingBlock(line.substring(3), 2))
                    i++
                }
                line.startsWith("### ") -> {
                    blocks.add(ContentBlock.HeadingBlock(line.substring(4), 3))
                    i++
                }
                line.startsWith("#### ") -> {
                    blocks.add(ContentBlock.HeadingBlock(line.substring(5), 4))
                    i++
                }
                line.startsWith("##### ") -> {
                    blocks.add(ContentBlock.HeadingBlock(line.substring(6), 5))
                    i++
                }
                line.startsWith("###### ") -> {
                    blocks.add(ContentBlock.HeadingBlock(line.substring(7), 6))
                    i++
                }
                
                //列表：连续的 - 项被合并成一个列表块
                //这样看起来更整齐，不用一行行处理
                line.startsWith("- ") || line.startsWith("* ") -> {
                    val listItems = mutableListOf<String>()
                    while (i < lines.size && 
                           (lines[i].startsWith("- ") || lines[i].startsWith("* "))) {
                        val prefix = if (lines[i].startsWith("- ")) 2 else 2
                        listItems.add(lines[i].substring(prefix))
                        i++
                    }
                    blocks.add(ContentBlock.ListItemBlock(listItems))
                }
                
                //引用：用 > 开头的行
                line.startsWith("> ") -> {
                    blocks.add(ContentBlock.QuoteBlock(line.substring(2)))
                    i++
                }
                
                //普通文本：就是任何不符合上面规则的非空行
                //这里用 MarkdownParser 处理行内的富文本（加粗、斜体、代码等）
                line.isNotBlank() -> {
                    val richText = markdownParser.parse(line)
                    blocks.add(ContentBlock.TextBlock(richText))
                    i++
                }
                
                //空行：直接跳过
                else -> i++
            }
        }
        
        return blocks
    }
}

