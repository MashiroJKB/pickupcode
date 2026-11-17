package com.example.pickupcode.util

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 离线端侧 OCR 识别工具：
 * 配合 Android 系统 Photo Picker 仅读取用户主动勾选的截图 Uri，纯本地离线提取文字（绝不联网上传）。
 */
object PickupOcrHelper {

    suspend fun recognizeTextFromImageUri(
        context: Context,
        imageUri: Uri
    ): String = withContext(Dispatchers.IO) {
        val image = InputImage.fromFilePath(context, imageUri)
        val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation {
                runCatching { recognizer.close() }
            }
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    runCatching { recognizer.close() }
                    val extracted = visionText.textBlocks.joinToString("\n") { block ->
                        block.lines.joinToString("\n") { it.text }
                    }.ifBlank { visionText.text }
                    if (continuation.isActive) {
                        continuation.resume(extracted)
                    }
                }
                .addOnFailureListener { error ->
                    runCatching { recognizer.close() }
                    if (continuation.isActive) {
                        continuation.resumeWithException(error)
                    }
                }
        }
    }
}
