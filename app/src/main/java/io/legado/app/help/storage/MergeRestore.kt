package io.legado.app.help.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.legado.app.constant.AppConst.androidId
import io.legado.app.constant.AppLog
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookGroup
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.Bookmark
import io.legado.app.data.entities.ReadRecord
import io.legado.app.help.book.isLocal
import io.legado.app.help.book.upType
import io.legado.app.model.localBook.LocalBook
import io.legado.app.utils.FileUtils
import io.legado.app.utils.GSON
import io.legado.app.utils.compress.ZipUtils
import io.legado.app.utils.createFolderIfNotExist
import io.legado.app.utils.fromJsonArray
import io.legado.app.utils.getFile
import io.legado.app.utils.isContentScheme
import io.legado.app.utils.openInputStream
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import splitties.init.appCtx
import java.io.File
import java.io.FileInputStream

object MergeRestore {

    private const val TAG = "MergeRestore"
    private val mutex = Mutex()

    private val mergePath: String
        get() = appCtx.filesDir.getFile("mergeRestore").createFolderIfNotExist().absolutePath

    suspend fun merge(context: Context, uri: Uri) {
        FileUtils.delete(mergePath)
        File(mergePath).mkdirs()
        runCatching {
            if (uri.isContentScheme()) {
                DocumentFile.fromSingleUri(context, uri)!!.openInputStream()!!.use {
                    ZipUtils.unZipToPath(it, mergePath)
                }
            } else {
                ZipUtils.unZipToPath(File(uri.path!!), mergePath)
            }
        }.onFailure {
            AppLog.put("合并导入备份解压失败\n${it.localizedMessage}", it)
            throw it
        }
        val result = mergeLocked(mergePath)
        appCtx.toastOnUi(result.message)
    }

    suspend fun mergeLocked(path: String): MergeResult {
        return mutex.withLock {
            withContext(IO) {
                mergeBackup(path)
            }
        }
    }

    private fun mergeBackup(path: String): MergeResult {
        val backupGroups = fileToListT<BookGroup>(path, "bookGroup.json").orEmpty()
        val backupSources = fileToListT<BookSource>(path, "bookSource.json").orEmpty()
        val backupBooks = fileToListT<Book>(path, "bookshelf.json").orEmpty()
        val result = MergeResult()

        appDb.runInTransaction {
            val groupIdMap = mergeBookGroups(backupGroups, result)
            mergeBookSources(backupSources, result)
            mergeBooks(backupBooks, groupIdMap, result)
            mergeBookmarks(path, result)
            mergeReadRecords(path, result)
        }

        return result
    }

    private fun mergeBookGroups(
        backupGroups: List<BookGroup>,
        result: MergeResult
    ): Map<Long, Long> {
        val idMap = hashMapOf<Long, Long>()
        val currentGroups = appDb.bookGroupDao.all
        val currentByName = currentGroups
            .filter { it.groupId > 0 }
            .associateBy { it.groupName }
            .toMutableMap()
        var usedMask = currentGroups
            .filter { it.groupId > 0 }
            .fold(0L) { acc, group -> acc or group.groupId }
        var nextOrder = currentGroups
            .filter { it.groupId >= 0 }
            .maxOfOrNull { it.order }
            ?.plus(1) ?: 0

        val newGroups = arrayListOf<BookGroup>()
        backupGroups
            .filter { it.groupId > 0 }
            .forEach { group ->
                currentByName[group.groupName]?.let {
                    idMap[group.groupId] = it.groupId
                    result.reusedGroups += 1
                    return@forEach
                }

                val newId = nextUnusedGroupId(usedMask)
                usedMask = usedMask or newId
                val newGroup = group.copy(groupId = newId, order = nextOrder++)
                currentByName[newGroup.groupName] = newGroup
                idMap[group.groupId] = newId
                newGroups.add(newGroup)
            }

        if (newGroups.isNotEmpty()) {
            appDb.bookGroupDao.insert(*newGroups.toTypedArray())
            result.addedGroups = newGroups.size
        }
        return idMap
    }

