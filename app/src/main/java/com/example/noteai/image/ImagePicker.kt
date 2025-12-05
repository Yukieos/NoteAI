package com.example.noteai.image

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 图片选择器 - 提供从相册选择图片或拍照的功能
 */
class ImagePicker {
    // 从相册选择图片的Contract
    class PickImageFromGallery : ActivityResultContract<Unit, Uri?>() {
        override fun createIntent(context: Context, input: Unit): Intent {
            return Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                .setType("image/*")
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return intent?.data
        }
    }

    // 拍照的Contract
    class TakePhoto : ActivityResultContract<Unit, Uri?>() {
        private var photoUri: Uri? = null

        override fun createIntent(context: Context, input: Unit): Intent {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "JPEG_${timeStamp}_"
            
            // 创建临时文件存储拍照结果
            val storageDir = context.getExternalFilesDir("images")
            storageDir?.mkdirs() // 确保目录存在
            val imageFile = File.createTempFile(fileName, ".jpg", storageDir)
            
            // 使用FileProvider创建安全的URI
            val packageName = context.applicationContext.packageName
            photoUri = FileProvider.getUriForFile(
                context,
                "$packageName.fileprovider",
                imageFile
            )
            
            // 创建相机Intent
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            
            // 授予权限
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val resInfoList = context.packageManager.queryIntentActivities(intent, 0)
                for (resolveInfo in resInfoList) {
                    val packageName = resolveInfo.activityInfo.packageName
                    context.grantUriPermission(packageName, photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } else {
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            return intent
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return photoUri
        }
    }
}
