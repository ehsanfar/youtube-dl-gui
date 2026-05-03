package org.openvideodownloader.mini

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.stream.StreamInfo

data class MiniUiState(
    val busy: Boolean = false,
    val status: String? = null,
    val error: String? = null,
)

class MainViewModel : ViewModel() {

    private val _state = MutableStateFlow(MiniUiState())
    val state: StateFlow<MiniUiState> = _state.asStateFlow()

    fun download(context: Context, rawUrl: String) {
        val url = rawUrl.trim()
        if (url.isEmpty()) return

        viewModelScope.launch {
            _state.value = MiniUiState(busy = true, status = "Resolving URL…", error = null)
            try {
                val info =
                    withContext(Dispatchers.IO) {
                        StreamInfo.getInfo(url)
                    }

                val picked = pickProgressiveStream(info)
                if (picked == null) {
                    _state.value =
                        MiniUiState(
                            busy = false,
                            status = null,
                            error =
                                "No progressive HTTP stream available. " +
                                    "Many videos are DASH-only; use the desktop app for full formats.",
                        )
                    return@launch
                }

                val ext = picked.stream.fileExtension()
                val fileName = "${sanitizeFileName(info.name)}$ext"
                val statusMsg =
                    when {
                        picked.muxedAudio -> "Downloading to Downloads…"
                        picked.audioOnly -> "Downloading audio only…"
                        else -> "Downloading video only (no muxed audio)…"
                    }
                _state.value = MiniUiState(busy = true, status = statusMsg, error = null)

                withContext(Dispatchers.IO) {
                    downloadStreamToDownloads(
                        context.applicationContext,
                        picked.stream.content,
                        fileName,
                        picked.stream.mimeType(),
                        DOWNLOAD_USER_AGENT,
                    )
                }

                _state.value =
                    MiniUiState(busy = false, status = "Saved: $fileName", error = null)
            } catch (e: Exception) {
                _state.value =
                    MiniUiState(
                        busy = false,
                        status = null,
                        error = e.message ?: e.javaClass.simpleName,
                    )
            }
        }
    }

    companion object {
        /** Same UA as [OkHttpDownloader] for media segment requests. */
        private const val DOWNLOAD_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"
    }
}
