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
    //val selectedTags = mutableSetOf<String>()
    private val _selectedTags = MutableStateFlow<Set<String>>(emptySet())
    val selectedTags: StateFlow<Set<String>> = _selectedTags.asStateFlow()
    fun toggleTagSelection(tag: String, isChecked: Boolean) {
        val current = _selectedTags.value.toMutableSet()

        if (isChecked) {
            current.add(tag)
        } else {
            current.remove(tag)
        }
        _selectedTags.value = current

        // 套用原有的过滤逻辑
        applyFilter()
    }

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
        // 判断当前是否已选中
        val isSelectedNow = _selectedTags.value.contains(tagName)
        // 反转勾选状态，交给新方法处理
        toggleTagSelection(tagName, !isSelectedNow)
    }


    //这里根据选择的tag过滤allnoteslist来更新我们内部的_notes
    private fun applyFilter() {
        // 先把 StateFlow 里的集合取出来
        val selected = _selectedTags.value   // 或者 selectedTags.value 也可以

        _notes.value = if (selected.isEmpty()) {
            allNotesList
        } else {
            allNotesList.filter { noteWithTags ->
                noteWithTags.tags.any { tag ->
                    selected.contains(tag.name)
                }
            }
        }
    }

    //如果user在我们搜索文字框里写任何东西，用这个函数来update我们给db的query（功能如函数其名）
    fun updateSearchQuery(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                //如果搜索框是空的就return all notes
                noteDao.getAllNotesWithTags().collect { notesList ->
                    allNotesList = notesList
                    applyFilter() //应用tag过滤
                }
            } else {
                //不是空的去db搜索
                noteDao.searchNotesWithTags(query).collect { notesList ->
                    allNotesList = notesList
                    applyFilter() //应用tag过滤
                }
            }
        }
    }
    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            noteDao.deleteTag(tag)
        }
    }
    //id如果是空的说明这个笔记是一个新的，否则这个笔记已经存在了属于是更新
    fun saveNote(
        id: Long?,
        title: String,
        content: String,
        tagsInput: List<String>,
        tagColors: Map<String, String>? = null, //因为tag有颜色嘛，所以我们保存笔记的时候同时也要保存一下不同标签的颜色
        createdAt: Long? = null
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            //对用户输入的tag名称做去重
            val trimmedTags = tagsInput.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            val tagEntities = trimmedTags.map { tagName -> val color = tagColors?.get(tagName) ?: "#E9E8E6" //默认颜色灰色，不能不选颜色吧
                Tag(name = tagName, color = color)
            }
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

    //之前的那个delete函数很基础，就删掉当前那个笔记还有相关表格，这个就是可以批量的删除选中的笔记，多了一个for loop去找到就是多个要删除的笔记
    fun deleteNotes(noteIds: List<Long>) {
        viewModelScope.launch {
            noteIds.forEach { noteId ->
                val note = noteDao.getNoteWithTags(noteId)
                note?.let {
                    noteDao.deleteNoteWithTags(it.note)
                }
            }
        }
    }
    
    /**fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            //删除tag之前，如果这个tag在filter的列表里我们就移除
            selectedTags.remove(tag.name)
            noteDao.deleteTag(tag)
            applyFilter() //重新应用filter
        }
    }*/

    //从某个笔记删除某个tag
    fun removeTagFromNote(note: Note, tagName: String) {
        viewModelScope.launch {
            //获取当前笔记的所有tag
            val noteWithTags = noteDao.getNoteWithTags(note.id)
            noteWithTags?.let {
                //过滤掉要删除的tag
                val remainingTags = it.tags.filter { tag -> tag.name != tagName }
                //重新保存笔记和tag
                noteDao.upsertNoteWithTags(note, remainingTags)
            }
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
