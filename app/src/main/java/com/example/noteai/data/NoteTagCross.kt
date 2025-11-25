//note和tag的cross table中间表
package com.example.noteai.data
import androidx.room.Entity
import androidx.room.Index

//用noteId和tagId作为联合primary key来确保一对关系不重复
//给tagId加索引是为了后期我们通过tag找note的时候快一点
@Entity(
    tableName = "note_tag_cross",
    primaryKeys = ["noteId", "tagId"],
    indices = [Index("tagId")]
)
data class NoteTagCross(
    val noteId: Long,
    val tagId: Long
)
