package zalo.taitd.zipviewer

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_input_url.*
import kotlinx.android.synthetic.main.fragment_input_url.view.*
import kotlinx.android.synthetic.main.fragment_input_url.view.fileNameTextView
import kotlinx.android.synthetic.main.fragment_input_url.view.fileSizeTextView
import java.io.InputStream
import java.net.HttpURLConnection
import java.nio.ByteBuffer
import java.nio.ByteOrder

@SuppressLint("CheckResult")
class InputUrlFragment(private val fm: FragmentManager) : DialogFragment(), View.OnClickListener {
    private val compositeDisposable = CompositeDisposable()
    private var zipInfo: ZipInfo? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_input_url, container, false)
        initView(view)
        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun initView(view: View) {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        view.apply {
            urlEditText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(editable: Editable) {
                    val url = editable.toString()
                    openButton.isEnabled = false
//                    hideWarning()

                    if (URLUtil.isValidUrl(url)) {
                        showLoadingAnimation()
                        getZipInfo(url)
                    } else if (url != "") {
                        showWarning(getString(R.string.invalid_url))
                    }
                }

                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    hideFilePreview()
                    hideWarning()
                    hideLoadingAnimation()
                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }
            })

            urlEditText.setOnFocusChangeListener { _, b ->
                if (b) {
                    urlEditText.post {
                        Utils.showKeyboard(context, urlEditText)
                    }
                } else {
                    Utils.hideKeyboard(context, this)
                }
            }
            urlEditText.requestFocus()

            openButton.isEnabled = false

            openButton.setOnClickListener(this@InputUrlFragment)
            cancelButton.setOnClickListener(this@InputUrlFragment)
            swapImgView.setOnClickListener(this@InputUrlFragment)
        }
    }

    private fun getZipInfo(url: String) {
        val singles = ArrayList<Single<ZipInfo>>()

        ZipViewerApplication.zipInfoCaches[url]?.let {
            singles.add(Single.just(it))
        }

        singles.add(
            Single.zip(
                Single.fromCallable { getZipCentralDirInfo(url) }
                    .subscribeOn(Schedulers.newThread()),
                Single.fromCallable { getFileSize(url) }
                    .subscribeOn(Schedulers.newThread()),
                BiFunction { pair: Pair<Int, Int>, fileSize: Long ->
                    ZipInfo(url, fileSize, pair.first, pair.second)
                }
            )
        )

        singles.forEach {
            it.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(GetZipInfoObserver())
        }
    }

    private fun getFileSize(fileUri: String): Long {
        val input: InputStream? = null
        var connection: HttpURLConnection? = null
        return try {
            connection = Utils.openConnection(fileUri)
            connection.contentLength.toLong()
        } catch (e: Throwable) {
            e.printStackTrace()
            Constants.ERROR.toLong()
        } finally {
            //close all resources
            input?.close()
            connection?.disconnect()
        }
    }

    private fun getZipCentralDirInfo(fileUri: String): Pair<Int, Int> {
        var input: InputStream? = null
        var connection: HttpURLConnection? = null
        try {
            connection =
                Utils.openConnection(fileUri, rangeEnd = Constants.MAX_EOCD_AND_COMMENT_SIZE)
            input = connection.inputStream

            val data = ByteArray(4096)
            val wrapped = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

            var count = input.read(data, 3, data.size - 3)
            var isEocdFound = false
            var i: Int
            var centralDirOffset = -1
            var centralDirSize = -1
            while (count != -1) {
                i = 0
                while (i < count + 3 - 21) {
                    if (data[i] == 0x50.toByte() && data[i + 1] == 0x4B.toByte() && data[i + 2] == 0x05.toByte() && data[i + 3] == 0x06.toByte()) {
                        centralDirOffset = wrapped.getInt(i + 16)
                        centralDirSize = wrapped.getInt(i + 12)
                        isEocdFound = true
                        break
                    }
                    i++
                }
                if (isEocdFound) {
                    break
                }
                for (j in 0 until 3) {
                    data[j] = if (count + j >= 0) data[count + j] else 0
                }
                count = input.read(data, 3, data.size - 3)
            }

            return Pair(centralDirOffset, centralDirSize)
        } catch (e: Throwable) {
            e.printStackTrace()
            return Pair(Constants.ERROR, Constants.ERROR)
        } finally {
            //close all resources
            input?.close()
            connection?.disconnect()
        }
    }

    private fun showFilePreview(zipInfo: ZipInfo) {
        view?.apply {
            fileExtensionImgView.visibility = View.VISIBLE
            fileNameTextView.visibility = View.VISIBLE
            fileSizeTextView.visibility = View.VISIBLE

            fileNameTextView.text =
                Utils.getFileName(zipInfo.url).let { if (it.endsWith(".zip")) it else "$it.zip" }

            fileSizeTextView.text = if (zipInfo.size != (-1).toLong()) String.format(
                "(%s)",
                Utils.getFormatFileSize(zipInfo.size)
            ) else getString(R.string.unknown_size)
        }
    }

    private fun hideFilePreview() {
        view?.apply {
            fileExtensionImgView.visibility = View.GONE
            fileNameTextView.visibility = View.GONE
            fileSizeTextView.visibility = View.GONE
        }
    }

    private fun showWarning(message: CharSequence) {
        view?.apply {
            warningTextView.text = message
            warningTextView.visibility = View.VISIBLE
        } ?: Log.e(TAG, "view is null")
    }

    private fun hideWarning() {
        view?.apply {
            warningTextView?.visibility = View.GONE
        } ?: Log.e(TAG, "view is null")
    }

    private fun showLoadingAnimation() {
        view?.apply {
            loadingAnimView.visibility = View.VISIBLE
            loadingAnimView.playAnimation()
        } ?: Log.e(TAG, "view is null")
    }

    private fun hideLoadingAnimation() {
        view?.apply {
            loadingAnimView.cancelAnimation()
            loadingAnimView.visibility = View.GONE
        } ?: Log.e(TAG, "view is null")
    }

    override fun onResume() {
        super.onResume()
        view?.urlEditText?.setText(zipInfo?.url)
    }

    override fun onPause() {
        super.onPause()
        zipInfo?.url = view?.urlEditText?.text.toString()
    }

    fun show() {
        show(fm, TAG)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.openButton -> openFileAndDismiss()
            R.id.cancelButton -> clearFocusAndDismiss()
            R.id.swapImgView -> {
                val linkSlow =
                    "https://firebasestorage.googleapis.com/v0/b/zalo-4c204.appspot.com/o/test_db-master.zip?alt=media&token=dae8f14a-feb3-41eb-8538-742cedfe6eae"
                val linkNoSize =
                    "https://drive.google.com/uc?export=download&id=10ct392b5iPrfvN8hrXreWnQKviMAuGV0"
                val linkFake = "https://"
                val linkQuick1 =
                    "https://drive.google.com/uc?export=download&id=1Zson--ESF9M2AhsN7n1AQoGeF06NmiFK"
                val linkQuick2 =
                    "https://drive.google.com/uc?export=download&id=1xvOM_us_rzXMGKbmVlCbwdh0XSaETy5c"
                val linkQuick3 =
                    "https://drive.google.com/uc?export=download&id=18f57kX1rBLL-yn661sLvcQxDVn5oJcyG"
                val linkNotZip =
                    "https://drive.google.com/uc?export=download&id=15LFS6C1dJN2BdxjptYZjWJCGQIU89PEK"
                val linkZalo =
                    "https://zalo-filegroup-bf1.zdn.vn/d7c39a788ff463aa3ae5/99484683834119407"

                urlEditText.setText(
                    when (urlEditText.text.toString()) {
                        linkQuick1 -> linkQuick2
                        linkQuick2 -> linkQuick3
                        linkQuick3 -> linkFake
                        linkFake -> linkNoSize
                        linkNoSize -> linkSlow
                        linkSlow -> linkNotZip
                        linkNotZip -> linkZalo
                        else -> linkQuick1
                    }
                )
            }
        }
    }

    private fun clearFocusAndDismiss() {
        view?.urlEditText?.clearFocus()
        dismiss()
    }

    private fun openFileAndDismiss() {
        view?.apply {
            (activity as MainActivity).addTabIfNotAdded(
                zipInfo!!
            )
            clearFocusAndDismiss()
        }
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }

    inner class GetZipInfoObserver : SingleObserver<ZipInfo> {
        override fun onSuccess(zipInfo: ZipInfo) {
            if (zipInfo.url == urlEditText.text.toString()) {
                hideLoadingAnimation()
                hideWarning()
                hideFilePreview()
                if (zipInfo.size == Constants.ERROR.toLong() || zipInfo.centralDirOffset == Constants.ERROR || zipInfo.centralDirSize == Constants.ERROR) {
                    showWarning(getString(R.string.cannot_resolve_host))
                } else {
                    if (zipInfo.centralDirOffset != -1 || zipInfo.centralDirSize != -1) {
                        ZipViewerApplication.zipInfoCaches[zipInfo.url] = zipInfo
                        this@InputUrlFragment.zipInfo = zipInfo
                        openButton.isEnabled = true
                        showFilePreview(zipInfo)
                    } else {
                        showWarning(getString(R.string.not_a_zip_file))
                    }
                }
            }
        }

        override fun onSubscribe(d: Disposable) {
            compositeDisposable.add(d)
        }

        override fun onError(e: Throwable) {
            hideLoadingAnimation()
            showWarning(getString(R.string.cannot_resolve_host))
            e.printStackTrace()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        zipInfo = null
        super.onDismiss(dialog)
    }
}