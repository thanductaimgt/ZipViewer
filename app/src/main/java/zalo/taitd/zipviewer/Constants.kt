package zalo.taitd.zipviewer

import android.Manifest

object Constants{
    const val CHOOSE_FILE = 0
    const val ZIP_EXTENSION = "zip"
    const val ERROR = -2
    const val MAX_EOCD_AND_COMMENT_SIZE = 0xFFFF + 22
    const val HTTP_PARTIAL_CONTENT = 206
    const val RELATIVE_OFFSET_LOCAL_HEADER:Short = 0x1447
    const val LOCAL_FILE_HEADER:Int = 0x04034b50
    // Storage Permissions
    const val REQUEST_EXTERNAL_STORAGE = 1
    val PERMISSIONS_STORAGE = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    const val RESPONSE_SUCCESS = 1234
    const val RESPONSE_ERROR = 4321
//    const val HTTP_RANGE_NOT_SATISFIABLE = 416
}