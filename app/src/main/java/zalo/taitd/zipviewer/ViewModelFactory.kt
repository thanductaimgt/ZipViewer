package zalo.taitd.zipviewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ViewModelFactory private constructor() : ViewModelProvider.Factory {
    private lateinit var zipInfo: ZipInfo

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            ZipViewFragmentViewModel::class.java -> ZipViewFragmentViewModel(zipInfo) as T
            else -> modelClass.newInstance()
        }
    }

    companion object {
        private var instance: ViewModelFactory? = null

        fun getInstance(zipInfo:ZipInfo?=null): ViewModelFactory {
            if (instance == null) {
                synchronized(ViewModelFactory) {
                    if (instance == null) {
                        instance = ViewModelFactory()
                    }
                }
            }

            zipInfo?.let { instance!!.zipInfo =it }
            return instance!!
        }
    }
}