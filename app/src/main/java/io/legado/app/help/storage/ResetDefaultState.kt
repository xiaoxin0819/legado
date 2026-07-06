package io.legado.app.help.storage

import android.content.Context
import androidx.preference.PreferenceManager
import io.legado.app.data.AppDatabase
import java.io.File

object ResetDefaultState {

    private const val KEY_PENDING_RESET = "storage_slim_pending_reset_default_state"
    private const val KEY_PENDING_DATABASE_SLIM = "storage_slim_pending_database_slim"

    fun mark(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
            .edit()
            .putBoolean(KEY_PENDING_RESET, true)
            .commit()
    }

    fun markDatabaseSlim(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
            .edit()
            .putBoolean(KEY_PENDING_DATABASE_SLIM, true)
            .commit()
    }

    fun resetIfNeeded(context: Context) {
        val appContext = context.applicationContext
        val preferences = PreferenceManager.getDefaultSharedPreferences(appContext)
        when {
            preferences.getBoolean(KEY_PENDING_RESET, false) -> {
                preferences.edit().clear().commit()
                clearAppPrivateData(appContext)
            }

            preferences.getBoolean(KEY_PENDING_DATABASE_SLIM, false) -> {
                preferences.edit().remove(KEY_PENDING_DATABASE_SLIM).commit()
                deleteDatabaseTempFiles(appContext)
            }
        }
    }

    private fun clearAppPrivateData(context: Context) {
        context.deleteDatabase(AppDatabase.DATABASE_NAME)
        context.externalCacheDir?.deleteRecursivelySafely()
        context.getExternalFilesDir(null)?.deleteRecursivelySafely()

        File(context.applicationInfo.dataDir).listFiles()?.forEach {
            if (!it.name.startsWith("lib")) {
                it.deleteRecursivelySafely()
            }
        }
        context.filesDir.mkdirs()
        context.cacheDir.mkdirs()
        context.noBackupFilesDir.mkdirs()
        context.getExternalFilesDir(null)?.mkdirs()
        context.externalCacheDir?.mkdirs()
    }

    private fun deleteDatabaseTempFiles(context: Context) {
        val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
        listOf(
            File(dbFile.absolutePath + "-wal"),
            File(dbFile.absolutePath + "-shm"),
            File(dbFile.absolutePath + "-journal")
        ).forEach { it.deleteRecursivelySafely() }
    }

    private fun File.deleteRecursivelySafely() {
        runCatching {
            if (exists()) {
                deleteRecursively()
            }
        }
    }
}
