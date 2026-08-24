package org.draken.usagi.settings.sources.catalog

import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePaddingRelative
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.draken.usagi.R
import org.draken.usagi.core.model.getSummary
import org.draken.usagi.core.model.getTitle
import org.draken.usagi.core.ui.image.FaviconDrawable
import org.draken.usagi.core.ui.list.OnListItemClickListener
import org.draken.usagi.core.util.ext.drawableStart
import org.draken.usagi.core.util.ext.getThemeDimensionPixelOffset
import org.draken.usagi.core.util.ext.setTextAndVisible
import org.draken.usagi.databinding.ItemEmptyHintBinding
import org.draken.usagi.databinding.ItemSourceCatalogBinding
import org.draken.usagi.list.ui.model.ListModel
import androidx.appcompat.R as appcompatR

fun sourceCatalogItemSourceAD(listener: OnListItemClickListener<SourceCatalogItem.Source>) =
	adapterDelegateViewBinding<SourceCatalogItem.Source, ListModel, ItemSourceCatalogBinding>(
		{ layoutInflater, parent -> ItemSourceCatalogBinding.inflate(layoutInflater, parent, false) },
	) {
		binding.imageViewAdd.setOnClickListener { v -> listener.onItemLongClick(item, v) }
		binding.root.setOnClickListener { v -> listener.onItemClick(item, v) }
		val basePadding = context.getThemeDimensionPixelOffset(appcompatR.attr.listPreferredItemPaddingEnd, binding.root.paddingStart)
		binding.root.updatePaddingRelative(end = (basePadding - context.resources.getDimensionPixelOffset(R.dimen.margin_small)).coerceAtLeast(0))
		bind {
			binding.textViewTitle.text = item.source.getTitle(context)
			binding.textViewDescription.text = item.source.getSummary(context)
			binding.textViewDescription.drawableStart = if (item.source.isBroken) ContextCompat.getDrawable(context, R.drawable.ic_off_small) else null
			FaviconDrawable(context, R.style.FaviconDrawable_Small, item.source.name)
			binding.imageViewIcon.setImageAsync(item.source)
		}
	}

fun sourceCatalogItemTachiyomiAD(
	onClick: (SourceCatalogItem.Tachiyomi, View) -> Unit,
	onInstall: (SourceCatalogItem.Tachiyomi, View) -> Unit,
) = adapterDelegateViewBinding<SourceCatalogItem.Tachiyomi, ListModel, ItemSourceCatalogBinding>(
	{ layoutInflater, parent -> ItemSourceCatalogBinding.inflate(layoutInflater, parent, false) },
) {
	var loadingAnimator: ObjectAnimator? = null
	binding.root.setOnClickListener { v -> if (!item.isLoading) onClick(item, v) }
	binding.imageViewAdd.setOnClickListener { v -> if (!item.isLoading) onInstall(item, v) }
	val basePadding = context.getThemeDimensionPixelOffset(appcompatR.attr.listPreferredItemPaddingEnd, binding.root.paddingStart)
	binding.root.updatePaddingRelative(end = (basePadding - context.resources.getDimensionPixelOffset(R.dimen.margin_small)).coerceAtLeast(0))
	bind {
		loadingAnimator?.cancel()
		loadingAnimator = null
		binding.imageViewAdd.rotation = 0f
		binding.root.isEnabled = !item.isLoading
		binding.imageViewAdd.isEnabled = !item.isLoading
		if (item.isLoading) {
			binding.imageViewAdd.setImageResource(R.drawable.ic_sync)
			binding.imageViewAdd.contentDescription = context.getString(R.string.loading_)
			loadingAnimator =
				ObjectAnimator.ofFloat(binding.imageViewAdd, View.ROTATION, 0f, 360f).apply {
					duration = 800L
					interpolator = LinearInterpolator()
					repeatCount = ObjectAnimator.INFINITE
					start()
				}
		}
		val fallback = FaviconDrawable(context, R.style.FaviconDrawable, item.artifact.packageName)
		binding.imageViewIcon.errorDrawable = fallback
		binding.imageViewIcon.fallbackDrawable = fallback
		if (item.artifact.iconUrl.isNullOrBlank()) {
			binding.imageViewIcon.setImageDrawable(fallback)
		} else {
			binding.imageViewIcon.setImageAsync(item.artifact.iconUrl)
		}
		binding.imageViewIcon.background = null
		binding.textViewTitle.text = item.displayName
		binding.textViewDescription.text = item.description(context)
		binding.textViewDescription.drawableStart = null
		binding.imageViewAdd.isVisible = true
		if (!item.isLoading) {
			binding.imageViewAdd.setImageResource(
				when {
					item.hasUpdate -> R.drawable.ic_updated
					item.isInstalled -> R.drawable.ic_delete
					else -> R.drawable.ic_download
				},
			)
			binding.imageViewAdd.contentDescription =
				context.getString(
					when {
						item.hasUpdate -> R.string.update
						item.isInstalled -> R.string.remove
						else -> R.string.add
					},
				)
		}
	}
}

fun sourceCatalogItemHintAD() =
	adapterDelegateViewBinding<SourceCatalogItem.Hint, ListModel, ItemEmptyHintBinding>(
		{ inflater, parent -> ItemEmptyHintBinding.inflate(inflater, parent, false) },
	) {
		binding.buttonRetry.isVisible = false
		bind {
			binding.icon.setImageAsync(item.icon)
			binding.textPrimary.setText(item.title)
			binding.textSecondary.setTextAndVisible(item.text)
		}
	}
