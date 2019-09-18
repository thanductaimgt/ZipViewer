package zalo.taitd.zipviewer

import java.util.zip.ZipEntry

data class ZipNode(
    var entry: ZipEntry?=null,
    var childNodes:ArrayList<ZipNode> = ArrayList()
){
    fun insertEntry(entry:ZipEntry){
        var curNode = this
        val layerNames = entry.name.split('/')
        layerNames.forEachIndexed { index, layerName->
            curNode.childNodes.forEach {curZipNode->
                if(Utils.getFileName(curZipNode.entry!!.name) == layerName){
                    curNode = curZipNode
                    return@forEach
                }
            }
            if(index == layerNames.lastIndex){
                curNode.childNodes.add(ZipNode(entry))
            }
        }
    }
}