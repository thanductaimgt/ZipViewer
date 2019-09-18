package zalo.taitd.zipviewer

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_zip_view.*
import kotlinx.android.synthetic.main.fragment_zip_view.view.*
import java.util.zip.ZipInputStream


class ZipViewFragment(private val fileUri:Uri) :Fragment(), View.OnClickListener{
    private lateinit var rootZipNode:ZipNode

    override fun onClick(v: View) {
        when(v.id){
            R.id.rootItemView->{
                val position = recyclerView.getChildLayoutPosition(v)
                val zipNode = adapter.zipNodes[position]
                setCurrentNode(zipNode)
            }
        }
    }

    private lateinit var adapter:FileListAdapter

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
        rootZipNode = parseZipTree(zipInputStream)
        setCurrentNode(rootZipNode)
    }

    private fun setCurrentNode(zipNode: ZipNode){
        adapter.zipNodes = zipNode.childNodes
        adapter.notifyDataSetChanged()
    }

    private fun parseZipTree(zipInputStream:ZipInputStream):ZipNode{
        val rootZipNode = ZipNode()
        var curEntry = zipInputStream.nextEntry
        while (curEntry!=null){
            rootZipNode.insertEntry(curEntry)
            curEntry = zipInputStream.nextEntry
        }
        return rootZipNode
    }

    private fun initView(view: View){
        view.apply {
            adapter = FileListAdapter(this@ZipViewFragment)
            recyclerView.apply {
                adapter = this@ZipViewFragment.adapter
                layoutManager = LinearLayoutManager(context)
            }
        }
    }
}