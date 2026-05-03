package org.openvideodownloader.mini

import android.app.Application
import org.schabi.newpipe.extractor.NewPipe

class MiniApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NewPipe.init(OkHttpDownloader())
    }
}
