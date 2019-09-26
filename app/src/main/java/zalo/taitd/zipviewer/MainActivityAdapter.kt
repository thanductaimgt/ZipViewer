package zalo.taitd.zipviewer

import android.content.Context
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.PagerAdapter


class MainActivityAdapter(private val context: Context, fm: FragmentManager) :
    FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    val filesInfo = ArrayList<Triple<String, Int, Int>>()
    var curFragment: Fragment? = null
    var lastRemovedUri: String? = null
    var lastRemovedIndex: Int? = null

    override fun getItem(position: Int): Fragment {
        return ZipViewFragment(filesInfo[position])
    }

    override fun getCount(): Int {
        return filesInfo.size
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return Utils.parseFileName(filesInfo[position].first)
    }

    override fun setPrimaryItem(container: ViewGroup, position: Int, any: Any) {
        if (curFragment !== any) {
            curFragment = any as Fragment
        }
        super.setPrimaryItem(container, position, any)
    }

    fun addTabPage(fileInfo:Triple<String, Int, Int>) {
        filesInfo.add(fileInfo)
        notifyDataSetChanged()
    }

    fun removeTabPage(position: Int) {
        if (filesInfo.isNotEmpty() && position < filesInfo.size) {
            filesInfo.removeAt(position)
//            lastRemovedUri = filesInfo[position]
//            lastRemovedIndex = position
            notifyDataSetChanged()
//            lastRemovedUri = null
//            lastRemovedIndex = null
        }
    }

    override fun getItemPosition(any: Any): Int {
//        val uri = (any as ZipViewFragment).fileUri
//        val position = filesInfo.indexOf(uri)
//        return if (position == -1) {
//            PagerAdapter.POSITION_NONE
//        } else {
//            PagerAdapter.POSITION_UNCHANGED
//        }
        return PagerAdapter.POSITION_NONE
    }
}