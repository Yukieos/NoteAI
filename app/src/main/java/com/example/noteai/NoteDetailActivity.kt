//笔记的详情页也是编辑页
package com.example.noteai

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.noteai.ai.AiClient
import com.example.noteai.ai.OpenAiClient
import com.example.noteai.data.NoteDatabase
import com.example.noteai.data.NoteWithTags
import com.example.noteai.image.ImagePicker
import com.example.noteai.image.ImageUtils
import com.example.noteai.markdown.MarkdownBlockParser
import com.example.noteai.markdown.MarkdownParser
import com.example.noteai.ui.NotesViewModel
import com.example.noteai.ui.NotesViewModelFactory
import com.example.noteai.ui.MarkdownContentAdapter
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteDetailActivity : AppCompatActivity() {
    private val viewModel: NotesViewModel by viewModels {
        NotesViewModelFactory(NoteDatabase.getInstance(applicationContext).noteDao())
    }
    private val aiClient: AiClient by lazy { OpenAiClient() }// AI客户端（懒加载）
    private val currentTags = mutableListOf<String>()
    private val tagColors = mutableMapOf<String, String>()
    private var existingNote: NoteWithTags? = null //如果是编辑旧笔记就放这

    // 图片选择相关
    private lateinit var pickImageLauncher: ActivityResultLauncher<Unit>
    private lateinit var takePhotoLauncher: ActivityResultLauncher<Unit>

    private val defaultColor = "#E9E8E6" //我给一个默认颜色，后面parse失败就回退到它
    private val markdownParser = MarkdownParser() //用来把 markdown 转成富文本
    private val markdownBlockParser = MarkdownBlockParser() //用来把 markdown 分解成块，高效渲染
    private var isPreviewMode = false //记录当前是编辑还是预览模式
    private var contentAdapter: MarkdownContentAdapter? = null //预览模式用的适配器
    private lateinit var contentInput: EditText // 内容输入框，提升为类成员变量

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
        contentInput = findViewById<EditText>(R.id.inputContent) // 使用类成员变量
        val previewContent = findViewById<TextView>(R.id.previewContent)
        val saveButton = findViewById<Button>(R.id.buttonSave)
        val deleteButton = findViewById<ImageButton>(R.id.buttonDelete)
        val backButton = findViewById<ImageButton>(R.id.buttonBack)
        val togglePreviewButton = findViewById<Button>(R.id.buttonTogglePreview)
        val chipGroup = findViewById<ChipGroup>(R.id.chipGroupTags)
        val addTagButton = findViewById<Button>(R.id.buttonAddTag)
        val summarizeButton = findViewById<Button>(R.id.buttonSummarize)
        val aiTagsButton = findViewById<Button>(R.id.buttonAiTags)
        val buttonAddImage = findViewById<Button>(R.id.buttonAddImage) // 新增图片按钮
        // 高效预览：用 RecyclerView 替代 TextView，支持虚拟滚动
        val recyclerPreview = findViewById<RecyclerView>(R.id.recyclerContentPreview)
        if (recyclerPreview != null) {
            contentAdapter = MarkdownContentAdapter(this,emptyList())
            recyclerPreview.adapter = contentAdapter
        }

        backButton.setOnClickListener { finish() }
        
        //眼睛按钮：点击切换编辑/预览模式
        togglePreviewButton.setOnClickListener {
            isPreviewMode = !isPreviewMode
            updatePreviewMode(contentInput, previewContent, recyclerPreview, togglePreviewButton)
        }
        
        // 添加图片按钮点击事件
        buttonAddImage.setOnClickListener { showImagePickerDialog() }
        
        //编辑内容时实时更新预览
        contentInput.doOnTextChanged { text, _, _, _ ->
            if (isPreviewMode && recyclerPreview != null) {
                //用高效的块解析器，而不是一次性渲染整个文档
                val blocks = markdownBlockParser.parseToBlocks(text?.toString().orEmpty())
                contentAdapter = MarkdownContentAdapter(this,blocks)
                recyclerPreview.adapter = contentAdapter
            } else if (isPreviewMode) {
                previewContent.text = markdownParser.parse(text?.toString().orEmpty())
            }
        }

        setupImagePickers()
        
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
            if (title.isBlank() && content.isBlank()) {
                Toast.makeText(this, "请先写点内容，再生成摘要～", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }






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
        // AI 摘要
        summarizeButton.setOnClickListener {
            val title = titleInput.text.toString()
            val content = contentInput.text.toString()

            if (title.isBlank() && content.isBlank()) {
                Toast.makeText(this, "请先写点内容，再生成摘要～", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    summarizeButton.isEnabled = false
                    summarizeButton.text = "生成中..."

                    val summary = aiClient.summarizeNote(title, content)

                    // 用对话框展示摘要，并给一个插入到内容里的选项
                    AlertDialog.Builder(this@NoteDetailActivity)
                        .setTitle("AI 摘要")
                        .setMessage(summary)
                        .setPositiveButton("插入到内容开头") { _, _ ->
                            val existing = contentInput.text.toString()
                            contentInput.setText("【摘要】$summary\n\n$existing")
                        }
                        .setNegativeButton("只看看", null)
                        .show()
                } catch (e: Exception) {
                    Toast.makeText(
                        this@NoteDetailActivity,
                        "生成摘要失败：${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                } finally {
                    summarizeButton.isEnabled = true
                    summarizeButton.text = "AI 摘要"
                }
            }
        }
        // AI 生成主题标签
        aiTagsButton.setOnClickListener {
            val title = titleInput.text.toString()
            val content = contentInput.text.toString()

            if (title.isBlank() && content.isBlank()) {
                Toast.makeText(this, "请先写点内容，再让 AI 帮你想标签～", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    aiTagsButton.isEnabled = false
                    aiTagsButton.text = "生成中..."

                    val tags = aiClient.suggestTopics(title, content)

                    if (tags.isEmpty()) {
                        Toast.makeText(this@NoteDetailActivity, "AI 没有生成任何标签。", Toast.LENGTH_SHORT).show()
                    } else {
                        // 合并到 currentTags，当作普通标签处理；颜色就用默认颜色
                        tags.forEach { tagName ->
                            if (!currentTags.contains(tagName)) {
                                currentTags.add(tagName)
                                if (!tagColors.containsKey(tagName)) {
                                    tagColors[tagName] = defaultColor
                                }
                            }
                        }
                        renderTagChips(chipGroup)
                        Toast.makeText(
                            this@NoteDetailActivity,
                            "已添加标签：${tags.joinToString()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        this@NoteDetailActivity,
                        "生成标签失败：${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                } finally {
                    aiTagsButton.isEnabled = true
                    aiTagsButton.text = "AI 生成标签"
                }
            }
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
    
    // 设置图片选择器
    private fun setupImagePickers() {
        // 从相册选择图片
        pickImageLauncher = registerForActivityResult(ImagePicker.PickImageFromGallery()) {
            it?.let { uri -> handleImageSelected(uri) }
        }
        
        // 拍照
        takePhotoLauncher = registerForActivityResult(ImagePicker.TakePhoto()) {
            it?.let { uri -> handleImageSelected(uri) }
        }
    }

    private val CAMERA_PERMISSION_REQUEST_CODE = 100
    
    // 显示图片选择对话框
    private fun showImagePickerDialog() {
        AlertDialog.Builder(this)
            .setTitle("选择图片")
            .setItems(arrayOf("从相册选择", "拍照")) { _, which ->
                when (which) {
                    0 -> pickImageLauncher.launch(Unit)
                    1 -> {
                        // 检查相机权限
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                                != PackageManager.PERMISSION_GRANTED) {
                                // 请求相机权限
                                ActivityCompat.requestPermissions(
                                    this,
                                    arrayOf(Manifest.permission.CAMERA),
                                    CAMERA_PERMISSION_REQUEST_CODE
                                )
                            } else {
                                // 已有权限，直接拍照
                                takePhotoLauncher.launch(Unit)
                            }
                        } else {
                            // Android 6.0以下不需要运行时权限
                            takePhotoLauncher.launch(Unit)
                        }
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    // 处理权限请求结果
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // 权限已授予，拍照
                takePhotoLauncher.launch(Unit)
            } else {
                // 权限被拒绝，显示提示
                Toast.makeText(this, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 处理选择的图片
    private fun handleImageSelected(uri: Uri) {
        try {
            // 生成唯一的文件名
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "image_$timeStamp.jpg"
            
            // 保存图片到内部存储
            val imagePath = ImageUtils.saveImageToInternalStorage(this, uri, fileName)
            
            // 生成Markdown图片语法并插入到当前光标位置
            val markdownImage = ImageUtils.generateMarkdownImageSyntax(imagePath, "图片")
            insertTextAtCursor(contentInput, markdownImage + "\n\n")
            
            Toast.makeText(this, "图片已插入", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "处理图片失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // 在光标位置插入文本
    private fun insertTextAtCursor(editText: EditText, text: String) {
        val cursorPosition = editText.selectionStart
        val editable = editText.text
        editable.insert(cursorPosition, text)
        // 移动光标到插入文本的末尾
        editText.setSelection(cursorPosition + text.length)
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
                contentAdapter = MarkdownContentAdapter(this,blocks)
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
