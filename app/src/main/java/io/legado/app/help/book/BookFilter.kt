package io.legado.app.help.book

import io.legado.app.data.entities.BaseBook
import io.legado.app.data.entities.Book
import io.legado.app.utils.safeCnCompare
import kotlin.math.roundToLong

data class BookFilterConfig(
    val minWordCount: Long? = null,
    val maxWordCount: Long? = null,
    val includeKind: String = "",
    val excludeKind: String = "",
    val sortMode: SortMode = SortMode.DEFAULT
) {
    val isActive: Boolean
        get() = minWordCount != null
                || maxWordCount != null
                || includeKind.isNotBlank()
                || excludeKind.isNotBlank()
                || sortMode != SortMode.DEFAULT

    enum class SortMode {
        DEFAULT,
        WORD_COUNT_ASC,
        WORD_COUNT_DESC,
        NAME_ASC,
        AUTHOR_ASC
    }
}

object BookFilter {

    fun <T : BaseBook> apply(
        books: List<T>,
        config: BookFilterConfig,
        defaultSort: (List<T>) -> List<T>
    ): List<T> {
        val includeWords = config.includeKind.keywords()
        val excludeWords = config.excludeKind.keywords()
        val filtered = if (
            config.minWordCount == null
            && config.maxWordCount == null
            && includeWords.isEmpty()
            && excludeWords.isEmpty()
        ) {
            books
        } else {
            books.filter { book ->
                val wordCount = parseWordCount(book.wordCount)
                if (config.minWordCount != null && (wordCount == null || wordCount < config.minWordCount)) {
                    return@filter false
                }
                if (config.maxWordCount != null && (wordCount == null || wordCount > config.maxWordCount)) {
                    return@filter false
                }
                val kindText = book.filterKindText()
                if (includeWords.isNotEmpty() && includeWords.none { kindText.contains(it, true) }) {
                    return@filter false
                }
                if (excludeWords.isNotEmpty() && excludeWords.any { kindText.contains(it, true) }) {
                    return@filter false
                }
                true
            }
        }
        return when (config.sortMode) {
            BookFilterConfig.SortMode.DEFAULT -> defaultSort(filtered)
            BookFilterConfig.SortMode.WORD_COUNT_ASC -> {
                filtered.sortedWith(compareBy<T> { parseWordCount(it.wordCount) ?: Long.MAX_VALUE }
                    .thenBy { it.name })
            }
            BookFilterConfig.SortMode.WORD_COUNT_DESC -> {
                filtered.sortedWith(compareByDescending<T> { parseWordCount(it.wordCount) ?: Long.MIN_VALUE }
                    .thenBy { it.name })
            }
            BookFilterConfig.SortMode.NAME_ASC -> filtered.sortedWith { o1, o2 ->
                o1.name.safeCnCompare(o2.name)
            }
            BookFilterConfig.SortMode.AUTHOR_ASC -> filtered.sortedWith { o1, o2 ->
                o1.author.safeCnCompare(o2.author)
            }
        }
    }

    fun parseWordCount(value: String?): Long? {
        val text = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val normalized = text
            .replace(",", "")
            .replace("\uFF0C", "")
            .replace(" ", "")
        val match = Regex("""([0-9]+(?:\.[0-9]+)?)(\u4EBF|\u4E07|\u5343|k|K|w|W)?""")
            .find(normalized) ?: return null
        val number = match.groupValues[1].toDoubleOrNull() ?: return null
        val unit = match.groupValues.getOrNull(2)
        val multiplier = when (unit) {
            "\u4EBF" -> 100_000_000.0
            "\u4E07", "w", "W" -> 10_000.0
            "\u5343", "k", "K" -> 1_000.0
            else -> 1.0
        }
        return (number * multiplier).roundToLong()
    }

    fun toWanText(value: Long?): String {
        return value?.let {
            if (it % 10_000L == 0L) {
                (it / 10_000L).toString()
            } else {
                (it / 10_000.0).toString().trimEnd('0').trimEnd('.')
            }
        } ?: ""
    }

    fun parseWanInput(value: String?): Long? {
        val text = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return (text.toDoubleOrNull() ?: return null).let {
            (it * 10_000).roundToLong()
        }
    }

    private fun String.keywords(): List<String> {
        return split(",", "\uFF0C", "\n", " ")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun BaseBook.filterKindText(): String {
        val base = "${kind.orEmpty()} ${wordCount.orEmpty()}"
        return if (this is Book) {
            "$base ${customTag.orEmpty()}"
        } else {
            base
        }
    }
}

