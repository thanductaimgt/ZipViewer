package zalo.taitd.zipviewer

data class ZipInfo(
    var url:String,
    var size:Long,
    var centralDirOffset:Int,
    var centralDirSize:Int
)