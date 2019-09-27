package zalo.taitd.zipviewer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_sheet_dialog.view.*


class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var adapter: MainActivityAdapter
    private var doubleBackToExitPressedOnce = false
    private lateinit var bottomSheetDialog: BottomSheetDialog
    private lateinit var inputUrlFragment: InputUrlFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initView()
    }

    private fun initView() {
        adapter = MainActivityAdapter(supportFragmentManager)
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
        chooseFileTextView.setOnClickListener(this)

        initBottomSheet()

        inputUrlFragment = InputUrlFragment(supportFragmentManager)
    }

    private fun initBottomSheet() {
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_dialog, null)

        bottomSheetDialog = BottomSheetDialog(this)

        // Fix BottomSheetDialog not showing after getting hidden when the user drags it down
        bottomSheetDialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<FrameLayout>(R.id.design_bottom_sheet)
            BottomSheetBehavior.from(bottomSheet!!).apply {
                skipCollapsed = true
            }
        }
        bottomSheetDialog.setContentView(sheetView)

        sheetView.openFromLocalTextView.setOnClickListener(this)
        sheetView.openFromUrlTextView.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        when {
            arrayOf(R.id.chooseFileImgView, R.id.addFileImgView, R.id.chooseFileTextView).contains(
                view.id
            ) -> bottomSheetDialog.show()
            view.id == R.id.openFromLocalTextView -> dispatchChoosePictureIntent()
            view.id == R.id.openFromUrlTextView -> inputUrlFragment.show()
        }
    }

    private fun dispatchChoosePictureIntent() {
        Toast.makeText(this, "Not implemented =)", Toast.LENGTH_SHORT).show()
//        val zipMimeType =
//            MimeTypeMap.getSingleton().getMimeTypeFromExtension(Constants.ZIP_EXTENSION)
//        val getIntent = Intent(Intent.ACTION_GET_CONTENT).apply { type = zipMimeType }
//
//        val pickIntent =
//            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
//                type = zipMimeType
//            }
//
//        val chooserIntent =
//            Intent.createChooser(getIntent, getString(R.string.label_choose_file_from)).apply {
//                putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(pickIntent))
//            }
//
//        startActivityForResult(chooserIntent, Constants.CHOOSE_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                Constants.CHOOSE_FILE -> {
                    intent?.data?.let {
                        addTabIfNotAdded(Triple(it.toString(), -1, -1))
                    }
                }
            }
        } else {
            Log.d(TAG, "resultCode != Activity.RESULT_OK")
        }
    }

    fun addTabIfNotAdded(fileInfo: Triple<String, Int, Int>) {
        var isTabAdded = false
        adapter.filesInfo.forEachIndexed { index, it ->
            if (fileInfo.first == it.first) {
                viewPager.currentItem = index
                isTabAdded = true
                return@forEachIndexed
            }
        }

        bottomSheetDialog.dismiss()

        if(!isTabAdded){
            val fileName = fileInfo.first
            tabLayout.addTab(tabLayout.newTab().setCustomView(getTabView(fileName)))
            adapter.addTabPage(fileInfo)
            viewPager.currentItem = viewPager.childCount - 1

            showAddButton()
        }
    }

    private fun removeTab(position: Int) {
        if (tabLayout.tabCount > 0 && position < tabLayout.tabCount) {
            tabLayout.removeTabAt(position)
            adapter.removeTabPage(position)

            if (adapter.filesInfo.isEmpty()) {
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

    private fun getTabView(uri: String): View {
        val rootView = LayoutInflater.from(this).inflate(R.layout.item_tab, null)
        val titleTextView = rootView.findViewById(R.id.titleTextView) as TextView
        titleTextView.text = Utils.parseFileName(uri).let { if (it.endsWith(".zip")) it else "$it.zip" }

        val closeTabImgView = rootView.findViewById(R.id.closeTabImgView) as ImageView
        closeTabImgView.setOnClickListener {
            var indexToRemove: Int? = null
            adapter.filesInfo.forEachIndexed { index, fileInfo ->
                if (uri == fileInfo.first) {
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
