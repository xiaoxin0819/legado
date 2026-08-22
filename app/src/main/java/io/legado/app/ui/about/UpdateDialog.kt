package io.legado.app.ui.about

import android.app.DownloadManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.format.Formatter
import android.view.View
import android.view.ViewGroup
import android.view.MenuItem
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.databinding.DialogUpdateBinding
import io.legado.app.help.update.AppUpdate
import io.legado.app.lib.theme.primaryColor
import io.legado.app.utils.setLayout
import io.legado.app.utils.openFileUri
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.glide.GlideImagesPlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val APK_MIME_TYPE = "application/vnd.android.package-archive"

class UpdateDialog() : BaseDialogFragment(R.layout.dialog_update) {

    constructor(updateInfo: AppUpdate.UpdateInfo) : this() {
        arguments = Bundle().apply {
            putString("newVersion", updateInfo.tagName)
            putString("updateBody", updateInfo.updateLog)
            putString("url", updateInfo.downloadUrl)
            putString("name", updateInfo.fileName)
        }
    }

    val binding by viewBinding(DialogUpdateBinding::bind)
    private var downloadId: Long? = null
    private var downloadJob: Job? = null
    private var downloadMenuItem: MenuItem? = null

    override fun onStart() {
        super.onStart()
        setLayout(0.9f, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.toolBar.title = arguments?.getString("newVersion")
        val updateBody = arguments?.getString("updateBody")
        if (updateBody == null) {
            toastOnUi("没有数据")
            dismiss()
            return
        }
        binding.textView.post {
            Markwon.builder(requireContext())
                .usePlugin(GlideImagesPlugin.create(requireContext()))
                .usePlugin(HtmlPlugin.create())
                .usePlugin(TablePlugin.create(requireContext()))
                .build()
                .setMarkdown(binding.textView, updateBody)
        }
        binding.toolBar.inflateMenu(R.menu.app_update)
        downloadMenuItem = binding.toolBar.menu.findItem(R.id.menu_download)
        binding.toolBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_download -> {
                    if (downloadId != null) {
                        cancelUpdateDownload()
                    } else {
                        val url = arguments?.getString("url")
                        val name = arguments?.getString("name")
                        if (url != null && name != null) {
                            startUpdateDownload(url, name)
                        }
                    }
                }
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun startUpdateDownload(url: String, fileName: String) {
        val safeFileName = java.io.File(fileName).name
        runCatching {
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle(safeFileName)
                setDescription(getString(R.string.update_download_description))
                setMimeType(APK_MIME_TYPE)
                setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                setDestinationInExternalFilesDir(
                    requireContext(),
                    Environment.DIRECTORY_DOWNLOADS,
                    safeFileName
                )
            }
            downloadId = requireContext()
                .getSystemService(DownloadManager::class.java)
                .enqueue(request)
            binding.downloadProgressPanel.visibility = View.VISIBLE
            binding.downloadProgress.isIndeterminate = true
            binding.downloadProgressText.text = getString(R.string.update_download_preparing)
            downloadMenuItem?.setTitle(R.string.download_cancel)
            toastOnUi(R.string.download_start)
            observeUpdateDownload()
        }.onFailure {
            resetDownloadState()
            toastOnUi(getString(R.string.download_error) + "," + (it.localizedMessage ?: ""))
        }
    }

    private fun observeUpdateDownload() {
        val id = downloadId ?: return
        downloadJob?.cancel()
        downloadJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive && downloadId == id) {
                val state = withContext(Dispatchers.IO) {
                    queryUpdateDownload(id)
                }
                if (state == null) {
                    resetDownloadState()
                    toastOnUi(R.string.download_error)
                    break
                }
                when (state.status) {
                    DownloadManager.STATUS_PENDING,
                    DownloadManager.STATUS_PAUSED -> {
                        updateDownloadProgress(state.bytes, state.total, indeterminate = true)
                    }

                    DownloadManager.STATUS_RUNNING -> {
                        updateDownloadProgress(state.bytes, state.total, indeterminate = false)
                    }

                    DownloadManager.STATUS_SUCCESSFUL -> {
                        updateDownloadProgress(state.bytes, state.total, indeterminate = false)
                        downloadJob?.cancel()
                        downloadId = null
                        downloadMenuItem?.apply {
                            isEnabled = false
                            setTitle(R.string.download_success)
                        }
                        val uri = requireContext()
                            .getSystemService(DownloadManager::class.java)
                            .getUriForDownloadedFile(id)
                        if (uri == null) {
                            toastOnUi(R.string.download_error)
                        } else {
                            toastOnUi(R.string.download_success)
                            requireContext().openFileUri(uri, APK_MIME_TYPE)
                        }
                        break
                    }

                    DownloadManager.STATUS_FAILED -> {
                        resetDownloadState()
                        toastOnUi(R.string.download_error)
                        break
                    }
                }
                delay(500)
            }
        }
    }

    private fun queryUpdateDownload(id: Long): UpdateDownloadState? {
        val manager = requireContext().getSystemService(DownloadManager::class.java)
        val query = DownloadManager.Query().setFilterById(id)
        return manager.query(query).use { cursor ->
            if (!cursor.moveToFirst()) return null
            UpdateDownloadState(
                status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),
                bytes = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                ),
                total = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                )
            )
        }
    }

    private fun updateDownloadProgress(bytes: Long, total: Long, indeterminate: Boolean) {
        binding.downloadProgress.isIndeterminate = indeterminate || total <= 0
        val progress = if (total > 0) {
            (bytes * 100 / total).toInt().coerceIn(0, 100)
        } else {
            0
        }
        binding.downloadProgress.progress = progress
        binding.downloadProgressText.text = if (total > 0) {
            getString(
                R.string.update_download_progress,
                progress,
                Formatter.formatFileSize(requireContext(), bytes),
                Formatter.formatFileSize(requireContext(), total)
            )
        } else {
            getString(R.string.update_download_preparing)
        }
    }

    private fun cancelUpdateDownload() {
        downloadId?.let {
            requireContext().getSystemService(DownloadManager::class.java).remove(it)
        }
        downloadJob?.cancel()
        resetDownloadState()
        toastOnUi(R.string.download_cancel)
    }

    private fun resetDownloadState() {
        downloadId = null
        downloadJob?.cancel()
        downloadJob = null
        binding.downloadProgressPanel.visibility = View.GONE
        downloadMenuItem?.apply {
            isEnabled = true
            setTitle(R.string.action_download)
        }
    }

    override fun onDestroyView() {
        downloadJob?.cancel()
        super.onDestroyView()
    }

    private data class UpdateDownloadState(
        val status: Int,
        val bytes: Long,
        val total: Long
    )

}
