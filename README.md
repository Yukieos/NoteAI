目前一个workflow：
- 主页面
- <img width="333" height="744" alt="image" src="https://github.com/user-attachments/assets/789d4ebb-80a2-4e5d-a684-485e497249a6" />
上面的1/omg都是标签，如果长按那些标签可以彻底删除那些标签（是完全删掉这些标签而不是单纯的把标签从笔记中挪走），我已经写好了警告弹窗防止误删
主页面还有搜索的按钮，可以用来搜索，然后左边有一个sidebar，侧面栏，也可以看到全部的这个笔记，但是是一个list的形式。你从侧面点进笔记和直接在主页面点击笔记都可以看到笔记的详情的
**批量删除**：长按笔记可以进入多选模式，这个时候可以多选删除文件，这个我也写好了警告弹窗防止误删
如果你给一个笔记选好了这个标签，主页面是可以看到的，最上面的标签其实更像是一个filter过滤系统
<img width="326" height="744" alt="image" src="https://github.com/user-attachments/assets/157002fa-e909-45df-a547-a79d6f156fea" />
<img width="331" height="743" alt="image" src="https://github.com/user-attachments/assets/038075a0-0e21-496e-b415-00f14df3efd4" />
选择omg只能看到有omg标签的笔记
搜索功能经过测试也没有什么问题

- 详情页
- <img width="333" height="746" alt="image" src="https://github.com/user-attachments/assets/71a6cf4b-0e34-45b7-8580-933a4a333e1d" />
可以看到这个是一个很简单的一个增加tag的页面，可以选择标签的颜色，为了方便用户的快捷体验，我这边也增加了一个实时更新可以看到用户已经创造的标签。
<img width="342" height="757" alt="image" src="https://github.com/user-attachments/assets/db0af25b-30f1-45f1-bad3-f81246d4d2ea" />
整个详情页长这样，就是简单的一个标题/一个具体内容/标签，然后我们下一阶段应该是要重点搞这个渲染功能，在正文内增加代码块等渲染功能。详情页右上角也有这个删除的功能，和保存功能，所有改变一定要保存要不然会丢失的。
