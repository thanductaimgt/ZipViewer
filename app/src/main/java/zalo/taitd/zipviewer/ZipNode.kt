package zalo.taitd.zipviewer

import java.util.zip.ZipEntry

data class ZipNode(
    var entry: ZipEntry? = null,
    var parentNode: ZipNode? = null,
    var level: Int = 0,
    var childNodes: ArrayList<ZipNode> = ArrayList()
) {
    fun insertEntry(entry: ZipEntry, level: Int): ZipNode {
        return if (level > this.level) {
            ZipNode(entry, this, level).also { childNodes.add(it) }
        } else {
            parentNode!!.insertEntry(entry, level)
        }
    }
}