package zalo.taitd.zipviewer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var adapter: MainActivityAdapter
    private var doubleBackToExitPressedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initView()
    }

    private fun initView() {
        adapter = MainActivityAdapter(this, supportFragmentManager)
        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = 8
        tabLayout.setupWithViewPager(viewPager, false)
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                tab.customView?.background = ContextCompat.getDrawable(
                    this@MainActivity,
                    R.drawable.shape_round_top_dark_strong_gray
                )
            }

            override fun onTabSelected(tab: TabLayout.Tab) {
                tab.customView?.background = ContextCompat.getDrawable(
                    this@MainActivity,
                    R.drawable.shape_round_top_dark_weak_gray
                )
            }
        })

        chooseFileImgView.setOnClickListener(this)
        addFileImgView.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.chooseFileImgView -> dispatchChoosePictureIntent()
            R.id.addFileImgView -> dispatchChoosePictureIntent()
        }
    }

    private fun dispatchChoosePictureIntent() {
        val zipMimeType =
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(Constants.ZIP_EXTENSION)
        val getIntent = Intent(Intent.ACTION_GET_CONTENT).apply { type = zipMimeType }

        val pickIntent =
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                type = zipMimeType
            }

        val chooserIntent =
            Intent.createChooser(getIntent, getString(R.string.label_choose_file_from)).apply {
                putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(pickIntent))
            }

        startActivityForResult(chooserIntent, Constants.CHOOSE_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                Constants.CHOOSE_FILE -> {
                    intent?.data?.let {
                        adapter.fileUris.forEachIndexed { index, uri ->
                            if(uri == it){
                                viewPager.currentItem = index
                                return@let
                            }
                        }
                        addTab(it)
                    }
                }
            }
        } else {
            Log.d(TAG, "resultCode != Activity.RESULT_OK")
        }
    }

    private fun addTab(uri: Uri) {
        tabLayout.addTab(tabLayout.newTab().setCustomView(getTabView(uri)))
        adapter.addTabPage(uri)
        viewPager.currentItem = viewPager.childCount-1

        showAddButton()
    }

    private fun removeTab(position: Int) {
        if (tabLayout.tabCount > 0 && position < tabLayout.tabCount) {
            tabLayout.removeTabAt(position)
            adapter.removeTabPage(position)

            if(adapter.fileUris.isEmpty()){
                hideAddButton()
            }
        }
    }

    private fun showAddButton() {
        if (addFileImgView.visibility == View.GONE) {
            addFileImgView.visibility = View.VISIBLE
        }
    }

    private fun hideAddButton() {
        if (addFileImgView.visibility == View.VISIBLE) {
            addFileImgView.visibility = View.GONE
        }
    }

    private fun getTabView(uri: Uri): View {
        // Given you have a custom layout in `res/layout/custom_tab.xml` with a TextView and ImageView
        val rootView = LayoutInflater.from(this).inflate(R.layout.custom_tab, null)
        val titleTextView = rootView.findViewById(R.id.titleTextView) as TextView
        titleTextView.text = Utils.getFileName(this, uri)

        val closeTabImgView = rootView.findViewById(R.id.closeTabImgView) as ImageView
        closeTabImgView.setOnClickListener{
            var indexToRemove:Int?=null
            adapter.fileUris.forEachIndexed { index, fileUri ->
                if(uri == fileUri){
                    indexToRemove = index
                    return@forEachIndexed
                }
            }
            indexToRemove?.let { removeTab(it) }
        }
        return rootView
    }

    override fun onBackPressed() {
        val fragment = adapter.curFragment as ZipViewFragment?
        //supportFragmentManager.findFragmentByTag("android:switcher:" + R.id.viewPager + ":" + viewPager.currentItem) as ZipViewFragment
        if (fragment?.curZipNode?.parentNode != null) {
            fragment.setCurrentNode(fragment.curZipNode!!.parentNode!!)
        } else {
            if (doubleBackToExitPressedOnce) {
                super.onBackPressed()
            }
            doubleBackToExitPressedOnce = true
            Toast.makeText(this, getString(R.string.press_again_to_exit), Toast.LENGTH_SHORT).show()
            Handler().postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
        }
    }
}
