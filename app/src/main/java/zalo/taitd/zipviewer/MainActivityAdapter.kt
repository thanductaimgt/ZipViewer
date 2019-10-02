package zalo.taitd.zipviewer

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.PagerAdapter


class MainActivityAdapter(fm: FragmentManager) :
    FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    val zipInfos = ArrayList<ZipInfo>()
    var curFragment: Fragment? = null

    override fun getItem(position: Int): Fragment {
        return ZipViewFragment(zipInfos[position])
    }

    override fun getCount(): Int {
        return zipInfos.size
    }

    override fun setPrimaryItem(container: ViewGroup, position: Int, any: Any) {
        if (curFragment !== any) {
            curFragment = any as Fragment
        }
        super.setPrimaryItem(container, position, any)
    }

    fun addTabPage(zipInfo:ZipInfo) {
        zipInfos.add(zipInfo)
        notifyDataSetChanged()
    }

    fun removeTabPage(position: Int) {
        if (zipInfos.isNotEmpty() && position < zipInfos.size) {
            zipInfos.removeAt(position)
            notifyDataSetChanged()
        }
    }

    override fun getItemPosition(any: Any): Int {
        return PagerAdapter.POSITION_NONE
    }
}