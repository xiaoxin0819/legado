package io.legado.app.ui.storage

import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.viewModels
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.databinding.ActivityStorageSlimBinding
import io.legado.app.databinding.ItemStorageSlimBinding
import io.legado.app.lib.dialogs.alert
import io.legado.app.utils.applyNavigationBarPadding
import io.legado.app.utils.restart
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding

class StorageSlimActivity : VMBaseActivity<ActivityStorageSlimBinding, StorageSlimViewModel>() {

    override val binding by viewBinding(ActivityStorageSlimBinding::inflate)
    override val viewModel by viewModels<StorageSlimViewModel>()

    private val adapter by lazy { StorageSlimAdapter() }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.recyclerView.adapter = adapter
        binding.recyclerView.applyNavigationBarPadding()
        binding.tvScan.setOnClickListener {
            scan()
        }
        scan()
    }

    private fun scan() {
        binding.tvScan.isEnabled = false
        binding.tvTotal.text = getString(R.string.storage_slim_scanning)
        viewModel.scan { state ->
            binding.tvScan.isEnabled = true
            binding.tvTotal.text = getString(
                R.string.storage_slim_total,
                viewModel.formatSize(state.totalSize)
            )
            adapter.setItems(state.items)
        }
    }

    private fun clean(item: StorageSlimItem) {
        val message = if (item.type == StorageSlimType.RESET_DEFAULT) {
            getString(R.string.reset_default_state_confirm)
        } else if (item.type == StorageSlimType.DATABASE) {
            getString(R.string.storage_slim_database_restart_confirm)
        } else {
            getString(R.string.storage_slim_clean_confirm, item.title)
        }
        alert(item.title, message) {
            yesButton {
                if (item.type == StorageSlimType.RESET_DEFAULT) {
                    viewModel.markResetDefaultState()
                    toastOnUi(R.string.reset_default_state_restarting)
                    applicationContext.restart()
                    return@yesButton
                }
                if (item.type == StorageSlimType.DATABASE) {
                    viewModel.markDatabaseSlim()
                    toastOnUi(R.string.storage_slim_database_restarting)
                    applicationContext.restart()
                    return@yesButton
                }
                binding.tvScan.isEnabled = false
                binding.tvTotal.text = getString(R.string.storage_slim_cleaning)
                viewModel.clean(
                    item.type,
                    onSuccess = { freed ->
                        toastOnUi(
                            getString(R.string.storage_slim_clean_done, viewModel.formatSize(freed))
                        )
                        scan()
                    },
                    onError = {
                        binding.tvScan.isEnabled = true
                        binding.tvTotal.text = getString(R.string.storage_slim_clean_failed)
                        toastOnUi(it.localizedMessage ?: it.javaClass.simpleName)
                    }
                )
            }
            noButton()
        }
    }

    inner class StorageSlimAdapter :
        RecyclerAdapter<StorageSlimItem, ItemStorageSlimBinding>(this@StorageSlimActivity) {

        override fun getViewBinding(parent: ViewGroup): ItemStorageSlimBinding {
            return ItemStorageSlimBinding.inflate(inflater, parent, false)
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: ItemStorageSlimBinding,
            item: StorageSlimItem,
            payloads: MutableList<Any>,
        ) {
            binding.tvTitle.text = item.title
            binding.tvSummary.text = item.summary
            binding.tvSize.text = if (item.type == StorageSlimType.RESET_DEFAULT) {
                ""
            } else {
                viewModel.formatSize(item.size)
            }
            binding.btnClean.setText(
                when (item.type) {
                    StorageSlimType.RESET_DEFAULT -> R.string.restore
                    else -> R.string.clear
                }
            )
            binding.btnClean.isEnabled = item.enabled &&
                (item.size > 0 || item.type == StorageSlimType.RESET_DEFAULT)
        }

        override fun registerListener(holder: ItemViewHolder, binding: ItemStorageSlimBinding) {
            binding.btnClean.setOnClickListener {
                getItem(holder.layoutPosition)?.let {
                    clean(it)
                }
            }
        }
    }
}
