package com.example.noteai.data
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
//这个只是个查询表，为了不写太多JOIN就直接用embedded和relation给查询了
data class NoteWithTags(
    @Embedded val note: Note,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = NoteTagCross::class,
            parentColumn = "noteId",
            entityColumn = "tagId"
        )
    )
    val tags: List<Tag>
)
