package zalo.taitd.zipviewer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private var fileViewFragment:Fragment?=null

    override fun onClick(view: View) {
        when(view.id){
            R.id.chooseFileImgView -> dispatchChoosePictureIntent()
        }
    }

    private fun dispatchChoosePictureIntent() {
        val zipMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(Constants.ZIP_EXTENSION)
        val getIntent = Intent(Intent.ACTION_GET_CONTENT).apply { type = zipMimeType}

        val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply { type = zipMimeType }

        val chooserIntent = Intent.createChooser(getIntent, getString(R.string.label_choose_file_from)).apply {
            putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(pickIntent))
        }

        startActivityForResult(chooserIntent, Constants.CHOOSE_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (resultCode == Activity.RESULT_OK) {
                when (requestCode) {
                    Constants.CHOOSE_FILE -> {
                        intent?.data?.let{displayFile(it)}
                    }
                }
        } else {
            Log.d(TAG, "resultCode != Activity.RESULT_OK")
        }
    }

    private fun displayFile(uri: Uri){
        if(fileViewFragment==null){
            fileViewFragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        chooseFileImgView.setOnClickListener(this)
    }
}
