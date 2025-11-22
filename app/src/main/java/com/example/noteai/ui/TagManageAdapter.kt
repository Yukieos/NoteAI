//这个adapter给managetags activity的，每行展示一个tag然后要有删除按钮
//这个构造和noteadapter类似，我noteadapter注释写的比较详细可以看那个
package com.example.noteai.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.noteai.R
import com.example.noteai.data.Tag

class TagManageAdapter(
    private val onDeleteClick: (Tag) -> Unit
) : RecyclerView.Adapter<TagManageAdapter.TagViewHolder>() {
    private var tagsList: List<Tag> = emptyList() //和noteadapter类似，先默认空列表
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tag_manage, parent, false)
        return TagViewHolder(view, onDeleteClick)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        holder.bind(tagsList[position])
    }

    override fun getItemCount() = tagsList.size

    fun submitList(newTags: List<Tag>) {
        tagsList = newTags
        notifyDataSetChanged()
    }

    class TagViewHolder(
        itemView: View,
        private val onDeleteClick: (Tag) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val colorIndicator: View = itemView.findViewById(R.id.viewColorIndicator)
        private val tagName: TextView = itemView.findViewById(R.id.textTagName)
        private val noteCount: TextView = itemView.findViewById(R.id.textNoteCount)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.buttonDelete)

        fun bind(tag: Tag) {
            tagName.text = tag.name
            noteCount.text = "0" //先hardcode 0，后面如果统计某个tag下面多少note再改dao查询
            
            //把tagcolor填进去，item_tag_manage.xml其实是一个圆角背景，我感觉圆角bubble设计很好看，但是没颜色所以需要填颜色

            val drawable = colorIndicator.background as? GradientDrawable
            drawable?.setColor(Color.parseColor(tag.color))

            deleteButton.setOnClickListener {
                onDeleteClick(tag)
            }
        }
    }
}

