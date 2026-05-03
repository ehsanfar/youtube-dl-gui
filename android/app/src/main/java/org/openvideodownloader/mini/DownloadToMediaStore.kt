package org.openvideodownloader.mini

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

private val downloadClient: OkHttpClient =
    OkHttpClient.Builder()
        .readTimeout(5, TimeUnit.MINUTES)
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

internal suspend fun downloadStreamToDownloads(
    context: Context,
    mediaUrl: String,
    displayName: String,
    mimeType: String,
    userAgent: String,
): Uri =
    withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val values =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val itemUri =
            resolver.insert(collection, values)
                ?: throw IOException("Could not create MediaStore row")

        try {
            resolver.openOutputStream(itemUri, "w").use { out ->
                if (out == null) throw IOException("Could not open output stream")
                val req =
                    Request.Builder()
                        .url(mediaUrl)
                        .header("User-Agent", userAgent)
                        .build()
                downloadClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        throw IOException("HTTP ${resp.code}")
                    }
                    val body = resp.body ?: throw IOException("Empty body")
                    body.byteStream().use { input -> input.copyTo(out) }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
            }
            itemUri
        } catch (e: Exception) {
            resolver.delete(itemUri, null, null)
            throw e
        }
    }

internal fun sanitizeFileName(title: String): String {
    val trimmed = title.trim().ifEmpty { "download" }
    return trimmed.replace(Regex("""[^\w.\- ()\[\]]+"""), "_").take(120)
}
