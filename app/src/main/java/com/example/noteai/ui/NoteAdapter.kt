//把一堆note的数据adapt成我们屏幕上一行行的item，一个列表的渲染器
//这个adapter感觉就是一个中间人告诉recyclerview每一行长啥样（通过item_note.xml),给我们每条note填进去，然后listen onclick
package com.example.noteai.ui

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.noteai.R
import com.example.noteai.data.NoteWithTags
import java.util.Date

//adapter不用自己管跳转页面逻辑直接从外部传onclick就行了
class NoteAdapter(private val onClick: (NoteWithTags) -> Unit) : 
    RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {
    private var notesList: List<NoteWithTags> = emptyList() //recyclerview数据源先默认空列表

    //当recyclerview要新的一行view用这个方法，然后我们把item_note.xml inflate成一个真正的view，然后包装成viewholder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view, onClick)
    }

    //recycler需要把第position条数据显示出来，然后我们调取datasource
    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notesList[position]
        holder.bind(note)
    }

    override fun getItemCount() = notesList.size
    
    //mainactivity一旦有新的列表我们用这个更新数据，然后让recyclerview重新渲染（我这里就直接暴力的刷新全部了，后期如果笔记很多我们可以再用新方法，感觉我们目前不会有几百条笔记吧。。
    fun submitList(newNotes: List<NoteWithTags>) {
        notesList = newNotes
        notifyDataSetChanged() 
    }
    //noteviewholder就是recyclerview的一行视图+绑定数据的逻辑
    class NoteViewHolder(itemView: View, val onClick: (NoteWithTags) -> Unit) :
        RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.textTitle)
        private val content: TextView = itemView.findViewById(R.id.textContentPreview)
        private val tags: TextView = itemView.findViewById(R.id.textTags)
        private val updatedAt: TextView = itemView.findViewById(R.id.textUpdatedAt)

        fun bind(item: NoteWithTags) {
            //我这里用了ifBlank是因为如果标题很空就要显示默认文案，空UI真感觉太丑了（
            title.text = item.note.title.ifBlank { itemView.context.getString(R.string.note_title) }
            content.text = item.note.content.ifBlank { itemView.context.getString(R.string.note_content) }
            
            //如果tags不为空，就显示tags，否则就隐藏
            if (item.tags.isNotEmpty()) {
                tags.text = item.tags.joinToString(separator = " · ") { it.name }
                tags.visibility = View.VISIBLE
            } else {
                tags.visibility = View.GONE
            }

            //现在我们是可以通过这个updatedAt来显示笔记更新时间，我后期可能想要通过这个方法来显示哪些笔记recently被改变过
            val formattedDate = DateFormat.getDateFormat(itemView.context).format(Date(item.note.updatedAt))
            updatedAt.text = itemView.context.getString(R.string.last_updated_format, formattedDate)

            itemView.setOnClickListener { onClick(item) }
        }
    }
}