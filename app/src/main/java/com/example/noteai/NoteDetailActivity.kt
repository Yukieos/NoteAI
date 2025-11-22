//笔记的详情页也是编辑页
package com.example.noteai

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.noteai.data.NoteDatabase
import com.example.noteai.data.NoteWithTags
import com.example.noteai.ui.NotesViewModel
import com.example.noteai.ui.NotesViewModelFactory
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

class NoteDetailActivity : AppCompatActivity() {
    private val viewModel: NotesViewModel by viewModels {
        NotesViewModelFactory(NoteDatabase.getInstance(applicationContext).noteDao())
    }
    private val currentTags = mutableListOf<String>()
    private var existingNote: NoteWithTags? = null //如果是编辑旧笔记就放这

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_note_detail)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val titleInput = findViewById<EditText>(R.id.inputTitle)
        val contentInput = findViewById<EditText>(R.id.inputContent)
        val saveButton = findViewById<Button>(R.id.buttonSave)
        val deleteButton = findViewById<ImageButton>(R.id.buttonDelete)
        val backButton = findViewById<ImageButton>(R.id.buttonBack)
        val chipGroup = findViewById<ChipGroup>(R.id.chipGroupTags)
        val addTagButton = findViewById<Button>(R.id.buttonAddTag)

        backButton.setOnClickListener { finish() }

        //从Intent获取note id，如果有id说明是编辑，没有就是新建
        val noteId = intent.getLongExtra(EXTRA_NOTE_ID, 0L).takeIf { it != 0L }

        if (noteId != null) {
            //load已经有的笔记
            lifecycleScope.launch {
                existingNote = viewModel.getNote(noteId)
                existingNote?.let { note ->
                    titleInput.setText(note.note.title)
                    contentInput.setText(note.note.content)
                    //把已经有的tags放currenttags再渲染
                    currentTags.clear()
                    currentTags.addAll(note.tags.map { it.name })
                    renderTagChips(chipGroup)
                    deleteButton.isVisible = true
                } ?: run {
                    //找不到笔记告诉用户一声就关了
                    Toast.makeText(this@NoteDetailActivity, R.string.note_not_found, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        } else {
            deleteButton.isVisible = false
        }

        addTagButton.setOnClickListener {
            showAddTagDialog(chipGroup)
        }

        saveButton.setOnClickListener {
            val title = titleInput.text.toString()
            val content = contentInput.text.toString()
            viewModel.saveNote(
                id = existingNote?.note?.id,
                title = title,
                content = content,
                tagsInput = currentTags.toList(),
                createdAt = existingNote?.note?.createdAt
            )
            Toast.makeText(this, R.string.save, Toast.LENGTH_SHORT).show()
            finish()
        }
        //删除笔记暂时是只有在编辑的情况下才显示这个按钮，后面可能会要做一个往左滑动删除的功能
        deleteButton.setOnClickListener {
            existingNote?.let { note ->
                viewModel.deleteNote(note.note)
                Toast.makeText(this, R.string.delete, Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }
    //输入新的tag
    private fun showAddTagDialog(chipGroup: ChipGroup) {
        val inputField = EditText(this).apply {
            hint = getString(R.string.note_tags_hint)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.add_tag)
            .setView(inputField)
            .setPositiveButton(R.string.add_tag_confirm) { _, _ ->
                val newTag = inputField.text.toString().trim()
                if (newTag.isEmpty()) {
                    Toast.makeText(this, R.string.note_tags_hint, Toast.LENGTH_SHORT).show()
                } else if (currentTags.contains(newTag)) {
                    Toast.makeText(this, R.string.tag_exists, Toast.LENGTH_SHORT).show()
                } else {
                    currentTags.add(newTag)
                    renderTagChips(chipGroup)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
    //重新渲染当前的tag
    private fun renderTagChips(chipGroup: ChipGroup) {
        chipGroup.removeAllViews()
        currentTags.forEach { tag ->
            val chip = Chip(this).apply {
                text = tag
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    currentTags.remove(tag)
                    renderTagChips(chipGroup)
                }//删除tag之后刷新chips
            }
            chipGroup.addView(chip)
        }
    }

    companion object {
        const val EXTRA_NOTE_ID = "extra_note_id"
    }
}
