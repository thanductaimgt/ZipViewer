package zalo.taitd.zipviewer

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.PagerAdapter


class MainActivityAdapter(fm: FragmentManager) :
    FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    val filesInfo = ArrayList<Triple<String, Int, Int>>()
    var curFragment: Fragment? = null

    override fun getItem(position: Int): Fragment {
        return ZipViewFragment(filesInfo[position])
    }

    override fun getCount(): Int {
        return filesInfo.size
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
            notifyDataSetChanged()
        }
    }

    override fun getItemPosition(any: Any): Int {
        return PagerAdapter.POSITION_NONE
    }
}