package zalo.taitd.zipviewer

import android.net.Uri
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
import java.util.zip.ZipInputStream


class ZipViewFragment(val fileUri: Uri) : Fragment(), View.OnClickListener {
    private lateinit var viewModel: ZipViewFragmentViewModel
    var curZipNode: ZipNode? = null
    private lateinit var adapter: FileListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val zipInputStream = ZipInputStream(context!!.contentResolver.openInputStream(fileUri))

        viewModel = ViewModelProvider(this, ViewModelFactory.getInstance(zipInputStream)).get(
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
            if (it != null) {
                setCurrentNode(it)
            } else {
                Toast.makeText(context, "Load File Error =(", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun initView(view: View) {
        view.apply {
            adapter = FileListAdapter(this@ZipViewFragment)
            recyclerView.apply {
                adapter = this@ZipViewFragment.adapter
                layoutManager = LinearLayoutManager(context)
            }

            animView.repeatCount = LottieDrawable.INFINITE
            animView.visibility = View.VISIBLE
            animView.playAnimation()
        }
    }

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

    fun setCurrentNode(zipNode: ZipNode) {
        curZipNode = zipNode
        adapter.zipNodes = zipNode.childNodes.sortedBy { !it.entry!!.isDirectory }
        adapter.notifyDataSetChanged()
    }
}