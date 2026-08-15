package org.draken.usagi.list.ui.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.draken.usagi.core.ui.widgets.ChipsView
import org.draken.usagi.databinding.ItemQuickFilterBinding
import org.draken.usagi.list.domain.ListFilterOption
import org.draken.usagi.list.ui.model.ListModel
import org.draken.usagi.list.ui.model.QuickFilter

fun quickFilterAD(listener: QuickFilterClickListener) =
	adapterDelegateViewBinding<QuickFilter, ListModel, ItemQuickFilterBinding>(
		{ layoutInflater, parent -> ItemQuickFilterBinding.inflate(layoutInflater, parent, false) },
	) {
		onViewRecycled {
			binding.chipsTags.clearForRecycle()
		}

		bind {
			binding.chipsTags.onChipClickListener =
				ChipsView.OnChipClickListener { _, data ->
					if (data is ListFilterOption) {
						listener.onFilterOptionClick(data)
					}
				}
			binding.chipsTags.setChips(item.items)
		}
	}
