package zalo.taitd.zipviewer

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.PagerAdapter


class MainActivityAdapter(private val context: Context, fm: FragmentManager) :
    FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    val fileUris = ArrayList<String>()
    var curFragment: Fragment? = null
    var lastRemovedUri: String? = null
    var lastRemovedIndex: Int? = null

    override fun getItem(position: Int): Fragment {
        return ZipViewFragment(fileUris[position])
    }

    override fun getCount(): Int {
        return fileUris.size
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return Utils.getFileName(fileUris[position])
    }

    override fun setPrimaryItem(container: ViewGroup, position: Int, any: Any) {
        if (curFragment !== any) {
            curFragment = any as Fragment
        }
        super.setPrimaryItem(container, position, any)
    }

    fun addTabPage(uri: String) {
        fileUris.add(uri)
        notifyDataSetChanged()
    }

    fun removeTabPage(position: Int) {
        if (fileUris.isNotEmpty() && position < fileUris.size) {
            fileUris.removeAt(position)
            lastRemovedUri = fileUris[position]
            lastRemovedIndex = position
            notifyDataSetChanged()
            lastRemovedUri = null
            lastRemovedIndex = null
        }
    }

    override fun getItemPosition(any: Any): Int {
        val uri = (any as ZipViewFragment).fileUri
        val position = fileUris.indexOf(uri)
        return if (position == -1) {
            PagerAdapter.POSITION_NONE
        } else {
            PagerAdapter.POSITION_UNCHANGED
        }
    }
}