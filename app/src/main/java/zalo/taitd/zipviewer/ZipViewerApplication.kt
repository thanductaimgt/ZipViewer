package zalo.taitd.zipviewer

import android.app.Application

class ZipViewerApplication :Application(){
    companion object{
        val zipTreeCaches = HashMap<String, ZipNode>()
    }
}