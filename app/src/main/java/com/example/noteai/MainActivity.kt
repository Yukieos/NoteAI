package com.example.noteai

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.noteai.data.NoteDatabase
import com.example.noteai.ui.NoteAdapter
import com.example.noteai.ui.NotesViewModel
import com.example.noteai.ui.NotesViewModelFactory
import com.example.noteai.ui.SidebarNoteAdapter
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt
import androidx.core.view.GravityCompat

class MainActivity : AppCompatActivity() {
    //通过factory把dao给viewmodel
    private val viewModel: NotesViewModel by viewModels {
        NotesViewModelFactory(NoteDatabase.getInstance(applicationContext).noteDao())
    }

    private lateinit var noteAdapter: NoteAdapter
    private lateinit var sidebarAdapter: SidebarNoteAdapter
    private var isSelectionMode = false //默认不在批量选择
    private val selectedNotes = mutableSetOf<Long>()

    //把一些常用控件存成成员变量，避免updateSelectionUI里每次find
    private lateinit var textAppTitle: TextView
    private lateinit var fab: FloatingActionButton
    private lateinit var buttonSearch: ImageButton
    private lateinit var buttonCancelSelection: ImageButton
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        //找出layout所有的控件
        drawerLayout = findViewById(R.id.drawerLayout)
        val buttonMenu = findViewById<ImageButton>(R.id.buttonMenu)
        val recycler = findViewById<RecyclerView>(R.id.recyclerNotes)
        val recyclerSidebar = findViewById<RecyclerView>(R.id.recyclerSidebarNotes)
        val searchInput = findViewById<EditText>(R.id.inputSearch)
        val emptyView = findViewById<TextView>(R.id.textEmpty)
        fab = findViewById(R.id.fabAdd)
        val chipGroup = findViewById<ChipGroup>(R.id.chipGroupTags)
        buttonSearch = findViewById(R.id.buttonSearch)
        buttonCancelSelection = findViewById(R.id.buttonCancelSelection)
        textAppTitle = findViewById(R.id.textAppTitle)

        //这整一个noteAdapter呢，都是帮我们处理逻辑的包括onclick逻辑，点击某个button页面之后会发生什么，还有删掉标签/tag的一系列逻辑，这样传给noteadapter就不用处理逻辑了，adapter可以就专心的帮我们update UI。
        noteAdapter = NoteAdapter(
            onClick = { noteWithTags ->
                if (isSelectionMode) {
                    toggleNoteSelection(noteWithTags.note.id)
                } else {
                    startActivity(
                        Intent(this, NoteDetailActivity::class.java).apply {
                            putExtra(NoteDetailActivity.EXTRA_NOTE_ID, noteWithTags.note.id)
                        }
                    )
                }
            },
            onTagRemove = {noteWithTags, tagName -> viewModel.removeTagFromNote(noteWithTags.note, tagName)},//从笔记里删掉tag
            onLongClick = {noteWithTags ->if (!isSelectionMode) {
                    enterSelectionMode()
                    toggleNoteSelection(noteWithTags.note.id)
                }
            }
        )
        recycler.layoutManager = GridLayoutManager(this, 2)//网格默认两列，大家可以看看还要不要更多
        recycler.adapter = noteAdapter

        sidebarAdapter = SidebarNoteAdapter {noteWithTags -> //这个adpater作用和上面类似
            startActivity(
                Intent(this, NoteDetailActivity::class.java).apply {
                    putExtra(NoteDetailActivity.EXTRA_NOTE_ID, noteWithTags.note.id)
                }
            )
            drawerLayout.closeDrawer(GravityCompat.START) //关闭这个sidebar
        }
        recyclerSidebar.layoutManager = LinearLayoutManager(this)
        recyclerSidebar.adapter = sidebarAdapter

        //我把所有视图切换的函数都删了因为打算grid view为主，listview可以在sidebar看到完整的所有笔记的list。（设计上的问题大家也可以看看补充一下

        //搜索按钮，点击之后搜索框会出现（感觉如果固定这在最上面有点丑还有点占地方
        buttonSearch.setOnClickListener {
            searchInput.isVisible = !searchInput.isVisible
            if (searchInput.isVisible) {
                searchInput.requestFocus()
            } else {
                searchInput.text.clear()
            }
        }

