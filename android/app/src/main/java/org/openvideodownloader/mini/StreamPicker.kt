package org.openvideodownloader.mini

import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.Stream
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.VideoStream

internal data class PickedStream(
    val stream: Stream,
    /** True when stream includes muxed audio (progressive video+audio). */
    val muxedAudio: Boolean,
    val audioOnly: Boolean,
)

internal fun pickProgressiveStream(info: StreamInfo): PickedStream? {
    val muxed =
        info.videoStreams
            .filter { s ->
                s.isUrl &&
                    !s.isVideoOnly &&
                    s.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP
            }.maxWithOrNull(compareBy(VideoStream::height))

    if (muxed != null) {
        return PickedStream(muxed, muxedAudio = true, audioOnly = false)
    }

    val videoOnly =
        info.videoStreams
            .filter { s ->
                s.isUrl &&
                    s.isVideoOnly &&
                    s.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP
            }.maxWithOrNull(compareBy(VideoStream::height))

    if (videoOnly != null) {
        return PickedStream(videoOnly, muxedAudio = false, audioOnly = false)
    }

    val audio =
        info.audioStreams
            .filter { s -> s.isUrl && s.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP }
            .maxWithOrNull(compareBy(AudioStream::getAverageBitrate))

    if (audio != null) {
        return PickedStream(audio, muxedAudio = false, audioOnly = true)
    }

    return null
}

internal fun Stream.fileExtension(): String {
    val fmt = format ?: return ".bin"
    return ".${fmt.suffix}"
}

internal fun Stream.mimeType(): String {
    val fmt = format
    return fmt?.mimeType ?: (if (this is AudioStream) "audio/*" else "video/*")
}
