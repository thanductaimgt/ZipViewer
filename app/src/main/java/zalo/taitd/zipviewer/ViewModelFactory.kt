package zalo.taitd.zipviewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ViewModelFactory private constructor() : ViewModelProvider.Factory {
    private lateinit var fileInfo: Triple<String, Int, Int>

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            ZipViewFragmentViewModel::class.java -> ZipViewFragmentViewModel(fileInfo) as T
            else -> modelClass.newInstance()
        }
    }

    companion object {
        private var instance: ViewModelFactory? = null

        fun getInstance(fileInfo:Triple<String, Int, Int>?=null): ViewModelFactory {
            if (instance == null) {
                synchronized(ViewModelFactory) {
                    if (instance == null) {
                        instance = ViewModelFactory()
                    }
                }
            }

            fileInfo?.let { instance!!.fileInfo =it }
            return instance!!
        }
    }
}