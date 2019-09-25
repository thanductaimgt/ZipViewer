package zalo.taitd.zipviewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.util.zip.ZipFile

class ViewModelFactory private constructor() : ViewModelProvider.Factory {
    private lateinit var fileUri: String

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            ZipViewFragmentViewModel::class.java -> ZipViewFragmentViewModel(fileUri) as T
            else -> modelClass.newInstance()
        }
    }

    companion object {
        private var instance: ViewModelFactory? = null

        fun getInstance(fileUri:String? = null): ViewModelFactory {
            if (instance == null) {
                synchronized(ViewModelFactory) {
                    if (instance == null) {
                        instance = ViewModelFactory()
                    }
                }
            }

            fileUri?.let { instance!!.fileUri =it }
            return instance!!
        }
    }
}