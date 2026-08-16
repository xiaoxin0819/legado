package io.legado.app.ui.main.bookshelf.style1.books

import android.content.Context
import android.os.Bundle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import io.legado.app.base.adapter.DiffRecyclerAdapter
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.data.entities.Book

abstract class BaseBooksAdapter<VB : ViewBinding>(context: Context) :
    DiffRecyclerAdapter<Book, VB>(context) {

    override val keepScrollPosition = true
    private var bookPositions: Map<String, Int> = emptyMap()
    private var recyclerView: RecyclerView? = null

    override val diffItemCallback: DiffUtil.ItemCallback<Book> =
        object : DiffUtil.ItemCallback<Book>() {

            override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean {
                return oldItem.name == newItem.name
                        && oldItem.author == newItem.author
            }

            override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean {
                return when {
                    oldItem.durChapterTime != newItem.durChapterTime -> false
                    oldItem.name != newItem.name -> false
                    oldItem.author != newItem.author -> false
                    oldItem.durChapterTitle != newItem.durChapterTitle -> false
                    oldItem.latestChapterTitle != newItem.latestChapterTitle -> false
                    oldItem.lastCheckCount != newItem.lastCheckCount -> false
                    oldItem.getDisplayCover() != newItem.getDisplayCover() -> false
                    oldItem.getUnreadChapterNum() != newItem.getUnreadChapterNum() -> false
                    else -> true
                }
            }

            override fun getChangePayload(oldItem: Book, newItem: Book): Any? {
                val bundle = Bundle()
                if (oldItem.name != newItem.name) {
                    bundle.putString("name", newItem.name)
                }
                if (oldItem.author != newItem.author) {
                    bundle.putString("author", newItem.author)
                }
                if (oldItem.durChapterTitle != newItem.durChapterTitle) {
                    bundle.putString("dur", newItem.durChapterTitle)
                }
                if (oldItem.latestChapterTitle != newItem.latestChapterTitle) {
                    bundle.putString("last", newItem.latestChapterTitle)
                }
                if (oldItem.getDisplayCover() != newItem.getDisplayCover()) {
                    bundle.putString("cover", newItem.getDisplayCover())
                }
                if (oldItem.lastCheckCount != newItem.lastCheckCount
                    || oldItem.durChapterTime != newItem.durChapterTime
                    || oldItem.getUnreadChapterNum() != newItem.getUnreadChapterNum()
                    || oldItem.lastCheckCount != newItem.lastCheckCount
                ) {
                    bundle.putBoolean("refresh", true)
                }
                if (oldItem.latestChapterTime != newItem.latestChapterTime) {
                    bundle.putBoolean("lastUpdateTime", true)
                }
                if (bundle.isEmpty) return null
                return bundle
            }

        }

    override fun onViewRecycled(holder: ItemViewHolder) {
        super.onViewRecycled(holder)
        holder.itemView.setOnClickListener(null)
        holder.itemView.setOnLongClickListener(null)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        if (this.recyclerView === recyclerView) {
            this.recyclerView = null
        }
        super.onDetachedFromRecyclerView(recyclerView)
    }

    override fun onCurrentListChanged() {
        bookPositions = getItems()
            .mapIndexed { index, book -> book.bookUrl to index }
            .toMap()
    }

    fun notification(bookUrl: String) {
        val position = bookPositions[bookUrl] ?: return
        notifyItemChanged(position, Bundle().apply {
            putString("refresh", null)
            putString("lastUpdateTime", null)
        })
    }

    fun upLastUpdateTime() {
        val layoutManager = recyclerView?.layoutManager as? LinearLayoutManager ?: return
        val first = layoutManager.findFirstVisibleItemPosition()
        val last = layoutManager.findLastVisibleItemPosition()
        if (first == RecyclerView.NO_POSITION || last < first) return
        notifyItemRangeChanged(first, last - first + 1, Bundle().apply {
            putString("lastUpdateTime", null)
        })
    }

    interface CallBack {
        fun open(book: Book)
        fun openBookInfo(book: Book)
        fun isUpdate(bookUrl: String): Boolean
    }
}
