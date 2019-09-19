package zalo.taitd.zipviewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.util.zip.ZipInputStream

class ViewModelFactory private constructor() : ViewModelProvider.Factory {
    private lateinit var zipInputStream: ZipInputStream

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            ZipViewFragmentViewModel::class.java -> ZipViewFragmentViewModel(zipInputStream) as T
            else -> modelClass.newInstance()
        }
    }

    companion object {
        private var instance: ViewModelFactory? = null

        fun getInstance(zipInputStream:ZipInputStream? = null): ViewModelFactory {
            if (instance == null) {
                synchronized(ViewModelFactory) {
                    if (instance == null) {
                        instance = ViewModelFactory()
                    }
                }
            }

            if (zipInputStream != null) {
                instance!!.zipInputStream = zipInputStream
            }
            return instance!!
        }
    }
}