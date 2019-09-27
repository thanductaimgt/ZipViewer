package zalo.taitd.zipviewer

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder


object Utils {
    fun getZipCentralDirInfo(fileUri: String): Pair<Int, Int> {
        var input: InputStream? = null
        var connection: HttpURLConnection? = null
        try {
            connection = openConnection(fileUri)//, rangeEnd = Constants.MAX_EOCD_AND_COMMENT_SIZE)
            input = connection.inputStream

            val data = ByteArray(4096)
            val wrapped = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

            var count = input.read(data)
            var isEocdFound = false
            var i: Int
            var centralDirOffset = -1
            var centralDirSize = -1
            while (count != -1) {
                i = 0
                while (i < count - 21) {
                    if (data[i] == 0x50.toByte() && data[i + 1] == 0x4B.toByte() && data[i + 2] == 0x05.toByte() && data[i + 3] == 0x06.toByte()) {
                        centralDirOffset = wrapped.getInt(i + 16)
                        centralDirSize = wrapped.getInt(i + 12)
                        isEocdFound = true
                        break
                    }
                    i++
                }
                if (isEocdFound) {
                    break
                }
                count = input.read(data)
            }

            return Pair(centralDirOffset, centralDirSize)
        } catch (e:Throwable){
            e.printStackTrace()
            return Pair(Constants.ERROR, Constants.ERROR)
        } finally {
            //close all resources
            input?.close()
            connection?.disconnect()
        }
    }

    fun getFileSize(fileUri: String): Long {
        val input: InputStream? = null
        var connection: HttpURLConnection? = null
        return try {
            connection = openConnection(fileUri)
            connection.contentLength.toLong()
        } catch (e: Throwable) {
            e.printStackTrace()
            Constants.ERROR.toLong()
        } finally {
            //close all resources
            input?.close()
            connection?.disconnect()
        }
    }

    fun openConnection(
        fileUri: String,
        rangeStart: Int? = null,
        rangeEnd: Int? = null
    ): HttpURLConnection {
        return (URL(fileUri).openConnection() as HttpURLConnection).apply {
            (rangeStart ?: rangeEnd)?.let {
//                setRequestProperty(
//                    "Range",
//                    "bytes=${rangeStart ?: ""}-${rangeEnd ?: ""}"
//                )
                setRequestProperty(
                    "Range",
                    "bytes=${Constants.LENGTH-rangeEnd!! ?: ""}-${""?: ""}"
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

    fun parseFileName(uri: String): String {
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
}

val Any.TAG: String
    get() = this::class.java.simpleName