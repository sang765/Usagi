package org.draken.usagi.settings.sources.catalog

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.draken.usagi.R
import org.draken.usagi.core.model.titleResId
import org.draken.usagi.core.nav.router
import org.draken.usagi.core.ui.BaseActivity
import org.draken.usagi.core.ui.list.OnListItemClickListener
import org.draken.usagi.core.ui.util.FadingAppbarMediator
import org.draken.usagi.core.ui.util.ReversibleActionObserver
import org.draken.usagi.core.ui.widgets.ChipsView
import org.draken.usagi.core.ui.widgets.ChipsView.ChipModel
import org.draken.usagi.core.util.LocaleComparator
import org.draken.usagi.core.util.ext.getDisplayName
import org.draken.usagi.core.util.ext.observe
import org.draken.usagi.core.util.ext.observeEvent
import org.draken.usagi.core.util.ext.toLocale
import org.draken.usagi.databinding.ActivitySourcesCatalogBinding
import org.draken.usagi.list.ui.adapter.TypedListSpacingDecoration
import org.draken.usagi.main.ui.owners.AppBarOwner
import tsuki.model.ContentType

@AndroidEntryPoint
class SourcesCatalogActivity :
	BaseActivity<ActivitySourcesCatalogBinding>(),
	OnListItemClickListener<SourceCatalogItem.Source>,
	AppBarOwner,
	MenuItem.OnActionExpandListener,
	ChipsView.OnChipClickListener {
	override val appBar: AppBarLayout
		get() = viewBinding.appbar

	private val viewModel by viewModels<SourcesCatalogViewModel>()
	private var tachiyomiPreviewPackageName: String? = null

	override fun onResume() {
		super.onResume()
		val packageName = tachiyomiPreviewPackageName ?: return
		tachiyomiPreviewPackageName = null
		lifecycleScope.launch {
			viewModel.unloadTachiyomiPreview(packageName)
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivitySourcesCatalogBinding.inflate(layoutInflater))
		setDisplayHomeAsUp(isEnabled = true, showUpAsClose = false)
		val externalTitle = intent.getStringExtra(EXTRA_EXTERNAL_REPOSITORY_TITLE)
		if (!externalTitle.isNullOrBlank()) title = externalTitle
		intent.getStringExtra(EXTRA_EXTERNAL_REPOSITORY_URL)?.let(viewModel::openExternalRepository)
		val sourcesAdapter =
			SourcesCatalogAdapter(
				listener = this,
				onTachiyomiClick = ::openTachiyomiSource,
				onTachiyomiInstall = ::toggleTachiyomi,
			)
		with(viewBinding.recyclerView) {
			setHasFixedSize(true)
			addItemDecoration(TypedListSpacingDecoration(context, false))
			adapter = sourcesAdapter
		}
		viewBinding.chipsFilter.onChipClickListener = this
		FadingAppbarMediator(viewBinding.appbar, viewBinding.toolbar).bind()
		viewModel.content.observe(this, sourcesAdapter)
		viewModel.onActionDone.observeEvent(
			this,
			ReversibleActionObserver(viewBinding.recyclerView),
		)
		viewModel.uiState.observe(this, ::updateFilters)
		addMenuProvider(SourcesCatalogMenuProvider(this, viewModel, this))
	}

	override fun onDestroy() {
		viewBinding.recyclerView.adapter = null
		super.onDestroy()
	}

	override fun onApplyWindowInsets(
		v: View,
		insets: WindowInsetsCompat,
	): WindowInsetsCompat {
		val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
		viewBinding.recyclerView.updatePadding(left = bars.left, right = bars.right, bottom = bars.bottom)
		viewBinding.appbar.updatePadding(left = bars.left, right = bars.right, top = bars.top)
		return WindowInsetsCompat.Builder(insets).setInsets(WindowInsetsCompat.Type.systemBars(), Insets.NONE).build()
	}

	override fun onChipClick(
		chip: Chip,
		data: Any?,
	) {
		when (data) {
			is ContentType -> viewModel.setContentType(data, !chip.isChecked)
			is Boolean -> viewModel.setNewOnly(!chip.isChecked)
			"plugins" -> showPluginsMenu(chip)
			else -> showLocalesMenu(chip)
		}
	}

	override fun onItemClick(
		item: SourceCatalogItem.Source,
		view: View,
	) {
		router.openList(item.source, null, null)
	}

	override fun onItemLongClick(
		item: SourceCatalogItem.Source,
		view: View,
	): Boolean {
		viewModel.addSource(item.source)
		return false
	}

	override fun onMenuItemActionExpand(item: MenuItem): Boolean {
		val sq =
			(item.actionView as? SearchView)
				?.query
				?.trim()
				?.toString()
				.orEmpty()
		viewModel.performSearch(sq)
		return true
	}

	override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
		viewModel.performSearch(null)
		return true
	}

	private fun openTachiyomiSource(
		item: SourceCatalogItem.Tachiyomi,
		view: View,
	) {
		lifecycleScope.launch {
			val result = viewModel.prepareTachiyomiSource(item)
			if (result == null) {
				Snackbar.make(viewBinding.root, R.string.load_failed, Snackbar.LENGTH_LONG).show()
				return@launch
			}
			tachiyomiPreviewPackageName = result.previewPackageName
			router.openList(result.source, null, null)
		}
	}

	private fun toggleTachiyomi(
		item: SourceCatalogItem.Tachiyomi,
		view: View,
	) {
		lifecycleScope.launch {
			val success = viewModel.toggleTachiyomi(item)
			Snackbar.make(viewBinding.root, if (success) R.string.load_success else R.string.load_failed, Snackbar.LENGTH_LONG).show()
		}
	}

	private fun updateFilters(state: SourcesCatalogUiState) {
		val appliedFilter = state.appliedFilter
		val chips = ArrayList<ChipModel>(state.contentTypes.size + 3)
		if (!viewModel.isExternalCatalog) {
			chips +=
				ChipModel(
					title = appliedFilter.plugin?.removeSuffix(".jar") ?: getString(R.string.any),
					icon = R.drawable.ic_services,
					isDropdown = true,
					data = "plugins",
				)
		}

		chips +=
			ChipModel(
				title = appliedFilter.locale?.toLocale().getDisplayName(this),
				icon = R.drawable.ic_language,
				isDropdown = true,
			)
		if (state.hasNewSources) {
			chips +=
				ChipModel(
					title = getString(R.string._new),
					icon = R.drawable.ic_updated,
					isChecked = appliedFilter.isNewOnly,
					data = true,
				)
		}
		state.contentTypes.mapTo(chips) { type ->
			ChipModel(title = getString(type.titleResId), isChecked = type in appliedFilter.types, data = type)
		}
		viewBinding.chipsFilter.setChips(chips)
	}

	private fun showLocalesMenu(anchor: View) {
		val locales = viewModel.locales.mapTo(ArrayList(viewModel.locales.size)) { it to it?.toLocale() }
		locales.sortWith(compareBy(nullsFirst(LocaleComparator())) { it.second })
		val menu = PopupMenu(this, anchor)
		for ((i, lc) in locales.withIndex()) menu.menu.add(Menu.NONE, Menu.NONE, i, lc.second.getDisplayName(this))
		menu.setOnMenuItemClickListener {
			viewModel.setLocale(locales.getOrNull(it.order)?.first)
			true
		}
		menu.show()
	}

	private fun showPluginsMenu(anchor: View) {
		val menu = PopupMenu(this, anchor)
		menu.menu.add(Menu.NONE, Menu.NONE, 0, getString(R.string.any))
		for ((i, plugin) in viewModel.plugins.withIndex()) menu.menu.add(Menu.NONE, Menu.NONE, i + 1, plugin.removeSuffix(".jar"))
		menu.setOnMenuItemClickListener {
			val plugin = if (it.order == 0) null else viewModel.plugins[it.order - 1]
			viewModel.setPlugin(plugin)
			true
		}
		menu.show()
	}

	companion object {
		const val EXTRA_EXTERNAL_REPOSITORY_URL = "external_repository_url"
		const val EXTRA_EXTERNAL_REPOSITORY_TITLE = "external_repository_title"
	}
}
