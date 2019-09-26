package zalo.taitd.zipviewer

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.InputStream
import java.net.HttpURLConnection
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipEntry
import java.util.zip.ZipFile


@SuppressLint("CheckResult")
class ZipViewFragmentViewModel(fileInfo: Triple<String, Int, Int>) : ViewModel() {
    val liveRootNode = MutableLiveData<NullableZipNode>()
    private val compositeDisposable = CompositeDisposable()

    init {
        val fileUri = fileInfo.first
        val centralDirOffset = fileInfo.second
        val centralDirSize = fileInfo.third

        ZipViewerApplication.zipTreeCaches[fileUri]?.let {
            liveRootNode.value = it
        } ?: Single.fromCallable {
            val zipEntries = getFileEntries(fileUri, centralDirOffset, centralDirSize)
            buildZipTree(zipEntries).also { rootNode ->
                ZipViewerApplication.zipTreeCaches[fileUri] = rootNode
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(BuildZipTreeObserver())
    }

    private fun getFileEntries(
        fileUri: String,
        centralDirOffset: Int,
        centralDirSize: Int
    ): List<ZipEntry> {
        return if (centralDirOffset == -1 && centralDirSize == -1) {
            getFileEntriesFromLocalUri(fileUri)
        } else {
            getFileEntriesFromHttpUri(fileUri, centralDirOffset, centralDirSize)
        }
    }

    private fun getFileEntriesFromLocalUri(fileUri: String): List<ZipEntry> {
        // check uri here
        return ZipFile(fileUri).entries().toList()
    }

    private fun getFileEntriesFromHttpUri(
        fileUri: String,
        centralDirOffset: Int,
        centralDirSize: Int
    ): List<ZipEntry> {
        val zipEntries = ArrayList<ZipEntry>()

        var input: InputStream? = null
        var connection: HttpURLConnection? = null
        try {
            connection =
                Utils.openConnection(
                    fileUri,
                    centralDirOffset,
                    centralDirOffset + centralDirSize - 1
                )
            input = connection.inputStream

            val data = ByteArray(centralDirSize)
            val buffer = ByteArray(4096)
            var count = input!!.read(buffer)
            var readByteNum = 0
            while (count != -1) {
                for (i in 0 until count) {
                    data[readByteNum + i] = buffer[i]
                }
                readByteNum += count
                count = input.read(buffer)
            }
            val wrapped = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

            var i = 0
            while (i < data.size) {
                val bitFlag = wrapped.getShort(i + 8)
                val compressionMethod = wrapped.getShort(i + 10)
                val lastModifiedTime = wrapped.getShort(i + 12)
                val lastModifiedDate = wrapped.getShort(i + 14)
                val lastModified = wrapped.getInt(i + 12)
                val crC32 = wrapped.getInt(i + 16)
                val compressedSize = wrapped.getInt(i + 20)
                val uncompressedSize = wrapped.getInt(i + 24)
                val fileNameLength = wrapped.getShort(i + 28)
                val extraFieldLength = wrapped.getShort(i + 30)
                val fileCommentLength = wrapped.getShort(i + 32)
                val relativeOffsetLocal = wrapped.getInt(i + 42)
                val fileExtra =
                    data.copyOfRange(
                        i + 46 + fileCommentLength,
                        i + 46 + fileNameLength + extraFieldLength
                    )
                val fileComment = data.copyOfRange(
                    i + 46 + fileNameLength + extraFieldLength,
                    i + 46 + fileNameLength + extraFieldLength + fileCommentLength
                ).toString()
                val utfLabel =
                    if (bitFlag.toInt().and(0x800) != 0) Charsets.UTF_8 else Charsets.US_ASCII
                val fileName = String(data.copyOfRange(i + 46, i + 46 + fileNameLength), utfLabel)

                zipEntries.add(ZipEntry(fileName).apply {
                    comment = fileComment
//                    crc = crC32.toLong()
                    setCompressedSize(compressedSize.toLong())
                    size = uncompressedSize.toLong()
                    method = compressionMethod.toInt()
                    extra = fileExtra
                    time = lastModifiedTime.toLong() * 2 * 1000
                })
//                    ZipEntry(fileName, fileComment, crC32, compressedSize,
//                    uncompressedSize, compressionMethod, lastModifiedTime, fileExtra,
//                    relativeOffsetLocal))
//                entries.push({
//                    directory: filename.endsWith('/'),
//                    fileName: filename,
//                    uncompressedSize: uncompressedSize,
//                    compressedSize: compressedSize,
//                    offset: relativeOffsetLocal + 30 + fileNameLength + extraFieldLength,
//                    isPhoto: ZCommon.isPhoto(filename, '')
//                });
                i += 46 + fileNameLength + extraFieldLength + fileCommentLength
            }
        } finally {
            //close all resources
            input?.close()
            connection?.disconnect()
        }
        return zipEntries
    }

    private fun buildZipTree(zipEntries: List<ZipEntry>): ZipNode {
        val rootNode = ZipNode()
        var lastInsertedNode = rootNode

        zipEntries.forEach { zipEntry ->
            lastInsertedNode = lastInsertedNode.insertEntry(
                zipEntry,
                zipEntry.name.split('/').filter { it != "" }.size
            )
        }
        return rootNode
    }

    inner class BuildZipTreeObserver : SingleObserver<ZipNode> {
        override fun onSuccess(rootNode: ZipNode) {
            liveRootNode.value = rootNode
        }

        override fun onSubscribe(d: Disposable) {
            compositeDisposable.add(d)
        }

        override fun onError(e: Throwable) {
            liveRootNode.value = NullZipNode()
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        compositeDisposable.clear()
        super.onCleared()
    }
}