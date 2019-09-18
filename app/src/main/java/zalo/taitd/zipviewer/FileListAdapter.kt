package zalo.taitd.zipviewer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_file.view.*
import java.util.zip.ZipEntry

class FileListAdapter(private val fragment: Fragment) :RecyclerView.Adapter<FileListAdapter.FileItemViewHolder>(){
    var zipNodes:List<ZipNode> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        return FileItemViewHolder(view)
    }

    override fun getItemCount(): Int {
        return zipNodes.size
    }

    override fun onBindViewHolder(holder: FileItemViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class FileItemViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        fun bind(position:Int){
            val zipEntry = zipNodes[position].entry!!
            itemView.apply {
                fileNameTextView.text = if(zipEntry.isDirectory) zipEntry.name.substring(0,zipEntry.name.lastIndex) else zipEntry.name
                fileSizeTextView.text = if(zipEntry.isDirectory) context.getString(R.string.directory) else Utils.getFormatFileSize(zipEntry.size)
                fileIconImgView.setImageResource(Utils.getResIdFromFileExtension(context, zipEntry.name))

                setOnClickListener(fragment as View.OnClickListener)
            }
        }
    }
}