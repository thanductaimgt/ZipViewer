package zalo.taitd.zipviewer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var adapter: MainActivityAdapter
    private var doubleBackToExitPressedOnce = false

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
                    intent?.data?.let { displayFile(it) }
                }
            }
        } else {
            Log.d(TAG, "resultCode != Activity.RESULT_OK")
        }
    }

    private fun displayFile(uri: Uri) {
        tabLayout.addTab(tabLayout.newTab())
        adapter.fileUris.add(uri)
        // must call this when add tab at runtime
        adapter.notifyDataSetChanged()
        viewPager.currentItem = adapter.fileUris.lastIndex

        if (addFileImgView.visibility == View.GONE) {
            addFileImgView.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initView()
    }

    private fun initView() {
        adapter = MainActivityAdapter(this, supportFragmentManager)
        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = 8
        tabLayout.setupWithViewPager(viewPager)

        chooseFileImgView.setOnClickListener(this)
        addFileImgView.setOnClickListener(this)
    }

    override fun onBackPressed() {
        val fragment = adapter.curFragment as ZipViewFragment?
            //supportFragmentManager.findFragmentByTag("android:switcher:" + R.id.viewPager + ":" + viewPager.currentItem) as ZipViewFragment
        if (fragment?.curZipNode?.parentNode != null) {
            fragment.setCurrentNode(fragment.curZipNode!!.parentNode!!)
        } else {
            if(doubleBackToExitPressedOnce){
                super.onBackPressed()
            }
            doubleBackToExitPressedOnce = true
            Toast.makeText(this, getString(R.string.press_again_to_exit), Toast.LENGTH_SHORT).show()
            Handler().postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
        }
    }
}
