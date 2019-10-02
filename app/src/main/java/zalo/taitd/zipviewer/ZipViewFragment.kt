package zalo.taitd.zipviewer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.lottie.LottieDrawable
import io.reactivex.Completable
import io.reactivex.CompletableObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_zip_view.*
import kotlinx.android.synthetic.main.fragment_zip_view.view.*
import java.io.File
import java.net.HttpURLConnection
import java.nio.ByteBuffer
import java.util.zip.*


@SuppressLint("CheckResult")
class ZipViewFragment(private val zipInfo: ZipInfo) : Fragment(),
    View.OnClickListener {
    private lateinit var viewModel: ZipViewFragmentViewModel
    var curZipNode: ZipNode? = null
    private lateinit var fileViewAdapter: FileViewAdapter
    private lateinit var filePathAdapter: FilePathAdapter
    private val compositeDisposable = CompositeDisposable()
    private lateinit var zipNodeToDownload: ZipNode

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this, ViewModelFactory.getInstance(zipInfo)).get(
            ZipViewFragmentViewModel::class.java
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_zip_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initView(view)

        viewModel.liveRootNode.observe(viewLifecycleOwner, Observer {
            animView?.cancelAnimation()
            animView?.visibility = View.GONE
            if (it is ZipNode) {
                setCurrentNode(it)
            } else {
                Toast.makeText(context, getString(R.string.error_occurred), Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private fun initView(view: View) {
        view.apply {
            fileViewAdapter = FileViewAdapter(this@ZipViewFragment)
            fileViewRecyclerView.apply {
                adapter = this@ZipViewFragment.fileViewAdapter
                layoutManager = LinearLayoutManager(context)
            }

            filePathAdapter = FilePathAdapter(this@ZipViewFragment)
            filePathRecyclerView.apply {
                adapter = this@ZipViewFragment.filePathAdapter
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            }

            animView.repeatCount = LottieDrawable.INFINITE
            animView.visibility = View.VISIBLE
            animView.playAnimation()
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.rootItemView -> {
                val position = fileViewRecyclerView.getChildLayoutPosition(view)
                val zipNode = fileViewAdapter.zipNodes[position]
                if (zipNode.entry == null || zipNode.entry!!.isDirectory) {
                    setCurrentNode(zipNode)
                } else {
                    Toast.makeText(context, "Open File Not Implemented =)", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            R.id.pathTextView -> {
                val position = filePathRecyclerView.getChildLayoutPosition(view)
                val zipNode = filePathAdapter.zipNodes[position]
                setCurrentNode(zipNode)
            }
            R.id.downloadImgView -> {
                val position = fileViewRecyclerView.getChildLayoutPosition(view.parent as View)
                val zipNode = fileViewAdapter.zipNodes[position]
                if (Utils.isStoragePermissionsGranted(activity!!)) {
                    startDownloadFile(zipNode)
                } else {
                    zipNodeToDownload = zipNode
                    Utils.requestStoragePermissions(activity!!)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                Constants.REQUEST_EXTERNAL_STORAGE -> startDownloadFile(zipNodeToDownload)
            }
        }
    }

    private fun startDownloadFile(zipNode: ZipNode) {
        Completable.fromCallable { downloadFile(zipNode) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(DownloadFileObserver(zipNode))
    }

    private fun downloadFile(zipNode: ZipNode) {
        val localHeaderRelativeOffsetExtra =
            Utils.getExtraBytes(zipNode.entry!!.extra, Constants.RELATIVE_OFFSET_LOCAL_HEADER)
        val localHeaderRelativeOffset = ByteBuffer.wrap(localHeaderRelativeOffsetExtra).int

        val localHeaderLength =
            30 + zipNode.entry!!.name.length + zipNode.entry!!.extra.size - (4 + localHeaderRelativeOffsetExtra.size)

        val dataStartOffset = localHeaderRelativeOffset + localHeaderLength

        var connection: HttpURLConnection? = null
        try {
            connection = Utils.openConnection(
                zipInfo.url,
                dataStartOffset,
                dataStartOffset + zipNode.entry!!.compressedSize.toInt()
            )

            connection.inputStream
                .let {
                    if (zipNode.entry!!.method == ZipEntry.DEFLATED) InflaterInputStream(it) else it
                }
                .use { inputStream ->
                    File("${Utils.getDownloadFolderPath()}/${Utils.getFileName(zipNode.entry!!.name)}").outputStream()
                        .use { outputStream ->
                            val data = ByteArray(4096)
                            var count = inputStream.read(data)
                            if (false && zipNode.entry!!.method == ZipEntry.DEFLATED) {
                                val inflater = Inflater()
                                val result = ByteArray(4096 * 2)
                                var resultLength: Int
                                while (count != -1) {
                                    inflater.setInput(data)
                                    resultLength = inflater.inflate(result)

                                    outputStream.write(result, 0, resultLength)
                                    count = inputStream.read(data)
                                }
                                inflater.end()
                            } else {
                                while (count != -1) {
                                    outputStream.write(data, 0, count)
                                    count = inputStream.read(data)
                                }
                            }
                        }
                }
        } catch (t: Throwable) {
            t.printStackTrace()
        } finally {
            connection?.disconnect()
        }
    }

    fun setCurrentNode(zipNode: ZipNode) {
        curZipNode = zipNode
        fileViewAdapter.zipNodes = zipNode.childNodes.sortedBy { !it.entry!!.isDirectory }
        fileViewAdapter.notifyDataSetChanged()
        filePathAdapter.setCurrentNode(zipNode)
    }

    inner class DownloadFileObserver(private val zipNode: ZipNode) : CompletableObserver {
        override fun onComplete() {
            Toast.makeText(
                context!!,
                "File ${Utils.getFileName(zipNode.entry!!.name)} downloaded",
                Toast.LENGTH_SHORT
            ).show()
        }

        override fun onSubscribe(d: Disposable) {
            Toast.makeText(
                context!!,
                "File ${Utils.getFileName(zipNode.entry!!.name)} downloading",
                Toast.LENGTH_SHORT
            ).show()
            compositeDisposable.add(d)
        }

        override fun onError(e: Throwable) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        compositeDisposable.dispose()
        super.onDestroy()
    }
}