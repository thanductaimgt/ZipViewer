package zalo.taitd.zipviewer

data class MyZipEntry(
    val name:String,
    val lastModified:Long,
    val size:Long
){
    fun isDirectory():Boolean{
        return size==0.toLong()
    }
}