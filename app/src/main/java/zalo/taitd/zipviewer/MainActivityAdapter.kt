package zalo.taitd.zipviewer

import android.content.Context
import android.net.Uri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import android.view.ViewGroup



class MainActivityAdapter(private val context: Context, fm:FragmentManager) :FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT){
    val fileUris = ArrayList<Uri>()
    var curFragment:Fragment?=null

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
}