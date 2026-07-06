package io.legado.app.ui.storage

import android.app.Application
import android.content.Context
import android.webkit.WebStorage
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.data.AppDatabase
import io.legado.app.data.appDb
import io.legado.app.help.book.BookHelp
import io.legado.app.help.storage.ResetDefaultState
import io.legado.app.utils.ConvertUtils
import io.legado.app.utils.FileUtils
import io.legado.app.utils.externalCache
import io.legado.app.utils.externalFiles
import io.legado.app.utils.getFile
import java.io.File

class StorageSlimViewModel(application: Application) : BaseViewModel(application) {

    fun scan(onSuccess: (StorageSlimState) -> Unit) {
        execute {
            scanStorage()
        }.onSuccess {
            onSuccess(it)
        }
    }

    fun clean(type: StorageSlimType, onSuccess: (Long) -> Unit, onError: (Throwable) -> Unit) {
        execute {
            val before = itemSize(type)
            when (type) {
                StorageSlimType.BOOK_CACHE -> clearShelfBookCache()
                StorageSlimType.INVALID_BOOK_CACHE -> BookHelp.clearInvalidCache()
                StorageSlimType.APP_CACHE -> clearAppCache()
                StorageSlimType.WEBVIEW -> clearWebViewData()
                StorageSlimType.LOGS -> FileUtils.delete(context.externalCache.getFile("logs"), true)
                StorageSlimType.TEMP_BACKUP -> clearTempBackup()
                StorageSlimType.RULE_DATA -> FileUtils.delete(context.externalFiles.getFile("ruleData"), true)
                StorageSlimType.LOCAL_COVERS -> FileUtils.delete(context.externalFiles.getFile("covers"), true)
                StorageSlimType.DATABASE -> ResetDefaultState.markDatabaseSlim(context)
                StorageSlimType.RESET_DEFAULT -> ResetDefaultState.mark(context)
            }
            val after = itemSize(type)
            (before - after).coerceAtLeast(0L)
        }.onSuccess {
            onSuccess(it)
        }.onError {
            onError(it)
        }
    }

    private fun scanStorage(): StorageSlimState {
        val items = buildList {
            val bookCache = context.externalFiles.getFile("book_cache")
            val shelfBookCacheSize = shelfBookCacheSize(bookCache)
            add(
                StorageSlimItem(
                    StorageSlimType.BOOK_CACHE,
                    context.getString(R.string.storage_slim_book_cache),
                    context.getString(R.string.storage_slim_book_cache_summary),
                    shelfBookCacheSize,
                    shelfBookCacheSize > 0
                )
            )
            val invalidBookCacheSize = invalidBookCacheSize(bookCache)
            add(
                StorageSlimItem(
                    StorageSlimType.INVALID_BOOK_CACHE,
                    context.getString(R.string.storage_slim_invalid_book_cache),
                    context.getString(R.string.storage_slim_invalid_book_cache_summary),
                    invalidBookCacheSize,
                    invalidBookCacheSize > 0
                )
            )
            val appCache = context.cacheDir
            val externalCache = context.externalCache
            add(
                StorageSlimItem(
                    StorageSlimType.APP_CACHE,
                    context.getString(R.string.storage_slim_app_cache),
                    context.getString(R.string.storage_slim_app_cache_summary),
                    appCache.safeSize() + externalCache.safeSize() - externalCache.getFile("logs").safeSize(),
                    appCache.exists() || externalCache.exists()
                )
            )
            val webViewSize = webViewDirs().sumOf { it.safeSize() }
            add(
                StorageSlimItem(
                    StorageSlimType.WEBVIEW,
                    context.getString(R.string.storage_slim_webview),
                    context.getString(R.string.storage_slim_webview_summary),
                    webViewSize,
                    webViewSize > 0
                )
            )
            val logs = context.externalCache.getFile("logs")
            add(
                StorageSlimItem(
                    StorageSlimType.LOGS,
                    context.getString(R.string.storage_slim_logs),
                    context.getString(R.string.storage_slim_logs_summary),
                    logs.safeSize(),
                    logs.exists()
                )
            )
            add(
                StorageSlimItem(
                    StorageSlimType.TEMP_BACKUP,
                    context.getString(R.string.storage_slim_temp_backup),
                    context.getString(R.string.storage_slim_temp_backup_summary),
                    tempBackupSize(),
                    true
                )
            )
            val ruleData = context.externalFiles.getFile("ruleData")
            add(
                StorageSlimItem(
                    StorageSlimType.RULE_DATA,
                    context.getString(R.string.storage_slim_rule_data),
                    context.getString(R.string.storage_slim_rule_data_summary),
                    ruleData.safeSize(),
                    ruleData.exists()
                )
            )
            val covers = context.externalFiles.getFile("covers")
            add(
                StorageSlimItem(
                    StorageSlimType.LOCAL_COVERS,
                    context.getString(R.string.storage_slim_local_covers),
                    context.getString(R.string.storage_slim_local_covers_summary),
                    covers.safeSize(),
                    covers.exists()
                )
            )
            add(
                StorageSlimItem(
                    StorageSlimType.DATABASE,
                    context.getString(R.string.storage_slim_database),
                    context.getString(R.string.storage_slim_database_quick_summary),
                    databaseQuickSize(),
                    databaseQuickSize() > 0
                )
            )
            add(
                StorageSlimItem(
                    StorageSlimType.RESET_DEFAULT,
                    context.getString(R.string.reset_default_state),
                    context.getString(R.string.reset_default_state_summary),
                    0L,
                    true
                )
            )
        }
        return StorageSlimState(
            items,
            items.sumOf { it.size }
        )
    }

