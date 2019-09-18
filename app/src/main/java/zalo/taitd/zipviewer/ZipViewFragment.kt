package zalo.taitd.zipviewer

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.lottie.LottieDrawable
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_zip_view.*
import kotlinx.android.synthetic.main.fragment_zip_view.view.*
import java.lang.Exception
import java.util.zip.ZipInputStream


@SuppressLint("CheckResult")
class ZipViewFragment(private val fileUri: Uri) : Fragment(), View.OnClickListener {
    private lateinit var rootZipNode: ZipNode
    private val compositeDisposable = CompositeDisposable()
    var curZipNode: ZipNode? = null

    override fun onClick(v: View) {
        when (v.id) {
            R.id.rootItemView -> {
                val position = recyclerView.getChildLayoutPosition(v)
                val zipNode = adapter.zipNodes[position]
                if (zipNode.entry == null || zipNode.entry!!.isDirectory) {
                    setCurrentNode(zipNode)
                } else {
                    Toast.makeText(context, "Open File Not Implemented =)", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private lateinit var adapter: FileListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_zip_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initView(view)
        val zipInputStream = ZipInputStream(context!!.contentResolver.openInputStream(fileUri))

        animView.visibility = View.VISIBLE
        animView.playAnimation()

        Single.fromCallable {
            parseZipTree(zipInputStream)
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(ParseZipTreeObserver())
    }

    fun setCurrentNode(zipNode: ZipNode) {
        curZipNode = zipNode
        adapter.zipNodes = ArrayList<ZipNode>().apply {
            addAll(zipNode.childNodes.values.filter { it.entry!!.isDirectory }.sortedBy { it.entry!!.name })
            addAll(zipNode.childNodes.values.filter { !it.entry!!.isDirectory }.sortedBy { it.entry!!.name })
        }
        adapter.notifyDataSetChanged()
    }

    private fun parseZipTree(zipInputStream: ZipInputStream): ZipNode {
        val rootZipNode = ZipNode()
        var curEntry = zipInputStream.nextEntry
        while (curEntry != null) {
            rootZipNode.insertEntry(curEntry)
            try {
                curEntry = zipInputStream.nextEntry
            } catch (e: Exception) {
                curEntry = null
                e.printStackTrace()
            }
        }
        zipInputStream.close()
        return rootZipNode
    }

    private fun initView(view: View) {
        view.apply {
            adapter = FileListAdapter(this@ZipViewFragment)
            recyclerView.apply {
                adapter = this@ZipViewFragment.adapter
                layoutManager = LinearLayoutManager(context)
            }

            animView.repeatCount = LottieDrawable.INFINITE
        }
    }

    inner class ParseZipTreeObserver : SingleObserver<ZipNode> {
        override fun onSuccess(zipNode: ZipNode) {
            rootZipNode = zipNode
            setCurrentNode(rootZipNode)
            animView.cancelAnimation()
            animView.visibility = View.GONE
        }

        override fun onSubscribe(d: Disposable) {
            compositeDisposable.add(d)
        }

        override fun onError(e: Throwable) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }
}