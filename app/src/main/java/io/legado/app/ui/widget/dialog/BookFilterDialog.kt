package io.legado.app.ui.widget.dialog

import android.content.Context
import android.widget.ArrayAdapter
import io.legado.app.R
import io.legado.app.databinding.DialogBookFilterBinding
import io.legado.app.help.book.BookFilter
import io.legado.app.help.book.BookFilterConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.getPrefString
import io.legado.app.utils.putPrefString
import io.legado.app.utils.removePref

object BookFilterDialog {

    fun show(
        context: Context,
        prefKey: String,
        onChanged: (BookFilterConfig) -> Unit
    ) {
        val binding = DialogBookFilterBinding.inflate(android.view.LayoutInflater.from(context))
        val config = load(context, prefKey)
        val sortItems = listOf(
            context.getString(R.string.sort_default),
            context.getString(R.string.sort_by_word_count_asc),
            context.getString(R.string.sort_by_word_count_desc),
            context.getString(R.string.sort_by_name),
            context.getString(R.string.bookshelf_px_5)
        )
        binding.editMinWordCount.setText(BookFilter.toWanText(config.minWordCount))
        binding.editMaxWordCount.setText(BookFilter.toWanText(config.maxWordCount))
        binding.editIncludeKind.setText(config.includeKind)
        binding.editExcludeKind.setText(config.excludeKind)
        binding.spSortMode.adapter = ArrayAdapter(
            context,
            R.layout.item_text_common,
            sortItems
        ).apply {
            setDropDownViewResource(R.layout.item_spinner_dropdown)
        }
        binding.spSortMode.setSelection(config.sortMode.ordinal)
        context.alert(titleResource = R.string.book_filter) {
            customView { binding.root }
            okButton {
                val newConfig = BookFilterConfig(
                    minWordCount = BookFilter.parseWanInput(binding.editMinWordCount.text?.toString()),
                    maxWordCount = BookFilter.parseWanInput(binding.editMaxWordCount.text?.toString()),
                    includeKind = binding.editIncludeKind.text?.toString()?.trim().orEmpty(),
                    excludeKind = binding.editExcludeKind.text?.toString()?.trim().orEmpty(),
                    sortMode = BookFilterConfig.SortMode.entries.getOrElse(
                        binding.spSortMode.selectedItemPosition
                    ) {
                        BookFilterConfig.SortMode.DEFAULT
                    }
                )
                save(context, prefKey, newConfig)
                onChanged(newConfig)
            }
            neutralButton(R.string.reset) {
                context.removePref(prefKey)
                onChanged(BookFilterConfig())
            }
            cancelButton()
        }
    }

    fun load(context: Context, prefKey: String): BookFilterConfig {
        return context.getPrefString(prefKey)
            ?.let { GSON.fromJsonObject<BookFilterConfig>(it).getOrNull() }
            ?: BookFilterConfig()
    }

    private fun save(context: Context, prefKey: String, config: BookFilterConfig) {
        if (config.isActive) {
            context.putPrefString(prefKey, GSON.toJson(config))
        } else {
            context.removePref(prefKey)
        }
    }
}
