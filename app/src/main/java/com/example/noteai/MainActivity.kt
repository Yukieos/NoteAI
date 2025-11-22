package com.example.noteai

import android.content.Intent
import android.graphics.Color
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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.noteai.data.NoteDatabase
import com.example.noteai.ui.NoteAdapter
import com.example.noteai.ui.NotesViewModel
import com.example.noteai.ui.NotesViewModelFactory
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt

class MainActivity : AppCompatActivity() {
    //通过factory把dao给viewmodel
    private val viewModel: NotesViewModel by viewModels {
        NotesViewModelFactory(NoteDatabase.getInstance(applicationContext).noteDao())
    }

    private lateinit var noteAdapter: NoteAdapter
    private var isGridView = false //这个是用来决定我们用grid view还是list view的（增加一点试图多样性，感觉list有点像老款备忘录）

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
        val recycler = findViewById<RecyclerView>(R.id.recyclerNotes)
        val searchInput = findViewById<EditText>(R.id.inputSearch)
        val emptyView = findViewById<TextView>(R.id.textEmpty)
        val fab = findViewById<FloatingActionButton>(R.id.fabAdd)
        val chipGroup = findViewById<ChipGroup>(R.id.chipGroupTags)
        val buttonViewToggle = findViewById<ImageButton>(R.id.buttonViewToggle)
        val buttonManageTags = findViewById<ImageButton>(R.id.buttonManageTags)

        //初始化recyclerview，然后处理点击逻辑，这样可以直接传给adapter
        noteAdapter = NoteAdapter { noteWithTags ->
            startActivity(
                Intent(this, NoteDetailActivity::class.java).apply {
                    putExtra(NoteDetailActivity.EXTRA_NOTE_ID, noteWithTags.note.id)
                }
            )
        }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = noteAdapter

        //新建笔记的一个按键，onclick之后可以跳转详情页
        fab.setOnClickListener {
            startActivity(Intent(this, NoteDetailActivity::class.java))
        }

        //这个onclick是一个视图切换，如果是grid view就变成2列，如果是list view就变成1列，然后我们刷新adapter让它使用新的布局
        buttonViewToggle.setOnClickListener {
            isGridView = !isGridView
            if (isGridView) {
                recycler.layoutManager = GridLayoutManager(this, 2)
            } else {
                recycler.layoutManager = LinearLayoutManager(this)
            }
            recycler.adapter = noteAdapter
        }

        //tag的管理按键
        buttonManageTags.setOnClickListener {
            startActivity(Intent(this, ManageTagsActivity::class.java))
        }

        //搜索框有变化就去updatesearchquery
        searchInput.doOnTextChanged { text, _, _, _ ->
            viewModel.updateSearchQuery(text?.toString().orEmpty())
        }

        //监听notes的flow
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.notes.collect { notes ->
                    noteAdapter.submitList(notes)
                    emptyView.isVisible = notes.isEmpty()
                }
            }
        }

        //监听tags flow，点击chip就是切换过滤
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allTags.collect { tags ->
                    chipGroup.removeAllViews()
                    tags.forEach { tag ->
                        val chip = Chip(this@MainActivity).apply {
                            text = tag.name
                            isCheckable = true
                            //设置标签的颜色
                            chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                                tag.color.toColorInt()
                            )
                            setOnCheckedChangeListener { _, _ ->
                                viewModel.toggleTagFilter(tag.name)
                            }

                        }
                        chipGroup.addView(chip)
                    }
                }
            }
        }
    }
}
