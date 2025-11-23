//把一堆note的数据adapt成我们屏幕上一行行的item，一个列表的渲染器
//这个adapter感觉就是一个中间人告诉recyclerview每一行长啥样（通过item_note.xml),给我们每条note填进去，然后listen onclick
package com.example.noteai.ui

import android.graphics.Color
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.example.noteai.R
import com.example.noteai.data.NoteWithTags
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.util.Date

//adapter不用自己管跳转页面逻辑直接从外部传onclick就行了
class NoteAdapter(
    private val onClick: (NoteWithTags) -> Unit,
    private val onTagRemove: (NoteWithTags, String) -> Unit, //从某个note删除掉tag
    private val onLongClick: (NoteWithTags) -> Unit //我后面设置了一些功能比如说批量管理文件还有删除tag的快捷都需要这个长按来进入一个模式/状态能被识别，所以有了这个longclick
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {
    private var notesList: List<NoteWithTags> = emptyList()
    private var isSelectionMode = false //我们需要确定当前的一个状态是多选还是正常点进文件就可以看到这个具体内容
    private var selectedNotes = setOf<Long>() //选中的note id

    //当recyclerview要新的一行view用这个方法，必须使用网格布局。我一开始其实有list布局的，后面实在觉得list布局好丑，我可以参考市面上这些app笔记就是用一个sidebar展现全部note也算是一种list view了吧
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.note_item_grid, parent, false)
        return NoteViewHolder(view, onClick, onTagRemove, onLongClick)
    }

    //recycler需要把第position条数据显示出来，然后我们调取datasource
    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notesList[position]
        val isSelected = selectedNotes.contains(note.note.id)
        holder.bind(note, isSelectionMode, isSelected)
    }

    override fun getItemCount() = notesList.size

    //下面三个函数都是更新的函数。第一个就多选模式开关，第二个就更新一下多选状态下被选中的笔记id，第三个就是更新全部的note列表
    fun setSelectionMode(enabled: Boolean) {
        isSelectionMode = enabled
        notifyDataSetChanged()
    }
    fun setSelectedNotes(notes: Set<Long>) {
        selectedNotes = notes
        notifyDataSetChanged()
    }
    //mainactivity一旦有新的列表我们用这个更新数据，然后让recyclerview重新渲染（我这里就直接暴力的刷新全部了，后期如果笔记很多我们可以再用新方法，感觉我们目前不会有几百条笔记吧。。
    fun submitList(newNotes: List<NoteWithTags>) {
        notesList = newNotes
        notifyDataSetChanged() 
    }
    //noteviewholder就是recyclerview的一行视图+绑定数据的逻辑
    class NoteViewHolder(
        itemView: View,
        private val onClick: (NoteWithTags) -> Unit, //新变量设置原因我上面写了
        private val onTagRemove: (NoteWithTags, String) -> Unit,
        private val onLongClick: (NoteWithTags) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.textTitle)
        private val content: TextView = itemView.findViewById(R.id.textContentPreview)
        private val updatedAt: TextView = itemView.findViewById(R.id.textUpdatedAt)
        private val chipGroup: ChipGroup = itemView.findViewById(R.id.chipGroupTags)

        fun bind(item: NoteWithTags, isSelectionMode: Boolean, isSelected: Boolean) {
            //我这里用了ifBlank是因为如果标题很空就要显示默认文案，空UI真感觉太丑了（
            title.text = item.note.title.ifBlank { "无标题" }
            content.text = item.note.content.ifBlank { "无内容" }

            //现在我们是可以通过这个updatedAt来显示笔记更新时间，我后期可能想要通过这个方法来显示哪些笔记recently被改变过（我们后面可以讨论笔记排序是按照时间还是标题）
            val formattedDate = DateFormat.getDateFormat(itemView.context).format(Date(item.note.updatedAt))
            updatedAt.text = formattedDate

            //用chips显示tags，每个标签后面都有叉可以删除，这样就实现了我们那个文档stage1要求的标签管理删除
            chipGroup.removeAllViews()
            item.tags.forEach { tag ->
                val chip = Chip(itemView.context).apply {
                    text = tag.name
                    isCloseIconVisible = true
                    chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                        try {
                            tag.color.toColorInt()
                        } catch (e: Exception) {
                            Color.parseColor("#E9E8E6")
                        }
                    )
                    setOnCloseIconClickListener {
                        onTagRemove(item, tag.name)
                    }
                }
                chipGroup.addView(chip)
            }

            //多选选中的效果就是有一种阴影之感
            if (isSelectionMode) {
                itemView.alpha = if (isSelected) 0.7f else 1.0f
            } else {
                itemView.alpha = 1.0f
            }

            itemView.setOnClickListener { onClick(item) }
            itemView.setOnLongClickListener {
                onLongClick(item)
                true
            }
        }
    }
}