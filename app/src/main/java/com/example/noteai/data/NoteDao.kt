//这个是我们的NoteDao，主要提供了我们接下来可能会用到的所有method
//（getAllNotesWithTags/searchNotesWithTags/insertNote/deleteNote/insertTags/getNoteWithTags/

package com.example.noteai.data
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    //这个transaction是为了让我们在查询的时候note和tags一起查就避免了数据不一致这种情况
    //用这个flow主要是为了自动listen db的变化，一有这个数据的更新我们的UI需要自动地刷新
    @Transaction
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotesWithTags(): Flow<List<NoteWithTags>>

    @Transaction
    @Query(
        "SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY updatedAt DESC"
    )
    fun searchNotesWithTags(query: String): Flow<List<NoteWithTags>>

    //用java要重新开线程嗯，就用suspend协程了一下
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    //用ignore来避免tag的名字重复
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTags(tags: List<Tag>): List<Long>

    @Query("SELECT * FROM tags WHERE name IN (:names)")
    suspend fun getTagsByNames(names: List<String>): List<Tag>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :noteId LIMIT 1")
    suspend fun getNoteWithTags(noteId: Long): NoteWithTags?

    //嗯这个其实可选吧，写这个是为了保证note id和tag id每对的唯一性，重复插入同样一对noteid和tagid可以新的给旧的overwrite了
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCross(cross: List<NoteTagCross>)

    @Query("DELETE FROM note_tag_cross WHERE noteId = :noteId")
    suspend fun deleteCrossForNote(noteId: Long)

    //我写upsert可能有点奇怪，其实意思是update+insert。如果note的id是0说明是新建，否则意思是更新已经存在的
    //处理了note和tag的关系就啥时候update/啥时候insert
    @Transaction
    suspend fun upsertNoteWithTags(note: Note, tags: List<Tag>): Long {
        val persistedNoteId = if (note.id == 0L) {
            insertNote(note)
        } else {
            updateNote(note)
            note.id
        }
        //如果有update的话，先删除以前的notetag关系，然后重新建立新的关系
        deleteCrossForNote(persistedNoteId)
        if (tags.isEmpty()) return persistedNoteId
        insertTags(tags)
        //根据tag的名字查出tag所对应的id，然后创建cross table的记录
        val savedTags = getTagsByNames(tags.map { it.name })
        val cross = savedTags.map { tag -> NoteTagCross(noteId = persistedNoteId, tagId = tag.id) }
        insertCross(cross)
        return persistedNoteId
    }

    //删除笔记的时候也要把相关的tag关系删掉
    @Transaction
    suspend fun deleteNoteWithTags(note: Note) {
        deleteCrossForNote(note.id)
        deleteNote(note)
    }
}
