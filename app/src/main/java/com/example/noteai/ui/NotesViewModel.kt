//这个file类似于ObservableObject（in SwiftUI），这个viewmodel负责管理我们UI需要的状态然后操作这些状态逻辑。我们UI监听stateflow，数据一变UI自动刷新
package com.example.noteai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.noteai.data.Note
import com.example.noteai.data.NoteDao
import com.example.noteai.data.NoteWithTags
import com.example.noteai.data.Tag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotesViewModel(private val noteDao: NoteDao) : ViewModel() {
    //这里需要注意就是 _notes是内部可写的stateflow，notes是对外只读的stateflow(搞两套防止UI乱改数据)
    private val _notes = MutableStateFlow<List<NoteWithTags>>(emptyList())
    val notes: StateFlow<List<NoteWithTags>> = _notes.asStateFlow()

    //所有tag的列表，后面会显示在mainactvity里的chip里
    private val _allTags = MutableStateFlow<List<Tag>>(emptyList())
    val allTags: StateFlow<List<Tag>> = _allTags.asStateFlow()

    //当前选中的标签filter
    private val selectedTags = mutableSetOf<String>()
    private var allNotesList = emptyList<NoteWithTags>() //保存所有笔记，后面好去filter

    init {
        loadAllNotes()
        loadAllTags()
    }
    //listen db所有的notes，db一变化就collect到新列表
    private fun loadAllNotes() {
        viewModelScope.launch {
            noteDao.getAllNotesWithTags().collect { notesList ->
                allNotesList = notesList
                applyFilter()
            }
        }
    }
    //上面听的是带tag的notes，这里是tag
    private fun loadAllTags() {
        viewModelScope.launch {
            noteDao.getAllTags().collect { tags ->
                _allTags.value = tags
            }
        }
    }

    //如果user点击某个chip的时候就call这个函数，选中了加入一个filter，没选中就remove一个filter
    fun toggleTagFilter(tagName: String) {
        if (selectedTags.contains(tagName)) {
            selectedTags.remove(tagName)
        } else {
            selectedTags.add(tagName)
        }
        applyFilter()
    }

    //这里根据选择的tag过滤allnoteslist来更新我们内部的_notes
    private fun applyFilter() {
        _notes.value = if (selectedTags.isEmpty()) {
            allNotesList
        } else {
            allNotesList.filter { noteWithTags ->
                noteWithTags.tags.any { tag -> selectedTags.contains(tag.name) }
            }
        }
    }
    //如果user在我们搜索文字框里写任何东西，用这个函数来update我们给db的query（功能如函数其名）
    fun updateSearchQuery(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                //如果搜索框是空的就return all notes
                noteDao.getAllNotesWithTags().collect { notesList ->
                    _notes.value = notesList
                }
            } else {
                //不是空的去db搜索
                noteDao.searchNotesWithTags(query).collect { notesList ->
                    _notes.value = notesList
                }
            }
        }
    }
    //id如果是空的说明这个笔记是一个新的，否则这个笔记已经存在了属于是更新
    fun saveNote(
        id: Long?,
        title: String,
        content: String,
        tagsInput: List<String>,
        createdAt: Long? = null
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            //对用户输入的tag名称做去重
            val trimmedTags = tagsInput.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            val tagEntities = trimmedTags.map { Tag(name = it) }
            
            //判断是update还是insert
            val note = if (id == null) {
                Note(title = title, content = content, createdAt = now, updatedAt = now)
            } else {
                Note(
                    id = id,
                    title = title,
                    content = content,
                    createdAt = createdAt ?: now,
                    updatedAt = now
                )
            }

            noteDao.upsertNoteWithTags(note, tagEntities)
        }
    }
    //如果delete了这个笔记那关联表也要一起删掉
    fun deleteNote(note: Note) {
        viewModelScope.launch {
            noteDao.deleteNoteWithTags(note)
        }
    }
    
    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            noteDao.deleteTag(tag)
        }
    }
    
    suspend fun getNote(noteId: Long): NoteWithTags? = noteDao.getNoteWithTags(noteId)
}

//notesviewmodel需要notedao参数，所以我们不能用默认构造所以创造了factory，用factory来创建viewmodel

class NotesViewModelFactory(private val noteDao: NoteDao) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotesViewModel::class.java)) {
            return NotesViewModel(noteDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
