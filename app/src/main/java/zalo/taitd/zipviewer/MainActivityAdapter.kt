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
    val fileUris = ArrayList<Uri>()
    var curFragment: Fragment? = null

    override fun getItem(position: Int): Fragment {
        return ZipViewFragment(fileUris[position])
    }

    override fun getCount(): Int {
        return fileUris.size
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return Utils.getFileName(context, fileUris[position])
    }

    override fun setPrimaryItem(container: ViewGroup, position: Int, any: Any) {
        if (curFragment !== any) {
            curFragment = any as Fragment
        }
        super.setPrimaryItem(container, position, any)
    }

    fun addTabPage(uri: Uri) {
        fileUris.add(uri)
        notifyDataSetChanged()
    }

    fun removeTabPage(position: Int) {
        if (fileUris.isNotEmpty() && position < fileUris.size) {
            fileUris.removeAt(position)
            notifyDataSetChanged()
        }
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        return (super.instantiateItem(container, position) as ZipViewFragment)
    }

    override fun getItemPosition(any: Any): Int {
        return if (fileUris.contains((any as ZipViewFragment).fileUri)) {
            PagerAdapter.POSITION_UNCHANGED
        } else {
            PagerAdapter.POSITION_NONE
        }
    }
}