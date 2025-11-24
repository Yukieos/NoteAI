# NoteAI Markdown 渲染功能说明


## 文件结构和主要改动
```
noteai/
├── markdown/
│   ├── MarkdownParser.kt              新增，富文本渲染
│   ├── MarkdownBlockParser.kt         新增，高效块解析
│   └── ContentBlock.kt                新增，内容块数据模型
├── ui/
│   ├── NoteAdapter.kt                 改动，列表预览
│   ├── MarkdownContentAdapter.kt       新增，高效内容适配器
│   └── NotesViewModel.kt              改动
├── NoteDetailActivity.kt              改动，支持高效预览
├── res/layout/
│   └── note_detail.xml                改动，添加 RecyclerView
└── 其他文件都未改动
```


## 性能优化

用 RecyclerView，只显示屏幕上能看到的内容块，其他块等你滑动到的时候再加载。这样内存占用就少了很多。

我们不是一行一行地解析 Markdown，而是按照"块"来划分，文本、标题、代码块、列表、引用这些都是独立的块。每个块对应 RecyclerView 的一个 item，所以渲染比较快

RecyclerView 会自动回收看不见的 item，用户滑开某个内容块后，那个 item 的内存就释放了。不用自己去手动管理，系统自动处理

## 已实现 Markdown 语法


# 标题一
## 标题二

这是 **加粗文本** 和 *斜体*。

code in text `this is code`

```
this is code block
this is code block
```

- 列表项1
- 列表项2

> 这是一个引用



已实现:标题、加粗、代码（我用的是灰色背景蓝色字体）和代码块（灰色背景）列表项、引用（左边加一个竖线，这个目前实现的还不太看，形式也许能换一个？

未实现：图文混排


## UI变化
右上角保存旁边加了一个图标，点一下眼睛图标进入预览模式，点一下笔的图标进入编辑模式

