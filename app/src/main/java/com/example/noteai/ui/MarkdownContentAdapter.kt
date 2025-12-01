package com.example.noteai.ui

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.example.noteai.R
import com.example.noteai.image.ImageContentBlock
import com.example.noteai.markdown.ContentBlock
import android.graphics.drawable.GradientDrawable
import android.widget.ProgressBar
import android.widget.Toast
import com.example.noteai.image.ImageUtils

// 高效的 Markdown 内容适配器，用来在预览模式渲染各种内容块
// 核心思想就是利用 RecyclerView 的虚拟滚动，只渲染屏幕可见的块
// 这样即使文档很长也能保持流畅，因为内存里只保留几个 item
class MarkdownContentAdapter(
    private val blocks: List<ContentBlock>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_TEXT = 0      //普通文本
        private const val TYPE_HEADING = 1   //标题
        private const val TYPE_CODE = 2      //代码块
        private const val TYPE_LIST = 3      //列表
        private const val TYPE_QUOTE = 4     //引用
        private const val TYPE_IMAGE = 5     //图片
    }

    //根据内容块的类型返回对应的 ViewHolder 类型
    override fun getItemViewType(position: Int): Int {
        val block = blocks[position]
        //检查是否是图片语法的文本块
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

    //为每种类型创建对应的 ViewHolder，这样不同的块可以有不同的样式
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_TEXT -> TextViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(android.R.layout.simple_list_item_1, parent, false)
            )
            TYPE_HEADING -> HeadingViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(android.R.layout.simple_list_item_1, parent, false)
            )
            TYPE_CODE -> CodeViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(android.R.layout.simple_list_item_1, parent, false)
            )
            TYPE_LIST -> ListViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(android.R.layout.simple_list_item_1, parent, false)
            )
            TYPE_QUOTE -> QuoteViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(android.R.layout.simple_list_item_1, parent, false)
            )
            TYPE_IMAGE -> ImageViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_image_block, parent, false)
            )
            else -> TextViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(android.R.layout.simple_list_item_1, parent, false)
            )
        }
    }

    //把数据绑定到对应的 ViewHolder，让每个块显示出来
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val block = blocks[position]
        when (holder) {
            is TextViewHolder -> holder.bind(block as ContentBlock.TextBlock)
            is HeadingViewHolder -> holder.bind(block as ContentBlock.HeadingBlock)
            is CodeViewHolder -> holder.bind(block as ContentBlock.CodeBlock)
            is ListViewHolder -> holder.bind(block as ContentBlock.ListItemBlock)
            is QuoteViewHolder -> holder.bind(block as ContentBlock.QuoteBlock)
            is ImageViewHolder -> {
                // 确保只传递TextBlock类型
                if (block is ContentBlock.TextBlock) {
                    holder.bind(block)
                }
            }
        }
    }

    override fun getItemCount() = blocks.size

    //下面是各个 ViewHolder，每个负责一种内容块的渲染

    inner class TextViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(android.R.id.text1)

        fun bind(block: ContentBlock.TextBlock) {
            textView.text = block.text
            textView.textSize = 16f
            //给文本加点内边距，看起来不会那么紧凑
            textView.setPadding(12, 8, 12, 8)
        }
    }

    inner class HeadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(android.R.id.text1)

        fun bind(block: ContentBlock.HeadingBlock) {
            textView.text = block.text
            //标题用粗体显示
            textView.setTypeface(null, Typeface.BOLD)
            //根据标题级别调整字号，级别越高（# 级别）字就越大
            textView.textSize = when (block.level) {
                1 -> 28f  //# 最大
                2 -> 24f  //## 中等
                3 -> 20f  //### 相对小一点
                4 -> 18f  //#### 更小
                5 -> 16f  //##### 最小
                6 -> 14f  //###### 最小的标题
                else -> 20f //默认大小
            }
            //给标题留更多顶部空间
            textView.setPadding(12, 16, 12, 8)
        }
    }

    inner class CodeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(android.R.id.text1)

        fun bind(block: ContentBlock.CodeBlock) {
            textView.text = block.code
            //代码块用等宽字体
            textView.typeface = Typeface.MONOSPACE
            textView.textSize = 12f  //代码字号稍微小一点
            //浅灰色背景
            textView.setBackgroundColor("#F5F5F5".toColorInt())
            textView.setPadding(12, 12, 12, 12)
        }
    }

    inner class ListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(android.R.id.text1)

        fun bind(block: ContentBlock.ListItemBlock) {
            //把列表的每一项用 • 符号标记，然后用换行分开
            val listText = block.items.joinToString("\n") { "• $it" }
            textView.text = listText
            textView.textSize = 16f
            textView.setPadding(12, 8, 12, 8)
        }
    }

    inner class QuoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(android.R.id.text1)

        fun bind(block: ContentBlock.QuoteBlock) {
            textView.text = block.text
            textView.typeface = Typeface.DEFAULT
            textView.textSize = 16f
            
            //用 LayerDrawable 来实现左竖线效果
            //第一层是灰色竖线，第二层是白色背景，这样看起来就像左边有个竖线
            val quoteLineColor = android.graphics.drawable.ColorDrawable("#999999".toColorInt())
            val backgroundColor = android.graphics.drawable.ColorDrawable("#FFFFFF".toColorInt())
            
            val layerDrawable = android.graphics.drawable.LayerDrawable(arrayOf(quoteLineColor, backgroundColor))
            //第一层（竖线）：只显示左边 3dp，其他地方用负数隐藏
            layerDrawable.setLayerInset(0, 0, 0, -10000, 0)
            //第二层（白色背景）：从左边 3dp 开始，右边、上下都正常显示
            layerDrawable.setLayerInset(1, 3, 0, 0, 0)
            
            textView.background = layerDrawable
            //左边留出 12dp 放内容，右边和上下也留点空间
            textView.setPadding(12, 12, 12, 12)
        }
    }

    //图片ViewHolder，负责渲染图片块
    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageView_content)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        private val errorText: TextView = itemView.findViewById(R.id.textView_error)

        fun bind(block: ContentBlock.TextBlock) {
            // 直接使用正则表达式提取图片路径
            val text = block.text.toString()
            val match = Regex("!\\[(.*?)\\]\\((.*?)\\)").find(text)
            if (match != null && match.groupValues.size >= 3) {
                val imagePath = match.groupValues[2]
                val altText = match.groupValues[1]
                
                // 显示加载中状态
                progressBar.visibility = View.VISIBLE
                imageView.visibility = View.GONE
                errorText.visibility = View.GONE
                
                // 设置alt text作为内容描述
                imageView.contentDescription = altText
                
                // 直接加载图片，避免使用lifecycleScope
                try {
                    // 尝试从文件路径加载图片
                    val bitmap = ImageUtils.loadBitmapFromPath(imagePath)
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                        imageView.visibility = View.VISIBLE
                        progressBar.visibility = View.GONE
                        errorText.visibility = View.GONE
                    } else {
                        // 尝试从资源或其他来源加载
                        errorText.text = "图片加载失败: $altText"
                        errorText.visibility = View.VISIBLE
                        progressBar.visibility = View.GONE
                        imageView.visibility = View.GONE
                    }
                } catch (e: Exception) {
                    errorText.text = "加载错误: ${e.message}"
                    errorText.visibility = View.VISIBLE
                    progressBar.visibility = View.GONE
                    imageView.visibility = View.GONE
                }
            }
        }

        private fun showErrorState() {
            progressBar.visibility = View.GONE
            imageView.visibility = View.GONE
            errorText.visibility = View.VISIBLE
            errorText.text = "图片加载失败"
        }
    }
}

