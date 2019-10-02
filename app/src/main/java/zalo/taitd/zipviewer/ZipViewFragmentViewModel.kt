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
import java.util.*
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.collections.ArrayList


@SuppressLint("CheckResult")
class ZipViewFragmentViewModel(zipInfo: ZipInfo) : ViewModel() {
    val liveRootNode = MutableLiveData<NullableZipNode>()
    private val compositeDisposable = CompositeDisposable()

    init {
        ZipViewerApplication.zipTreeCaches[zipInfo.url]?.let {
            liveRootNode.value = it
        } ?: Single.fromCallable {
            val zipEntries = getFileEntries(zipInfo)
            buildZipTree(zipEntries).also { rootNode ->
                ZipViewerApplication.zipTreeCaches[zipInfo.url] = rootNode
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(BuildZipTreeObserver())
    }

    private fun getFileEntries(
        zipInfo: ZipInfo
    ): List<ZipEntry> {
        return if (zipInfo.centralDirOffset == -1 && zipInfo.centralDirSize == -1) {
            getFileEntriesFromLocalUri(zipInfo.url)
        } else {
            getFileEntriesFromHttpUri(zipInfo)
        }
    }

    private fun getFileEntriesFromLocalUri(fileUri: String): List<ZipEntry> {
        // check url here
        return ZipFile(fileUri).entries().toList()
    }

    private fun getFileEntriesFromHttpUri(
        zipInfo: ZipInfo
    ): List<ZipEntry> {
        val zipEntries = ArrayList<ZipEntry>()

        var input: InputStream? = null
        var connection: HttpURLConnection? = null
        try {
            connection =
                Utils.openConnection(
                    zipInfo.url,
                    zipInfo.centralDirOffset,
                    zipInfo.centralDirOffset + zipInfo.centralDirSize - 1
                )
            input = connection.inputStream

            val data = ByteArray(zipInfo.centralDirSize)
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
                val lastModifiedTime = data.copyOfRange(i + 12, i + 14)
                val lastModifiedDate = data.copyOfRange(i + 14, i + 16)
                val crC32 = data.copyOfRange(i + 16, i + 20)
                val compressedSize = wrapped.getInt(i + 20)
                val uncompressedSize = wrapped.getInt(i + 24)
                val fileNameLength = wrapped.getShort(i + 28)
                val extraFieldLength = wrapped.getShort(i + 30)
                val fileCommentLength = wrapped.getShort(i + 32)
                val localHeaderRelativeOffset = data.copyOfRange(i + 42, i + 46)
                val fileExtra =
                    data.copyOfRange(
                        i + 46 + fileNameLength,
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
                    crc = CRC32().apply { update(crC32) }.value
                    setCompressedSize(compressedSize.toLong())
                    size = uncompressedSize.toLong()
                    method = compressionMethod.toInt()

                    //add localHeaderRelativeOffset to extra
                    extra = fileExtra + Utils.createExtraBytes(
                        Constants.RELATIVE_OFFSET_LOCAL_HEADER,
                        localHeaderRelativeOffset
                    )
                    time = Utils.getTime(
                        BitSet.valueOf(lastModifiedTime),
                        BitSet.valueOf(lastModifiedDate)
                    )
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