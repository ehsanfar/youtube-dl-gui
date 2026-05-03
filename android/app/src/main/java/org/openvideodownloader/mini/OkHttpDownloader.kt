package org.openvideodownloader.mini

import okhttp3.ConnectionSpec
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Minimal [Downloader] for NewPipeExtractor (mirrors test helper behavior: TLS + User-Agent).
 */
class OkHttpDownloader : Downloader() {

    private val client: OkHttpClient =
        OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
            .connectionSpecs(listOf(ConnectionSpec.RESTRICTED_TLS))
            .build()

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val body =
            request.dataToSend()?.let { bytes ->
                bytes.toRequestBody(OCTET_STREAM)
            }

        val builder =
            okhttp3.Request.Builder()
                .method(request.httpMethod(), body)
                .url(request.url())
                .addHeader("User-Agent", USER_AGENT)

        request.headers().forEach { (name, values) ->
            builder.removeHeader(name)
            values.forEach { builder.addHeader(name, it) }
        }

        client.newCall(builder.build()).execute().use { resp ->
            if (resp.code == 429) {
                throw ReCaptchaException("reCaptcha Challenge requested", request.url())
            }
            val text: String? = resp.body?.string()
            return Response(
                resp.code,
                resp.message,
                resp.headers.toMultimap(),
                text,
                resp.request.url.toString(),
            )
        }
    }

    companion object {
        private val OCTET_STREAM = "application/octet-stream".toMediaType()
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"
    }
}
