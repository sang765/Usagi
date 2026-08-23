package org.draken.usagi.settings.sources.manage.plugins

import android.annotation.SuppressLint
import android.view.View
import androidx.core.view.isVisible
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.draken.usagi.R
import org.draken.usagi.core.ui.BaseListAdapter
import org.draken.usagi.core.util.ext.setTextAndVisible
import org.draken.usagi.databinding.ItemEmptyHintBinding
import org.draken.usagi.databinding.ItemSourceConfigBinding
import org.draken.usagi.list.ui.adapter.ListItemType
import org.draken.usagi.list.ui.model.ListModel
import org.draken.usagi.settings.sources.manage.plugins.model.PluginManageItem

class PluginManageAdapter(
	onRenameClick: (PluginManageItem.Plugin) -> Unit,
	onUpdateClick: (PluginManageItem.Plugin) -> Unit,
	onLongClick: (PluginManageItem.Plugin) -> Unit,
	onClick: (PluginManageItem.Plugin) -> Unit,
	isSelected: (PluginManageItem.Plugin) -> Boolean,
) : BaseListAdapter<ListModel>() {
	init {
		addDelegate(
			ListItemType.CHAPTER_LIST,
			pluginItemDelegate(onRenameClick, onUpdateClick, onLongClick, onClick, isSelected),
		)
		addDelegate(ListItemType.HINT_EMPTY, pluginPlaceholderDelegate())
	}

	@SuppressLint("ClickableViewAccessibility")
	private fun pluginItemDelegate(
		onRenameClick: (PluginManageItem.Plugin) -> Unit,
		onUpdateClick: (PluginManageItem.Plugin) -> Unit,
		onLongClick: (PluginManageItem.Plugin) -> Unit,
		onClick: (PluginManageItem.Plugin) -> Unit,
		isSelected: (PluginManageItem.Plugin) -> Boolean,
	) = adapterDelegateViewBinding<PluginManageItem, ListModel, ItemSourceConfigBinding>(
		{ layoutInflater, parent -> ItemSourceConfigBinding.inflate(layoutInflater, parent, false) },
		{ item, _, _ -> item is PluginManageItem.Plugin || item is PluginManageItem.Remote },
	) {
		bind {
			when (val current = item) {
				is PluginManageItem.Plugin -> {
					binding.imageViewIcon.setImageResource(R.drawable.ic_services)
					binding.imageViewIcon.background = null
					binding.imageViewMenu.isVisible = true
					binding.imageViewMenu.setImageResource(R.drawable.ic_edit)
					binding.imageViewMenu.contentDescription = context.getString(R.string.rename)
					binding.imageViewMenu.setOnClickListener { onRenameClick(current) }
					binding.imageViewMenu.setOnTouchListener(null)
					binding.imageViewRemove.isVisible = false
					binding.imageViewRemove.setOnTouchListener(null)
					binding.imageViewRemove.setOnClickListener(null)
					binding.imageViewAdd.setImageResource(R.drawable.ic_download)
					binding.imageViewAdd.contentDescription = context.getString(R.string.update)
					itemView.isSelected = isSelected(current)
					itemView.setOnLongClickListener {
						onLongClick(current)
						true
					}
					itemView.setOnClickListener { onClick(current) }
					binding.textViewTitle.text = current.displayName
					val parts = ArrayList<String>(3)
					current.repository?.takeIf { it.isNotBlank() }?.let(parts::add)
					current.installedTag?.takeIf { it.isNotBlank() }?.let(parts::add)
					binding.textViewDescription.text = if (parts.isEmpty()) current.name else parts.joinToString(" • ")
					binding.imageViewAdd.isVisible = current.hasUpdate
					binding.imageViewAdd.setOnClickListener(
						if (current.hasUpdate) View.OnClickListener { onUpdateClick(current) } else null,
					)
				}

				is PluginManageItem.Remote -> {
					binding.imageViewIcon.setImageResource(R.drawable.ic_services)
					binding.imageViewIcon.background = null
					binding.imageViewMenu.isVisible = false
					binding.imageViewMenu.setOnClickListener(null)
					binding.imageViewRemove.isVisible = false
					binding.imageViewAdd.isVisible = false
					binding.imageViewAdd.setOnClickListener(null)
					itemView.isSelected = false
					itemView.setOnLongClickListener(null)
					itemView.setOnClickListener(null)
					binding.textViewTitle.text = current.displayName
					val details =
						buildList {
							add(current.repository)
							current.versionName?.takeIf { it.isNotBlank() }?.let(::add)
							current.versionCode?.takeIf { it.isNotBlank() }?.let { add("code $it") }
							current.languages.takeIf { it.isNotEmpty() }?.let { add(it.joinToString(", ")) }
							current.contentWarning?.takeIf { it.isNotBlank() }?.let(::add)
							current.packageName.takeIf { it.isNotBlank() }?.let(::add)
						}.joinToString(" • ")
					binding.textViewDescription.text = details
				}

				is PluginManageItem.Placeholder -> {
					Unit
				}
			}
		}
	}

	private fun pluginPlaceholderDelegate() =
		adapterDelegateViewBinding<PluginManageItem.Placeholder, ListModel, ItemEmptyHintBinding>(
			{ layoutInflater, parent -> ItemEmptyHintBinding.inflate(layoutInflater, parent, false) },
		) {
			binding.icon.setImageResource(R.drawable.ic_empty_feed)
			bind {
				binding.textPrimary.setText(item.titleResId)
				binding.textSecondary.setTextAndVisible(item.summaryResId ?: 0)
			}
		}
}
