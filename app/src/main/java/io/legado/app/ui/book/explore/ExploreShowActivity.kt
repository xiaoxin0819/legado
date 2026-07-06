package io.legado.app.ui.book.explore

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.entities.SearchBook
import io.legado.app.databinding.ActivityExploreShowBinding
import io.legado.app.databinding.ViewLoadMoreBinding
import io.legado.app.help.book.BookFilter
import io.legado.app.help.book.BookFilterConfig
import io.legado.app.ui.book.info.BookInfoActivity
import io.legado.app.ui.widget.dialog.BookFilterDialog
import io.legado.app.ui.widget.number.NumberPickerDialog
import io.legado.app.ui.widget.recycler.LoadMoreView
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.utils.applyNavigationBarPadding
import io.legado.app.utils.startActivity
import io.legado.app.utils.viewbindingdelegate.viewBinding

class ExploreShowActivity : VMBaseActivity<ActivityExploreShowBinding, ExploreShowViewModel>(),
    ExploreShowAdapter.CallBack {

    override val binding by viewBinding(ActivityExploreShowBinding::inflate)
    override val viewModel by viewModels<ExploreShowViewModel>()

    private val adapter by lazy { ExploreShowAdapter(this, this) }
    private val loadMoreView by lazy { LoadMoreView(this) }
    private val loadMoreViewTop by lazy { LoadMoreView(this) }
    private var oldPage = -1
    private var isClearAll = false
    private var allBooks: List<SearchBook> = emptyList()
    private var bookFilterConfig = BookFilterConfig()
    private var filterAutoLoadCount = 0
    private var menuPage: MenuItem? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.titleBar.title = intent.getStringExtra("exploreName")
        bookFilterConfig = BookFilterDialog.load(this, EXPLORE_FILTER_PREF)
        invalidateOptionsMenu()
        initRecyclerView()
        viewModel.booksData.observe(this) { upData(it) }
        viewModel.addBooksData.observe(this) { upDataTop(it) }
        viewModel.initData(intent)
        viewModel.errorLiveData.observe(this) {
            loadMoreView.error(it)
        }
        viewModel.errorTopLiveData.observe(this) {
            loadMoreViewTop.error(it)
        }
        viewModel.upAdapterLiveData.observe(this) {
            adapter.notifyItemRangeChanged(0, adapter.itemCount, bundleOf(it to null))
        }
        viewModel.pageLiveData.observe(this) {
            menuPage?.title = getString(R.string.menu_page, it)
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, MENU_FILTER, 0, getString(R.string.book_filter)).apply {
            setIcon(R.drawable.ic_filter)
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        menuPage = menu.add(
            0,
            MENU_PAGE,
            1,
            getString(R.string.menu_page, viewModel.pageLiveData.value ?: 1)
        ).apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM or MenuItem.SHOW_AS_ACTION_WITH_TEXT)
        }
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            MENU_FILTER -> {
                showBookFilter()
                true
            }
            MENU_PAGE -> {
                showPagePicker()
                true
            }
            else -> super.onCompatOptionsItemSelected(item)
        }
    }

    private fun initRecyclerView() {
        binding.recyclerView.addItemDecoration(VerticalDivider(this))
        binding.recyclerView.adapter = adapter
        binding.recyclerView.applyNavigationBarPadding()
        adapter.addFooterView {
            ViewLoadMoreBinding.bind(loadMoreView)
        }
        loadMoreView.startLoad()
        loadMoreView.setOnClickListener {
            if (!loadMoreView.isLoading) {
                scrollToBottom(true)
            }
        }
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(1)) {
                    scrollToBottom()
                } else if (!recyclerView.canScrollVertically(-1) && dy < 0) {
                    scrollToTop()
                }
            }
        })
    }

    private fun scrollToBottom(forceLoad: Boolean = false) {
        if ((loadMoreView.hasMore && !loadMoreView.isLoading && !loadMoreViewTop.isLoading) || forceLoad) {
            loadMoreView.hasMore()
            viewModel.explore()
        }
    }

    private fun scrollToTop(forceLoad: Boolean = false) {
        if ((oldPage > 1 && !loadMoreView.isLoading && !loadMoreViewTop.isLoading) || forceLoad) {
            loadMoreViewTop.hasMore()
            oldPage = maxOf(1, oldPage - LOAD_PAGE_COUNT)
            viewModel.explore(oldPage)
        }
    }

    private fun upData(books: List<SearchBook>) {
        loadMoreView.stopLoad()
        val oldRawSize = allBooks.size
        allBooks = books
        val showBooks = filterBooks()
        if (books.isEmpty() && adapter.isEmpty()) {
            loadMoreView.noMore(getString(R.string.empty))
        } else if (oldRawSize == books.size && !maybeAutoLoadFilteredMore()) {
            loadMoreView.noMore()
        } else {
            adapter.setItems(showBooks)
            if (isClearAll) {
                val layoutManager = binding.recyclerView.layoutManager as LinearLayoutManager
                layoutManager.scrollToPositionWithOffset(1, 0)
                isClearAll = false
            }
            maybeAutoLoadFilteredMore()
        }
    }

    private fun upDataTop(books: List<SearchBook>) {
        loadMoreViewTop.stopLoad()
        allBooks = books + allBooks
        adapter.setItems(filterBooks())
        val layoutManager = binding.recyclerView.layoutManager as LinearLayoutManager
        if (layoutManager.findFirstVisibleItemPosition() <= 1) {
            layoutManager.scrollToPositionWithOffset(books.size, 0)
        }
        if (oldPage <= 1) {
            val layoutParams = loadMoreViewTop.layoutParams
            if (layoutParams != null) {
                layoutParams.height = 0
                loadMoreViewTop.layoutParams = layoutParams
            }
        }
    }

    private fun filterBooks(): List<SearchBook> {
        return BookFilter.apply(allBooks, bookFilterConfig) { it }
    }

    private fun showBookFilter() {
        BookFilterDialog.show(this, EXPLORE_FILTER_PREF) {
            bookFilterConfig = it
            filterAutoLoadCount = 0
            adapter.setItems(filterBooks())
            maybeAutoLoadFilteredMore()
        }
    }

    private fun showPagePicker() {
        val page = viewModel.pageLiveData.value ?: 1
        NumberPickerDialog(this)
            .setTitle(getString(R.string.change_page))
            .setMaxValue(999)
            .setMinValue(1)
            .setValue(page)
            .show {
                if (page != it) {
                    if (oldPage == -1 && it != 1) {
                        adapter.addHeaderView {
                            ViewLoadMoreBinding.bind(loadMoreViewTop)
                        }
                    } else if (it != 1) {
                        val layoutParams = loadMoreViewTop.layoutParams
                        if (layoutParams?.height == 0) {
                            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                            loadMoreViewTop.layoutParams = layoutParams
                        }
                    }
                    oldPage = it
                    viewModel.skipPage(it)
                    isClearAll = true
                    allBooks = emptyList()
                    filterAutoLoadCount = 0
                    adapter.clearItems()
                    if (!loadMoreView.hasMore) {
                        scrollToBottom(true)
                    }
                }
            }
    }

    private fun maybeAutoLoadFilteredMore(): Boolean {
        if (!bookFilterConfig.isActive
            || loadMoreView.isLoading
            || loadMoreViewTop.isLoading
            || !loadMoreView.hasMore
            || adapter.getActualItemCount() >= FILTER_AUTO_LOAD_TARGET
            || filterAutoLoadCount >= FILTER_AUTO_LOAD_MAX_PAGES
        ) {
            return false
        }
        filterAutoLoadCount++
        scrollToBottom(true)
        return true
    }

    override fun isInBookshelf(book: SearchBook): Boolean {
        return viewModel.isInBookShelf(book)
    }

    override fun showBookInfo(book: SearchBook) {
        startActivity<BookInfoActivity> {
            putExtra("name", book.name)
            putExtra("author", book.author)
            putExtra("bookUrl", book.bookUrl)
        }
    }

    companion object {
        private const val MENU_FILTER = 1
        private const val MENU_PAGE = 2
        private const val EXPLORE_FILTER_PREF = "exploreBookFilter"
        private const val FILTER_AUTO_LOAD_TARGET = 20
        private const val FILTER_AUTO_LOAD_MAX_PAGES = 5
        private const val LOAD_PAGE_COUNT = 5
    }
}
