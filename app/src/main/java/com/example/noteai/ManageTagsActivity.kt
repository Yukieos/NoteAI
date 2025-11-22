package com.example.noteai

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.noteai.data.NoteDatabase
import com.example.noteai.ui.NotesViewModel
import com.example.noteai.ui.NotesViewModelFactory
import com.example.noteai.ui.TagManageAdapter
import kotlinx.coroutines.launch

//标签管理页面展示全部tags然后可以删除tag，然后增加了一个“确认删除？”的警告
class ManageTagsActivity : AppCompatActivity() {

    private val viewModel: NotesViewModel by viewModels {
        NotesViewModelFactory(NoteDatabase.getInstance(applicationContext).noteDao())
    }
    private lateinit var tagAdapter: TagManageAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_manage_tags)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val recyclerTags = findViewById<RecyclerView>(R.id.recyclerTags)
        val buttonClose = findViewById<ImageButton>(R.id.buttonClose)
        val emptyView = findViewById<TextView>(R.id.textEmpty)

        buttonClose.setOnClickListener {
            finish()
        }

        //初始化adapter，然后ondeleteclick给activity这样可以弹出alert
        tagAdapter = TagManageAdapter { tag ->
            AlertDialog.Builder(this)
                .setTitle("删除标签")
                .setMessage("确定要删除标签 \"${tag.name}\" 吗？")
                .setPositiveButton("删除") { _, _ ->
                    viewModel.deleteTag(tag)
                }
                .setNegativeButton("取消", null)
                .show()
        }

        recyclerTags.layoutManager = LinearLayoutManager(this)
        recyclerTags.adapter = tagAdapter

        //监听tag的列表，有变化刷新UI
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allTags.collect { tags ->
                    tagAdapter.submitList(tags)
                    emptyView.isVisible = tags.isEmpty()
                }
            }
        }
    }
}

