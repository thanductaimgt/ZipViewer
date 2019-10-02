package zalo.taitd.zipviewer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_file.view.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class FileViewAdapter(private val fragment: Fragment) :
    RecyclerView.Adapter<FileViewAdapter.FileViewViewHolder>() {
    var zipNodes: List<ZipNode> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        return FileViewViewHolder(view)
    }

    override fun getItemCount(): Int {
        return zipNodes.size
    }

    override fun onBindViewHolder(holder: FileViewViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class FileViewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(position: Int) {
            val zipNode = zipNodes[position]
            val zipEntry = zipNode.entry!!
            itemView.apply {
                val fileName = Utils.getFileName(zipEntry.name)
                val fileExtension = Utils.getFileExtension(fileName)
                fileNameTextView.text = fileName
                fileSizeTextView.text =
                    if (zipEntry.isDirectory) String.format(
                        context.getString(R.string.directory),
                        zipNode.childNodes.size
                    ) else Utils.getFormatFileSize(
                        zipEntry.size
                    )
                fileIconImgView.setImageResource(
                    Utils.getResIdFromFileExtension(
                        context,
                        fileExtension
                    )
                )
                val lastModifiedDate = Date(zipEntry.time)
                fileTimeTextView.text = String.format(
                    context.getString(R.string.time_format),
                    SimpleDateFormat.getDateInstance().format(lastModifiedDate),
                    SimpleDateFormat.getTimeInstance().format(lastModifiedDate)
                )

                downloadImgView.setOnClickListener(fragment as View.OnClickListener)
                infoImgView.setOnClickListener(fragment as View.OnClickListener)
                setOnClickListener(fragment as View.OnClickListener)
            }
        }
    }
}