//笔记的详情页也是编辑页
package com.example.noteai

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.noteai.data.NoteDatabase
import com.example.noteai.data.NoteWithTags
import com.example.noteai.markdown.MarkdownBlockParser
import com.example.noteai.markdown.MarkdownParser
import com.example.noteai.ui.NotesViewModel
import com.example.noteai.ui.NotesViewModelFactory
import com.example.noteai.ui.MarkdownContentAdapter
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

class NoteDetailActivity : AppCompatActivity() {
    private val viewModel: NotesViewModel by viewModels {
        NotesViewModelFactory(NoteDatabase.getInstance(applicationContext).noteDao())
    }
    private val currentTags = mutableListOf<String>()
    private val tagColors = mutableMapOf<String, String>()
    private var existingNote: NoteWithTags? = null //如果是编辑旧笔记就放这

    private val defaultColor = "#E9E8E6" //我给一个默认颜色，后面parse失败就回退到它
    private val markdownParser = MarkdownParser() //用来把 markdown 转成富文本
    private val markdownBlockParser = MarkdownBlockParser() //用来把 markdown 分解成块，高效渲染
    private var isPreviewMode = false //记录当前是编辑还是预览模式
    private var contentAdapter: MarkdownContentAdapter? = null //预览模式用的适配器

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.note_detail)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val titleInput = findViewById<EditText>(R.id.inputTitle)
        val contentInput = findViewById<EditText>(R.id.inputContent)
        val previewContent = findViewById<TextView>(R.id.previewContent)
        val saveButton = findViewById<Button>(R.id.buttonSave)
        val deleteButton = findViewById<ImageButton>(R.id.buttonDelete)
        val backButton = findViewById<ImageButton>(R.id.buttonBack)
        val togglePreviewButton = findViewById<Button>(R.id.buttonTogglePreview)
        val chipGroup = findViewById<ChipGroup>(R.id.chipGroupTags)
        val addTagButton = findViewById<Button>(R.id.buttonAddTag)
        
        // 高效预览：用 RecyclerView 替代 TextView，支持虚拟滚动
        val recyclerPreview = findViewById<RecyclerView>(R.id.recyclerContentPreview)
        if (recyclerPreview != null) {
            contentAdapter = MarkdownContentAdapter(emptyList())
            recyclerPreview.adapter = contentAdapter
        }

        backButton.setOnClickListener { finish() }
        
        //眼睛按钮：点击切换编辑/预览模式
        togglePreviewButton.setOnClickListener {
            isPreviewMode = !isPreviewMode
            updatePreviewMode(contentInput, previewContent, recyclerPreview, togglePreviewButton)
        }
        
        //编辑内容时实时更新预览
        contentInput.doOnTextChanged { text, _, _, _ ->
            if (isPreviewMode && recyclerPreview != null) {
                //用高效的块解析器，而不是一次性渲染整个文档
                val blocks = markdownBlockParser.parseToBlocks(text?.toString().orEmpty())
                contentAdapter = MarkdownContentAdapter(blocks)
                recyclerPreview.adapter = contentAdapter
            } else if (isPreviewMode) {
                previewContent.text = markdownParser.parse(text?.toString().orEmpty())
            }
        }

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
                    note.tags.forEach { tag ->
                        tagColors[tag.name] = tag.color
                    } //存一下已经有的tag，这样用户可以快速选择tag不用重复写了
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
                tagColors = tagColors,
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
    //显示添加标签对话框
    private fun showAddTagDialog(chipGroup: ChipGroup) {
        val dialogView = layoutInflater.inflate(R.layout.add_tag, null)
        val inputTagName = dialogView.findViewById<EditText>(R.id.inputTagName)
        val chipGroupExisting = dialogView.findViewById<ChipGroup>(R.id.chipGroupExistingTags)

        var selectedColor = defaultColor
        //currenttags是我们这个笔记会在详情页中显示的tags，然后selectedtags是我们点开这个tags窗口的时候用户创立/选的tags。就selected是临时选择，current是note的属性
        val selectedTags = mutableSetOf<String>().apply { addAll(currentTags) }

        //把数据库里存在的所有标签渲染到弹窗里，作为可选择的tags，这样用户就可以看到他们之前创造过的所有tags了（快捷设置！）
        chipGroupExisting.removeAllViews()
        viewModel.allTags.value.forEach { tag ->
            val chip = Chip(this).apply {
                text = tag.name
                isCheckable = true
                isChecked = currentTags.contains(tag.name)
                chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    safeColor(tag.color)
                )
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedTags.add(tag.name)
                        tagColors[tag.name] = tag.color
                    } else {
                        selectedTags.remove(tag.name)
                        tagColors.remove(tag.name)
                    }
                }
            }
            chipGroupExisting.addView(chip)
        }

        val colorMap = mapOf(
            R.id.colorGray to "#F4F4F5",
            R.id.colorBlue to "#E8F0FB",
            R.id.colorGreen to "#E8F7E4",
            R.id.colorYellow to "#FFF7DB",
            R.id.colorOrange to "#FFE8D9",
            R.id.colorRed to "#FFECEC",
            R.id.colorPink to "#FBE7F2",
            R.id.colorPurple to "#F1E9FF"
        )

        colorMap.forEach { (viewId, colorHex) ->
            dialogView.findViewById<View>(viewId)?.setOnClickListener {
                selectedColor = colorHex
                colorMap.keys.forEach { id ->
                    dialogView.findViewById<View>(id)?.alpha =
                        if (id == viewId) 1.0f else 0.5f
                }
            }
        }

        AlertDialog.Builder(this) //dialogView前面已经建立好了，然后我们建立一个弹窗，然后把dialogView内容真正的塞进去
            .setView(dialogView)
            .create()
            .apply {
                show()
                dialogView.findViewById<Button>(R.id.buttonCancel)?.setOnClickListener {
                    dismiss()
                }
                dialogView.findViewById<Button>(R.id.buttonAdd)?.setOnClickListener {
                    val newTagName = inputTagName.text.toString().trim()
                    if (newTagName.isNotEmpty()) {
                        if (!selectedTags.contains(newTagName)) {
                            selectedTags.add(newTagName)
                            tagColors[newTagName] = selectedColor
                        }
                    }
                    //更新currentTags为selectedtags
                    currentTags.clear()
                    currentTags.addAll(selectedTags)
                    renderTagChips(chipGroup)
                    dismiss()
                }
            }
    }

    //重新渲染当前的tag
    private fun renderTagChips(chipGroup: ChipGroup) {
        chipGroup.removeAllViews()
        currentTags.forEach { tagName ->
            val chip = Chip(this).apply {
                text = tagName
                isCloseIconVisible = true
                val color = tagColors[tagName] ?: defaultColor
                chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    safeColor(color)
                )
                setOnCloseIconClickListener {
                    currentTags.remove(tagName)
                    tagColors.remove(tagName)
                    renderTagChips(chipGroup)
                }
            }
            chipGroup.addView(chip)
        }
    }

    //一个保护的helper method，把颜色字符串转成 int，如果转换过程中出现问题就退回我们的默认灰色。
    private fun safeColor(hex: String): Int {
        return try {
            hex.toColorInt()
        } catch (e: Exception) {
            defaultColor.toColorInt()
        }
    }
    
    //切换编辑和预览模式。预览模式下显示 markdown 渲染的富文本，编辑模式下编辑源代码
    private fun updatePreviewMode(
        contentInput: EditText,
        previewContent: TextView,
        recyclerPreview: RecyclerView?,
        togglePreviewButton: Button
    ) {
        if (isPreviewMode) {
            //进入预览模式：隐藏编辑框，显示预览，按钮显示"编辑"
            contentInput.isVisible = false
            previewContent.isVisible = false
            togglePreviewButton.text = "编辑"

            //高效渲染：用 RecyclerView 显示
            if (recyclerPreview != null) {
                recyclerPreview.isVisible = true
                val blocks = markdownBlockParser.parseToBlocks(contentInput.text.toString())
                contentAdapter = MarkdownContentAdapter(blocks)
                recyclerPreview.adapter = contentAdapter
            } else {
                // 备用方案：用 TextView
                previewContent.isVisible = true
                previewContent.text = markdownParser.parse(contentInput.text.toString())
            }
        } else {
            //回到编辑模式：显示编辑框，隐藏预览，按钮显示"预览"
            contentInput.isVisible = true
            previewContent.isVisible = false
            if (recyclerPreview != null) recyclerPreview.isVisible = false
            togglePreviewButton.text = "预览"
        }
    }

    companion object {
        const val EXTRA_NOTE_ID = "extra_note_id"
    }
}
