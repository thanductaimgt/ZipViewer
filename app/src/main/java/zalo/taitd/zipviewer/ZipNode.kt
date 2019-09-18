package zalo.taitd.zipviewer

import java.util.zip.ZipEntry

data class ZipNode(
    var entry: ZipEntry? = null,
    var parentNode: ZipNode? = null,
    var childNodes: HashMap<String, ZipNode> = HashMap()
) {
    fun insertEntry(entry: ZipEntry) {
        var curNode = this
        val layerNames = entry.name.split('/').filter { it != "" }
        layerNames.forEachIndexed { index, layerName ->
            if (index == layerNames.lastIndex) {
                curNode.childNodes[Utils.getFileName(entry.name)] = ZipNode(entry, curNode)
            } else {
                curNode = curNode.childNodes[layerName]!!
            }
        }
    }
}