package io.legado.app.ui.storage

data class StorageSlimItem(
    val type: StorageSlimType,
    val title: String,
    val summary: String,
    val size: Long,
    val enabled: Boolean = true,
)

enum class StorageSlimType {
    BOOK_CACHE,
    INVALID_BOOK_CACHE,
    APP_CACHE,
    WEBVIEW,
    LOGS,
    TEMP_BACKUP,
    RULE_DATA,
    LOCAL_COVERS,
    DATABASE,
    RESET_DEFAULT,
}

data class StorageSlimState(
    val items: List<StorageSlimItem>,
    val totalSize: Long,
)
