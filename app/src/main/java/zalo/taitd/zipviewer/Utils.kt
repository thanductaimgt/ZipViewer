package zalo.taitd.zipviewer

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.RequiresApi
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder


object Utils {
    fun parseFileName(context: Context, uri: Uri): String {
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

    fun getZipCentralDirInfo(fileUri: String): Pair<Int, Int> {
        var input: InputStream? = null
        var connection: HttpURLConnection? = null
        try {
            connection = openConnection(fileUri, rangeEnd = Constants.MAX_EOCD_AND_COMMENT_SIZE)
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

    fun parseFileName(filePath: String): String {
        val filePathWithoutSeparator =
            if (filePath.endsWith('/')) filePath.substring(0, filePath.lastIndex) else filePath
        return filePathWithoutSeparator.substring(
            filePathWithoutSeparator.lastIndexOf('/') + 1,
            filePathWithoutSeparator.length
        )
    }

    fun getFormatFileSize(byteNum: Long): String {
        return when {
            byteNum < 1000 -> "${byteNum}B"
            byteNum < 1000 * 1000 -> String.format("%.1fKB", byteNum / 1000f)
            byteNum < 1000 * 1000 * 1000 -> String.format("%.2fMB", byteNum / (1000 * 1000f))
            else -> String.format("%.2fGB", byteNum / (1000f * 1000 * 1000f))
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

    /* Get uri related content real local file path. */
    @SuppressLint("NewApi")
    fun getUriRealPathCompat(context: Context, uri: Uri): String? {
        return if (isKitKatAndAbove()) {
            // Android OS above sdk version 19.
            getUriRealPathForKitkatAndAbove(context, uri)
        } else {
            // Android OS below sdk version 19
            getImageRealPath(context.contentResolver, uri, null)
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun getUriRealPathForKitkatAndAbove(context: Context, uri: Uri): String? {
        var ret: String? = ""

        if (isContentUri(uri)) {
            ret = if (isGooglePhotoDoc(uri.authority)) {
                uri.lastPathSegment
            } else {
                getImageRealPath(context.contentResolver, uri, null)
            }
        } else if (isFileUri(uri)) {
            ret = uri.path
        } else if (isDocumentUri(context, uri)) {

            // Get uri related document id.
            val documentId = DocumentsContract.getDocumentId(uri)

            // Get uri authority.
            val uriAuthority = uri.authority

            if (isMediaDoc(uriAuthority)) {
                val idArr = documentId.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                if (idArr.size == 2) {
                    // First item is document type.
                    val docType = idArr[0]

                    // Second item is document real id.
                    val realDocId = idArr[1]

                    // Get content uri by document type.
                    val mediaContentUri = when (docType) {
                        "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        else -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    }

                    // Get where clause with real document id.
                    val whereClause = MediaStore.Images.Media._ID + " = " + realDocId

                    ret = getImageRealPath(context.contentResolver, mediaContentUri, whereClause)
                }

            } else if (isDownloadDoc(uriAuthority)) {
                // Build download uri.
                val downloadUri = Uri.parse("content://downloads/public_downloads")

                // Append download document id at uri end.
                val downloadUriAppendId =
                    ContentUris.withAppendedId(downloadUri, java.lang.Long.valueOf(documentId))

                ret = getImageRealPath(context.contentResolver, downloadUriAppendId, null)

            } else if (isExternalStoreDoc(uriAuthority)) {
                val idArr = documentId.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                if (idArr.size == 2) {
                    val type = idArr[0]
                    val realDocId = idArr[1]

                    if ("primary".equals(type, ignoreCase = true)) {
                        ret = "${Environment.getExternalStorageDirectory()}/$realDocId"
                    }
                }
            }
        }

        return ret
    }

    /* Check whether current android os version is bigger than kitkat or not. */
    private fun isKitKatAndAbove(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
    }

    /* Check whether this uri represent a document or not. */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun isDocumentUri(context: Context, uri: Uri): Boolean {
        return DocumentsContract.isDocumentUri(context, uri)
    }

    /* Check whether this uri is a content uri or not.
*  content uri like content://media/external/images/media/1302716
*  */
    private fun isContentUri(uri: Uri): Boolean {
        return "content".equals(uri.scheme, ignoreCase = true)
    }

    /* Check whether this uri is a file uri or not.
*  file uri like file:///storage/41B7-12F1/DCIM/Camera/IMG_20180211_095139.jpg
* */
    private fun isFileUri(uri: Uri): Boolean {
        return "file".equals(uri.scheme, ignoreCase = true)
    }


    /* Check whether this document is provided by ExternalStorageProvider. */
    private fun isExternalStoreDoc(uriAuthority: String?): Boolean {
        return "com.android.externalstorage.documents" == uriAuthority
    }

    /* Check whether this document is provided by DownloadsProvider. */
    private fun isDownloadDoc(uriAuthority: String?): Boolean {
        return "com.android.providers.downloads.documents" == uriAuthority
    }

    /* Check whether this document is provided by MediaProvider. */
    private fun isMediaDoc(uriAuthority: String?): Boolean {
        return "com.android.providers.media.documents" == uriAuthority
    }

    /* Check whether this document is provided by google photos. */
    private fun isGooglePhotoDoc(uriAuthority: String?): Boolean {
        return "com.google.android.apps.photos.content" == uriAuthority
    }

    /* Return uri represented document file real local path.*/
    private fun getImageRealPath(
        contentResolver: ContentResolver,
        uri: Uri,
        whereClause: String?
    ): String {
        var ret = ""

        var document_id: String? = null

        // Query the uri with condition.
        with(contentResolver.query(uri, null, whereClause, null, null)) {
            this?.let { cursor ->
                if (cursor.moveToFirst()) {

//                    // Get columns name by uri type.
//                    val columnName = when(uri) {
//                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI -> MediaStore.Audio.Media.DATA
//                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI -> MediaStore.Video.Media.DATA
//                        else->MediaStore.Images.Media.DATA
//                    }
//
//                    // Get column index.
//                    val imageColumnIndex = cursor.getColumnIndex(columnName)
//
//                    // Get column value which is the uri related file local path.
//                    ret = cursor.getString(imageColumnIndex)

                    document_id = cursor.getString(0)
                }
            }
        }

        document_id = document_id!!.substring(document_id!!.lastIndexOf(":") + 1)

        with(
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                null,
                MediaStore.Images.Media._ID + " = ? ",
                arrayOf(document_id),
                null
            )
        ) {
            this?.let { cursor ->
                cursor.moveToFirst()
                ret = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
            }
        }

        return ret
    }

    fun isHttpUri(uri: String): Boolean {
        return uri.startsWith("http")
    }
}

val Any.TAG: String
    get() = this::class.java.simpleName