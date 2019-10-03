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
import kotlinx.android.synthetic.main.fragment_zip_view.*
import kotlinx.android.synthetic.main.fragment_zip_view.view.*


@SuppressLint("CheckResult")
class ZipViewFragment(private val zipInfo: ZipInfo) : Fragment(),
    View.OnClickListener {
    private lateinit var viewModel: ZipViewFragmentViewModel
    var curZipNode: ZipNode? = null
    private lateinit var fileViewAdapter: FileViewAdapter
    private lateinit var filePathAdapter: FilePathAdapter
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
                    startDownload(zipNode)
                } else {
                    zipNodeToDownload = zipNode
                    Utils.requestStoragePermissions(activity!!)
                }
            }
        }
    }

    private fun startDownload(zipNode: ZipNode) {
        viewModel.startDownload(zipInfo.url, zipNode) { responseCode, _ ->
            if (responseCode == Constants.RESPONSE_SUCCESS) {
                Toast.makeText(
                    context, String.format(
                        getString(R.string.downloaded),
                        if (zipNode.entry!!.isDirectory)
                            getString(R.string.directory)
                        else
                            getString(R.string.file),
                        Utils.getFileName(zipNode.entry!!.name)
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    context, String.format(
                        getString(R.string.download_error),
                        if (zipNode.entry!!.isDirectory)
                            getString(R.string.directory)
                        else
                            getString(R.string.file),
                        Utils.getFileName(zipNode.entry!!.name)
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                Constants.REQUEST_EXTERNAL_STORAGE -> startDownload(zipNodeToDownload)
            }
        }
    }

    fun setCurrentNode(zipNode: ZipNode) {
        curZipNode = zipNode
        fileViewAdapter.zipNodes = zipNode.childNodes.sortedBy { !it.entry!!.isDirectory }
        fileViewAdapter.notifyDataSetChanged()
        filePathAdapter.setCurrentNode(zipNode)
    }
}