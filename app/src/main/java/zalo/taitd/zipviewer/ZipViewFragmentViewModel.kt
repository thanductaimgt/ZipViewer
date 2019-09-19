package zalo.taitd.zipviewer

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.Completable
import io.reactivex.CompletableObserver
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

@SuppressLint("CheckResult")
class ZipViewFragmentViewModel(zipInputStream: ZipInputStream) : ViewModel() {
    val liveRootNode = MutableLiveData<ZipNode?>()
    private var isTreeParsed: Boolean = false
    private val compositeDisposable = CompositeDisposable()

    init {
        Completable.fromCallable {
            parseZipTree(zipInputStream)
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(ParseZipTreeObserver())
    }

    private fun parseZipTree(zipInputStream: ZipInputStream) {
        var curEntry = zipInputStream.nextEntry
        val entryQueue: Queue<ZipEntry> = LinkedList()

        Single.fromCallable {
            insertNodesFromQueue(entryQueue)
        }.subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(InsertNodesObserver())

        try {
            while (curEntry != null) {
                entryQueue.offer(curEntry)
                Log.d(TAG, "enqueue: $curEntry")
                curEntry = zipInputStream.nextEntry
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            zipInputStream.close()
        }
    }

    private fun insertNodesFromQueue(entryQueue: Queue<ZipEntry>): ZipNode {
        val rootNode = ZipNode()
        var lastInsertedNode = rootNode
        while (!isTreeParsed || entryQueue.isNotEmpty()) {
            if (entryQueue.isNotEmpty()) {
                val curEntry = entryQueue.poll()
                Log.d(TAG, "dequeue: $curEntry")
                lastInsertedNode = lastInsertedNode.insertEntry(
                    curEntry,
                    curEntry.name.split('/').filter { it != "" }.size
                )
            }
        }
        return rootNode
    }

    inner class ParseZipTreeObserver : CompletableObserver {
        override fun onComplete() {
            isTreeParsed = true
        }

        override fun onSubscribe(d: Disposable) {
            compositeDisposable.add(d)
        }

        override fun onError(e: Throwable) {
            e.printStackTrace()
        }
    }

    inner class InsertNodesObserver : SingleObserver<ZipNode> {
        override fun onSuccess(zipNode: ZipNode) {
            liveRootNode.value = zipNode
        }

        override fun onSubscribe(d: Disposable) {
            compositeDisposable.add(d)
        }

        override fun onError(e: Throwable) {
            e.printStackTrace()
            liveRootNode.value = null
        }
    }

    override fun onCleared() {
        compositeDisposable.clear()
        super.onCleared()
    }
}