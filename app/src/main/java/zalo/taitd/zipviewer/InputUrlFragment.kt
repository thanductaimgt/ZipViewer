package zalo.taitd.zipviewer

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

class InputUrlFragment(private val fm: FragmentManager) : DialogFragment(), View.OnClickListener {
    private val compositeDisposable = CompositeDisposable()
    private var fileUri: String? = null
    private var centralDirOffset = -1
    private var centralDirSize = -1

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
            urlEditText.setText("")

            urlEditText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(editable: Editable) {
                    val url = editable.toString()
                    openButton.isEnabled = false
                    hideWarning()

                    when {
                        URLUtil.isValidUrl(url) -> {
                            showLoadingAnimation()

                            Single.zip(
                                Single.fromCallable {
                                    Utils.getZipCentralDirInfo(url)
                                }
                                    .subscribeOn(Schedulers.newThread()),
                                Single.fromCallable { Utils.getFileSize(url) }
                                    .subscribeOn(Schedulers.newThread()),
                                BiFunction { pair: Pair<Int, Int>, fileSize: Long ->
                                    Triple(fileSize, pair.first, pair.second)
                                }
                            ).subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeWith(FileInfoObserver(url))
                        }
                        url != "" -> showWarning(getString(R.string.invalid_url))
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

    private fun showFilePreview(fileUri: String, fileSize: Long) {
        view?.apply {
            fileExtensionImgView.visibility = View.VISIBLE
            fileNameTextView.visibility = View.VISIBLE
            fileSizeTextView.visibility = View.VISIBLE

            fileNameTextView.text = Utils.parseFileName(fileUri)

            fileSizeTextView.text = String.format(
                "(%s)",
                Utils.getFormatFileSize(fileSize)
            )
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
        view?.urlEditText?.setText(fileUri)
    }

    override fun onPause() {
        super.onPause()
        fileUri = view?.urlEditText?.text.toString()
    }

    fun show() {
        show(fm, TAG)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.openButton -> openFileAndDismiss()
            R.id.cancelButton -> clearFocusAndDismiss()
            R.id.swapImgView -> {
                val linkWithSize =
                    "https://firebasestorage.googleapis.com/v0/b/zalo-4c204.appspot.com/o/test_db-master.zip?alt=media&token=dae8f14a-feb3-41eb-8538-742cedfe6eae"
                val linkNoSize =
                    "https://drive.google.com/uc?export=download&id=10ct392b5iPrfvN8hrXreWnQKviMAuGV0"
                val linkFake = "https://"
                val linkQuick =
                    "https://drive.google.com/uc?export=download&id=1Zson--ESF9M2AhsN7n1AQoGeF06NmiFK"
                urlEditText.setText(
                    when (urlEditText.text.toString()) {
                        linkWithSize -> linkNoSize
                        linkNoSize -> linkFake
                        linkFake -> linkQuick
                        else -> linkWithSize
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
            (activity as MainActivity).addTab(
                Triple(urlEditText.text.toString(), centralDirOffset, centralDirSize)
            )
            clearFocusAndDismiss()
        }
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }

    inner class FileInfoObserver(private val url: String) : SingleObserver<Triple<Long, Int, Int>> {
        override fun onSuccess(triple: Triple<Long, Int, Int>) {
            val fileSize = triple.first
            val centralDirOffset = triple.second
            val centralDirSize = triple.third

            if (url == urlEditText.text.toString()) {
                hideLoadingAnimation()
                if (fileSize == Constants.ERROR.toLong() || centralDirOffset == Constants.ERROR || centralDirSize == Constants.ERROR) {
                    showWarning(getString(R.string.cannot_resolve_host))
                } else {
                    if (centralDirOffset != -1 || centralDirSize != -1) {
                        if (fileSize != (-1).toLong()) {
                            this@InputUrlFragment.fileUri = url
                            this@InputUrlFragment.centralDirOffset =
                                centralDirOffset
                            this@InputUrlFragment.centralDirSize =
                                centralDirSize
                            openButton.isEnabled = true
                            showFilePreview(url, fileSize)
                        } else {
                            showWarning(getString(R.string.not_support_file_unknown_size))
                        }
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
        fileUri = null
    }
}