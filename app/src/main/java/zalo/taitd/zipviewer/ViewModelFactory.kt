package zalo.taitd.zipviewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

class ViewModelFactory private constructor() : ViewModelProvider.Factory {
    private lateinit var zipFile: ZipFile

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            ZipViewFragmentViewModel::class.java -> ZipViewFragmentViewModel(zipFile) as T
            else -> modelClass.newInstance()
        }
    }

    companion object {
        private var instance: ViewModelFactory? = null

        fun getInstance(zipFile:ZipFile? = null): ViewModelFactory {
            if (instance == null) {
                synchronized(ViewModelFactory) {
                    if (instance == null) {
                        instance = ViewModelFactory()
                    }
                }
            }

            if (zipFile != null) {
                instance!!.zipFile = zipFile
            }
            return instance!!
        }
    }
}