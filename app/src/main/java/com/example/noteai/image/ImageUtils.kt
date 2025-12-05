package com.example.noteai.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * 图片工具类 - 提供图片处理相关的工具函数
 */
object ImageUtils {
    // 将图片URI转换为Base64字符串
    fun uriToBase64(context: Context, uri: Uri): String {
        val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        return bitmapToBase64(bitmap)
    }

    // 将Bitmap转换为Base64字符串
    fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        val imageBytes = outputStream.toByteArray()
        return Base64.encodeToString(imageBytes, Base64.DEFAULT)
    }

    // 将Base64字符串转换为Bitmap
    fun base64ToBitmap(base64String: String): Bitmap {
        val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    // 保存图片到应用内部存储
    fun saveImageToInternalStorage(context: Context, uri: Uri, fileName: String): String {
        val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        val file = File(context.filesDir, "images/$fileName")
        
        // 确保目录存在
        file.parentFile?.mkdirs()
        
        // 保存图片
        FileOutputStream(file).use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
        }
        
        return file.absolutePath
    }

    // 从文件路径加载Bitmap
    fun loadBitmapFromPath(path: String): Bitmap? {
        return try {
            val options = BitmapFactory.Options()
            // 先解码获取尺寸
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(path, options)
            
            // 计算合适的采样率
            options.inSampleSize = calculateInSampleSize(options, 1024, 1024)
            options.inJustDecodeBounds = false
            
            // 解码实际的图片
            BitmapFactory.decodeFile(path, options)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 计算图片采样率
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    // 生成Markdown图片语法
    fun generateMarkdownImageSyntax(imagePath: String, altText: String = ""): String {
        return "![$altText]($imagePath)"
    }

    // 从Markdown语法中提取图片路径
    fun extractImagePathFromMarkdown(markdown: String): String? {
        val regex = Regex("!\\[.*?\\]\\((.*?)\\)")
        val matchResult = regex.find(markdown)
        return matchResult?.groupValues?.get(1)
    }
}