    private fun mergeBookSources(
        backupSources: List<BookSource>,
        result: MergeResult
    ) {
        val newSources = backupSources.filterNot {
            appDb.bookSourceDao.has(it.bookSourceUrl)
        }
        if (newSources.isNotEmpty()) {
            appDb.bookSourceDao.insert(*newSources.toTypedArray())
            result.addedSources = newSources.size
        }
        result.skippedSources = backupSources.size - newSources.size
    }

    private fun mergeBooks(
        backupBooks: List<Book>,
        groupIdMap: Map<Long, Long>,
        result: MergeResult
    ) {
        val newBooks = arrayListOf<Book>()
        backupBooks.forEach { book ->
            if (BackupConfig.ignoreLocalBook && book.isLocal) {
                result.skippedBooks += 1
                return@forEach
            }
            book.upType()
            if (book.isLocal) {
                book.coverUrl = LocalBook.getCoverPath(book)
            }
            val mergedGroup = remapGroup(book.group, groupIdMap)
            val currentBook = appDb.bookDao.getBook(book.bookUrl)
                ?: appDb.bookDao.getBook(book.name, book.author)
            if (currentBook == null) {
                newBooks.add(book.copy(group = mergedGroup))
                result.addedBooks += 1
            } else {
                val newGroup = currentBook.group or mergedGroup
                if (newGroup != currentBook.group) {
                    appDb.bookDao.update(currentBook.copy(group = newGroup))
                    result.updatedBooks += 1
                } else {
                    result.skippedBooks += 1
                }
            }
        }
        if (newBooks.isNotEmpty()) {
            appDb.bookDao.insert(*newBooks.toTypedArray())
        }
    }

    private fun mergeBookmarks(path: String, result: MergeResult) {
        fileToListT<Bookmark>(path, "bookmark.json")?.takeIf { it.isNotEmpty() }?.let {
            appDb.bookmarkDao.insert(*it.toTypedArray())
            result.bookmarks = it.size
        }
    }

    private fun mergeReadRecords(path: String, result: MergeResult) {
        fileToListT<ReadRecord>(path, "readRecord.json")?.forEach { readRecord ->
            if (readRecord.deviceId != androidId) {
                appDb.readRecordDao.insert(readRecord)
                result.readRecords += 1
            } else {
                val time = appDb.readRecordDao.getReadTime(readRecord.deviceId, readRecord.bookName)
                if (time == null || time < readRecord.readTime) {
                    appDb.readRecordDao.insert(readRecord)
                    result.readRecords += 1
                }
            }
        }
    }

    private fun remapGroup(group: Long, groupIdMap: Map<Long, Long>): Long {
        var newGroup = 0L
        groupIdMap.forEach { (oldId, newId) ->
            if (group and oldId > 0) {
                newGroup = newGroup or newId
            }
        }
        return newGroup
    }

    private fun nextUnusedGroupId(usedMask: Long): Long {
        var id = 1L
        while (id > 0 && id and usedMask != 0L) {
            id = id shl 1
        }
        if (id <= 0) {
            throw IllegalStateException("书架分组数量已达上限，无法继续合并导入")
        }
        return id
    }

    private inline fun <reified T> fileToListT(path: String, fileName: String): List<T>? {
        return runCatching {
            val file = File(path, fileName)
            if (!file.exists()) {
                return null
            }
            FileInputStream(file).use {
                GSON.fromJsonArray<T>(it).getOrThrow()
            }
        }.onFailure {
            AppLog.put("$fileName\n合并导入解析失败\n${it.localizedMessage}", it)
        }.getOrNull()
    }

    data class MergeResult(
        var addedGroups: Int = 0,
        var reusedGroups: Int = 0,
        var addedSources: Int = 0,
        var skippedSources: Int = 0,
        var addedBooks: Int = 0,
        var updatedBooks: Int = 0,
        var skippedBooks: Int = 0,
        var bookmarks: Int = 0,
        var readRecords: Int = 0
    ) {
        val message: String
            get() = "合并导入完成：新增${addedBooks}本，合并${updatedBooks}本，新增${addedGroups}个分组，新增${addedSources}个书源"
    }
}
