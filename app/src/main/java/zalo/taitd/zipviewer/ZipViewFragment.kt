package zalo.taitd.zipviewer

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.lottie.LottieDrawable
import kotlinx.android.synthetic.main.fragment_zip_view.*
import kotlinx.android.synthetic.main.fragment_zip_view.view.*
import java.io.File
import java.util.zip.ZipFile


class ZipViewFragment(val fileUri: String) : Fragment(), View.OnClickListener {
    private lateinit var viewModel: ZipViewFragmentViewModel
    var curZipNode: ZipNode? = null
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

        viewModel = ViewModelProvider(this, ViewModelFactory.getInstance(fileUri)).get(
            ZipViewFragmentViewModel::class.java
        )

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

    private fun getRealUriString(uri:Uri):String{
        val colonIndex = uri.path!!.indexOf(':')
        return "storage/emulated/0/"+uri.path!!.substring(colonIndex+1,uri.path!!.length)
    }

    private fun checkReadExternalStoragePermission(){
        if (ContextCompat.checkSelfPermission(context!!, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {

//            // Should we show an explanation?
//            if (shouldShowRequestPermissionRationale(
//                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
//                // Explain to the user why we need to read the contacts
//            }

            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE)

            // MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE is an
            // app-defined int constant that should be quite unique
        }else{
//            val uriRealPath = Utils.getUriRealPathCompat(context!!, fileUri)
//            val a=0
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
//                    val uriRealPath = Utils.getUriRealPathCompat(context!!, fileUri)
                } else {
                    Toast.makeText(context, "Permission not granted", Toast.LENGTH_SHORT).show()
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
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

    companion object{
        const val MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1447
    }
}