        //退出多选
        buttonCancelSelection.setOnClickListener {
            exitSelectionMode()
        }
        //打开sidebar
        buttonMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        //搜索框有变化就去updatesearchquery
        searchInput.doOnTextChanged { text, _, _, _ ->
            viewModel.updateSearchQuery(text?.toString().orEmpty())
        }

        //监听notes的flow 同时更新主页面和sidebar（sidebar就是一个笔记的listview！
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.notes.collect { notes ->
                    noteAdapter.submitList(notes)
                    sidebarAdapter.submitList(notes)
                    emptyView.isVisible = notes.isEmpty()
                }
            }
        }

        //监听tags的flow，点击chip切换filter，长按删除tag（我之前没有设计好，应该用一个viewmodel来管理这个filter的state，然后UI监听这个stateflow，state一变UI自动刷新）
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allTags.collect { tags ->
                    chipGroup.removeAllViews()
                    tags.forEach { tag ->
                        val chip = Chip(this@MainActivity).apply {
                            text = tag.name
                            isCheckable = true
                            chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                                tag.color.toColorInt()
                            )
                            setOnCheckedChangeListener { _, _ ->
                                viewModel.toggleTagFilter(tag.name)
                            }
                            //长按删除整个tag
                            setOnLongClickListener {
                                android.app.AlertDialog.Builder(this@MainActivity)
                                    .setTitle("删除标签")
                                    .setMessage("确定要删除标签 \"${tag.name}\" 吗？所有笔记中的这个标签都会被移除。")
                                    .setPositiveButton("删除") { _, _ ->
                                        viewModel.deleteTag(tag)
                                    }
                                    .setNegativeButton("取消", null)
                                    .show()
                                true
                            }
                        }
                        chipGroup.addView(chip)
                    }
                }
            }
        }

        //初始化一下UI状态（尤其是fab的行为）
        updateSelectionUI()
    }

    //长按进入多选模式/批量选择模式后，选择一个/取消选择一个笔记。多选模式和批量选择其实是一个东西，批量选择一堆笔记=多选了很多笔记，到时候好一起删掉。
    //其实我有一个想法就是可以设置folder+笔记，比如说多个笔记在一个folder里，或者是在笔记里的笔记。可能有点复杂啊到时候需要新的这个viewmodel啥的了后面有时间再说吧。
    private fun toggleNoteSelection(noteId: Long) {
        if (selectedNotes.contains(noteId)) {
            selectedNotes.remove(noteId)
        } else {
            selectedNotes.add(noteId)
        }
        noteAdapter.setSelectedNotes(selectedNotes)
        updateSelectionUI()
    }
    private fun enterSelectionMode() {
        isSelectionMode = true
        selectedNotes.clear()
        noteAdapter.setSelectionMode(true)
        updateSelectionUI()
    }
    private fun exitSelectionMode() {
        isSelectionMode = false
        selectedNotes.clear()
        noteAdapter.setSelectionMode(false)
        noteAdapter.setSelectedNotes(emptySet())
        updateSelectionUI()
    }
    private fun updateSelectionUI() {
        if (isSelectionMode) {
            textAppTitle.text = "已选择 ${selectedNotes.size} 项"
            fab.setImageResource(android.R.drawable.ic_menu_delete)
            buttonSearch.isVisible = false
            buttonCancelSelection.isVisible = true
            fab.setOnClickListener {
                if (selectedNotes.isNotEmpty()) {
                    android.app.AlertDialog.Builder(this)
                        .setTitle("批量删除")
                        .setMessage("确定要删除选中的 ${selectedNotes.size} 个笔记吗？")
                        .setPositiveButton("删除") { _, _ ->
                            viewModel.deleteNotes(selectedNotes.toList())
                            exitSelectionMode()
                        }
                        .setNegativeButton("取消", null)
                        .show()
                }
            }
        } else {
            textAppTitle.text = "所有笔记"
            fab.setImageResource(android.R.drawable.ic_input_add)
            buttonSearch.isVisible = true
            buttonCancelSelection.isVisible = false
            fab.setOnClickListener {
                startActivity(Intent(this, NoteDetailActivity::class.java))
            }
        }
    }

    //退出批量选择的模式
    override fun onBackPressed() {
        if (isSelectionMode) {
            exitSelectionMode()
        } else {
            super.onBackPressed()
        }
    }
}