    private fun invalidBookCacheSize(bookCache: File): Long {
        if (!bookCache.exists()) return 0L
        val names = appDb.bookDao.all.asSequence().map { it.getFolderName() }.toHashSet()
        return bookCache.listFiles()
            ?.asSequence()
            ?.filter { !names.contains(it.name) }
            ?.sumOf { it.safeSize() }
            ?: 0L
    }

    private fun shelfBookCacheSize(bookCache: File): Long {
        if (!bookCache.exists()) return 0L
        val names = appDb.bookDao.all.asSequence().map { it.getFolderName() }.toHashSet()
        return bookCache.listFiles()
            ?.asSequence()
            ?.filter { names.contains(it.name) }
            ?.sumOf { it.safeSize() }
            ?: 0L
    }

    private fun clearShelfBookCache() {
        val bookCache = context.externalFiles.getFile("book_cache")
        if (!bookCache.exists()) return
        val names = appDb.bookDao.all.asSequence().map { it.getFolderName() }.toHashSet()
        bookCache.listFiles()
            ?.asSequence()
            ?.filter { names.contains(it.name) }
            ?.forEach { FileUtils.delete(it, true) }
    }

    private fun clearTempBackup() {
        FileUtils.delete(context.externalFiles.getFile("tmp_backup.zip"))
        FileUtils.delete(context.externalFiles.getFile("backup.zip"))
        FileUtils.delete(context.filesDir.getFile("backup"), true)
    }

    private fun clearAppCache() {
        FileUtils.delete(context.cacheDir.absolutePath)
        context.externalCache.listFiles()?.forEach {
            if (it.name != "logs") {
                FileUtils.delete(it, true)
            }
        }
    }

    private fun tempBackupSize(): Long {
        return context.externalFiles.getFile("tmp_backup.zip").safeSize() +
            context.externalFiles.getFile("backup.zip").safeSize() +
            context.filesDir.getFile("backup").safeSize()
    }

    private fun clearWebViewData() {
        WebStorage.getInstance().deleteAllData()
        webViewDirs().forEach {
            FileUtils.delete(it, true)
        }
    }

    private fun webViewDirs(): List<File> {
        return listOf(
            context.getDir("webview", Context.MODE_PRIVATE),
            context.getDir("hws_webview", Context.MODE_PRIVATE),
            context.getDir("app_webview", Context.MODE_PRIVATE),
            File(context.dataDir, "app_webview"),
            File(context.dataDir, "app_hws_webview"),
        ).distinctBy { it.absolutePath }
    }

    fun markResetDefaultState() {
        ResetDefaultState.mark(context)
    }

    fun markDatabaseSlim() {
        ResetDefaultState.markDatabaseSlim(context)
    }

    private fun databaseQuickSize(): Long {
        val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
        return File(dbFile.absolutePath + "-wal").safeSize() +
            File(dbFile.absolutePath + "-shm").safeSize()
    }

    private fun itemSize(type: StorageSlimType): Long {
        return when (type) {
            StorageSlimType.BOOK_CACHE -> shelfBookCacheSize(context.externalFiles.getFile("book_cache"))
            StorageSlimType.INVALID_BOOK_CACHE -> invalidBookCacheSize(context.externalFiles.getFile("book_cache"))
            StorageSlimType.APP_CACHE -> {
                context.cacheDir.safeSize() + context.externalCache.safeSize() -
                    context.externalCache.getFile("logs").safeSize()
            }
            StorageSlimType.WEBVIEW -> webViewDirs().sumOf { it.safeSize() }
            StorageSlimType.LOGS -> context.externalCache.getFile("logs").safeSize()
            StorageSlimType.TEMP_BACKUP -> tempBackupSize()
            StorageSlimType.RULE_DATA -> context.externalFiles.getFile("ruleData").safeSize()
            StorageSlimType.LOCAL_COVERS -> context.externalFiles.getFile("covers").safeSize()
            StorageSlimType.DATABASE -> databaseQuickSize()
            StorageSlimType.RESET_DEFAULT -> 0L
        }
    }

    fun formatSize(size: Long): String {
        return ConvertUtils.formatFileSize(size)
    }

    private fun File.safeSize(): Long {
        if (!exists()) return 0L
        if (isFile) return length()
        return listFiles()?.sumOf { it.safeSize() } ?: 0L
    }
}
