package zalo.taitd.zipviewer

import android.Manifest
import android.content.Context
import android.os.Environment
import android.view.View
import android.view.inputmethod.InputMethodManager
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import android.app.Activity


object Utils {
    fun getTime(msDosTime: BitSet, msDosDate: BitSet): Long {
        val seconds = msDosTime[0, 5].toInt() * 2
        val minutes = msDosTime[5, 11].toInt()
        val hours = msDosTime[11, 16].toInt()

        val days = msDosDate[0, 5].toInt()
        val months = msDosDate[5, 9].toInt() - 1
        val years = msDosDate[9, 16].toInt() + 1980

        return Calendar.getInstance()
            .apply { set(years, months, days, hours, minutes, seconds) }
            .timeInMillis
    }

    fun createExtraBytes(extraId: Short, extraData: ByteArray): ByteArray {
        val extraIdBytes =
            ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(extraId).array()
        val extraSizeBytes =
            ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(extraData.size.toShort())
                .array()
        return extraIdBytes + extraSizeBytes + extraData
    }

    fun getExtraBytes(extra: ByteArray, extraId: Short): ByteArray {
        val extraIdBytes =
            ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(extraId).array()
        val wrapped = ByteBuffer.wrap(extra).order(ByteOrder.LITTLE_ENDIAN)
        var res: ByteArray? = null
        var i = 0
        while (i < extra.size) {
            if (extra[i] == extraIdBytes[0] && extra[i + 1] == extraIdBytes[1]) {
                val extraSize = wrapped.getShort(i + 2)
                res = extra.copyOfRange(i + 4, i + 4 + extraSize)
                break
            }
            i++
        }
        return res!!
    }

    fun openConnection(
        fileUri: String,
        rangeStart: Int? = null,
        rangeEnd: Int? = null
    ): HttpURLConnection {
        return (URL(fileUri).openConnection() as HttpURLConnection).apply {
            (rangeStart ?: rangeEnd)?.let {
                setRequestProperty(
                    "Range",
                    "bytes=${rangeStart ?: ""}-${rangeEnd ?: ""}"
                )
            }

            connect()
            if ((responseCode != Constants.HTTP_PARTIAL_CONTENT && (rangeStart != null || rangeEnd != null)) ||
                (responseCode != HttpURLConnection.HTTP_OK && (rangeStart == null && rangeEnd == null))
            ) {
                throw Throwable(
                    "$TAG: Server returned HTTP ${responseCode}: $responseMessage"
                )
            }
        }
    }

    fun getFileName(uri: String): String {
        val filePathWithoutSeparator =
            if (uri.endsWith('/')) uri.substring(0, uri.lastIndex) else uri
        return filePathWithoutSeparator.substring(
            filePathWithoutSeparator.lastIndexOf('/') + 1,
            filePathWithoutSeparator.length
        ).takeWhile { it != '#' && it != '?' }
    }

    fun getFormatFileSize(byteNum: Long): String {
        return when {
            byteNum < 1000 -> "$byteNum B"
            byteNum < 1000 * 1000 -> String.format("%.1f KB", byteNum / 1000f)
            byteNum < 1000 * 1000 * 1000 -> String.format("%.2f MB", byteNum / (1000 * 1000f))
            else -> String.format("%.2f GB", byteNum / (1000f * 1000 * 1000f))
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

    fun hideKeyboard(context: Context, view: View) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
        view.clearFocus()
    }

    fun showKeyboard(context: Context, view: View) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    fun getDownloadFolderPath(): String {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
    }

    fun isStoragePermissionsGranted(activity: Activity): Boolean {
        return ActivityCompat.checkSelfPermission(
            activity,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestStoragePermissions(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            Constants.PERMISSIONS_STORAGE,
            Constants.REQUEST_EXTERNAL_STORAGE
        )
    }
}

val Any.TAG: String
    get() = this::class.java.simpleName

fun BitSet.toInt(): Int {
    var res = 0
    for (i in 0 until this.length()) {
        res += if (this.get(i)) 1.shl(i) else 0
    }
    return res
}