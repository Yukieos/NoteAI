//Sidebar笔记列表的adapter
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

class SidebarNoteAdapter(
    private val onClick: (NoteWithTags) -> Unit
) : RecyclerView.Adapter<SidebarNoteAdapter.NoteViewHolder>() {

    private var notesList: List<NoteWithTags> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sidebar_note, parent, false)
        return NoteViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(notesList[position])
    }

    override fun getItemCount() = notesList.size

    fun submitList(newNotes: List<NoteWithTags>) {
        notesList = newNotes
        notifyDataSetChanged()
    }

    class NoteViewHolder(
        itemView: View,
        private val onClick: (NoteWithTags) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val title: TextView = itemView.findViewById(R.id.textTitle)
        private val updatedAt: TextView = itemView.findViewById(R.id.textUpdatedAt)

        fun bind(item: NoteWithTags) {
            title.text = item.note.title.ifBlank { "无标题" }
            
            val formattedDate = DateFormat.getDateFormat(itemView.context)
                .format(Date(item.note.updatedAt))
            updatedAt.text = formattedDate

            itemView.setOnClickListener {
                onClick(item)
            }
        }
    }
}

