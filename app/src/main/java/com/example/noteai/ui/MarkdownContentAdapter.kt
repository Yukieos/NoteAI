package com.example.noteai.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.noteai.R
import com.example.noteai.image.ImageUtils
import com.example.noteai.markdown.ContentBlock
import com.example.noteai.markdown.MarkdownHelper

// 高效的 Markdown 内容适配器
class MarkdownContentAdapter(
    private val context: Context, // 【关键修改】这里需要 Context
    private val blocks: List<ContentBlock>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // 【关键修改】初始化我们写好的 MarkdownHelper
    private val markdownHelper = MarkdownHelper(context)

    companion object {
        private const val TYPE_TEXT = 0
        private const val TYPE_HEADING = 1
        private const val TYPE_CODE = 2
        private const val TYPE_LIST = 3
        private const val TYPE_QUOTE = 4
        private const val TYPE_IMAGE = 5
    }

    override fun getItemViewType(position: Int): Int {
        val block = blocks[position]
        if (block is ContentBlock.TextBlock && block.text.toString().matches(Regex("!\\[.*?\\]\\(.*?\\)"))) {
            return TYPE_IMAGE
        }
        return when (block) {
            is ContentBlock.HeadingBlock -> TYPE_HEADING
            is ContentBlock.CodeBlock -> TYPE_CODE
            is ContentBlock.ListItemBlock -> TYPE_LIST
            is ContentBlock.QuoteBlock -> TYPE_QUOTE
            is ContentBlock.PlaceholderBlock -> TYPE_TEXT
            else -> TYPE_TEXT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        // 布局保持不变，依然使用 simple_list_item_1 或自定义布局
        return when (viewType) {
            TYPE_IMAGE -> ImageViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_image_block, parent, false)
            )
            // 其他所有文本类型的 ViewHolder 其实都可以共用同一种布局，这里为了逻辑清晰保留你的分类
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(android.R.layout.simple_list_item_1, parent, false)
                when (viewType) {
                    TYPE_HEADING -> HeadingViewHolder(view)
                    TYPE_CODE -> CodeViewHolder(view)
                    TYPE_LIST -> ListViewHolder(view)
                    TYPE_QUOTE -> QuoteViewHolder(view)
                    else -> TextViewHolder(view)
                }
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val block = blocks[position]

        // 【核心修改】不再调用 holder.bind() 手动设置样式
        // 而是直接把 textView 和 block 交给 markdownHelper 处理
        when (holder) {
            is TextViewHolder -> markdownHelper.renderBlock(holder.textView, block)
            is HeadingViewHolder -> markdownHelper.renderBlock(holder.textView, block)
            is CodeViewHolder -> markdownHelper.renderBlock(holder.textView, block)
            is ListViewHolder -> markdownHelper.renderBlock(holder.textView, block)
            is QuoteViewHolder -> markdownHelper.renderBlock(holder.textView, block)

            is ImageViewHolder -> {
                if (block is ContentBlock.TextBlock) {
                    holder.bind(block)
                }
            }
        }
    }

    override fun getItemCount() = blocks.size

    // ---------------- ViewHolder 定义 ----------------
    // 注意：把原来的 bind 方法删掉了，因为渲染逻辑移到了 MarkdownHelper
    // 并且把 textView 设为 public (val 默认就是 public getter)

    inner class TextViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(android.R.id.text1)
    }

    inner class HeadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(android.R.id.text1)
    }

    inner class CodeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(android.R.id.text1)
    }

    inner class ListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(android.R.id.text1)
    }

    inner class QuoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(android.R.id.text1)
    }

    // 图片逻辑保持不变，因为 Markwon 主要处理文字渲染
    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageView_content)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        private val errorText: TextView = itemView.findViewById(R.id.textView_error)

        fun bind(block: ContentBlock.TextBlock) {
            val text = block.text.toString()
            val match = Regex("!\\[(.*?)\\]\\((.*?)\\)").find(text)
            if (match != null && match.groupValues.size >= 3) {
                val imagePath = match.groupValues[2]
                val altText = match.groupValues[1]

                progressBar.visibility = View.VISIBLE
                imageView.visibility = View.GONE
                errorText.visibility = View.GONE
                imageView.contentDescription = altText

                try {
                    val bitmap = ImageUtils.loadBitmapFromPath(imagePath)
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                        imageView.visibility = View.VISIBLE
                        progressBar.visibility = View.GONE
                    } else {
                        errorText.text = "加载失败: $altText"
                        errorText.visibility = View.VISIBLE
                        progressBar.visibility = View.GONE
                    }
                } catch (e: Exception) {
                    errorText.text = "错误"
                    errorText.visibility = View.VISIBLE
                    progressBar.visibility = View.GONE
                }
            }
        }
    }
}
