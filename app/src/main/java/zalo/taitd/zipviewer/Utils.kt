package zalo.taitd.zipviewer

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.util.zip.ZipInputStream


object Utils {
    fun getFileName(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            with(context.contentResolver.query(uri, null, null, null, null)) {
                if (this != null && this.moveToFirst()) {
                    result =
                        this.getString(this.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result!!.lastIndexOf('/')
            if (cut != -1) {
                result = result!!.substring(cut + 1)
            }
        }
        return result!!
    }

    fun getFileName(filePath: String): String {
        val filePathWithoutSeparator =
            if (filePath.endsWith('/')) filePath.substring(0, filePath.lastIndex) else filePath
        return filePathWithoutSeparator.substring(
            filePathWithoutSeparator.lastIndexOf('/') + 1,
            filePathWithoutSeparator.length
        )
    }

    fun getFormatFileSize(byteNum: Long): String {
        return when {
            byteNum < 1024 -> "${byteNum}B"
            byteNum < 1024 * 1024 -> String.format("%.1fKB", byteNum / 1024f)
            byteNum < 1024 * 1024 * 1024 -> String.format("%.2fMB", byteNum / (1024f * 1024))
            else -> String.format("%.2fGB", byteNum / (1024f * 1024 * 1024))
        }
    }

    fun getResIdFromFileExtension(context: Context, fileExtension: String): Int {
        val resourceId = context.resources.getIdentifier(
            fileExtension, "drawable",
            context.packageName
        )

        return if (resourceId != 0) resourceId else R.drawable.file
    }

    fun getFileExtension(fileName: String): String {
        val lastDotIndex = fileName.lastIndexOf('.')
        return if (lastDotIndex != -1) fileName.substring(
            lastDotIndex + 1,
            fileName.length
        ) else ""
    }
}

val Any.TAG: String
    get() = this::class.java.simpleName

fun ZipInputStream.moveToDirectEntry(entrySimpleName: String) {
    var curEntry = this.nextEntry
    while (curEntry != null && curEntry.name != entrySimpleName) {
        curEntry = this.nextEntry
    }
}

fun ZipInputStream.moveToEntry(entryFullName: String) {
    this.reset()
    entryFullName.split('/').toMutableList().forEach {
        this.moveToDirectEntry(it)
    }
}