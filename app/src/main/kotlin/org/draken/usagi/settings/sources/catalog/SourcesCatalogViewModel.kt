package org.draken.usagi.settings.sources.catalog

import androidx.annotation.WorkerThread
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.draken.tsukimix.core.parser.tachiyomi.TachiyomiCatalogSource
import org.draken.tsukimix.core.parser.tachiyomi.TachiyomiExtensionArtifact
import org.draken.usagi.R
import org.draken.usagi.core.TachiyomiRuntime
import org.draken.usagi.core.db.MangaDatabase
import org.draken.usagi.core.db.TABLE_SOURCES
import org.draken.usagi.core.model.MangaSourceInfo
import org.draken.usagi.core.prefs.AppSettings
import org.draken.usagi.core.ui.BaseViewModel
import org.draken.usagi.core.ui.util.ReversibleAction
import org.draken.usagi.core.util.ext.MutableEventFlow
import org.draken.usagi.core.util.ext.call
import org.draken.usagi.core.util.ext.mapSortedByCount
import org.draken.usagi.explore.data.MangaSourcesRepository
import org.draken.usagi.explore.data.SourcesSortOrder
import org.draken.usagi.list.ui.model.ListModel
import org.draken.usagi.list.ui.model.LoadingState
import org.draken.usagi.settings.sources.manage.plugins.TachiyomiCatalogRepository
import tsuki.model.ContentType
import tsuki.model.MangaSource
import java.util.EnumSet
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SourcesCatalogViewModel
	@Inject
	constructor(
		private val repository: MangaSourcesRepository,
		db: MangaDatabase,
		private val settings: AppSettings,
		private val tachiyomiCatalogRepository: TachiyomiCatalogRepository,
		private val tachiyomiRuntime: TachiyomiRuntime,
	) : BaseViewModel() {
		val onActionDone = MutableEventFlow<ReversibleAction>()

		private val externalRepositoryUrl = MutableStateFlow<String?>(null)
		private val externalArtifacts = MutableStateFlow<List<TachiyomiExtensionArtifact>>(emptyList())
		private val externalLoading = MutableStateFlow(false)
		private val searchQuery = MutableStateFlow<String?>(null)
		private val appliedFilter =
			MutableStateFlow(
				SourcesCatalogFilter(
					types = emptySet(),
					locale = Locale.getDefault().language.takeIf { it in locales },
					isNewOnly = false,
					plugin = null,
				),
			)

		val isExternalCatalog: Boolean
			get() = externalRepositoryUrl.value != null

		val externalCatalogTitle: String?
			get() = externalRepositoryUrl.value?.let { tachiyomiCatalogRepository.savedRepositories().firstOrNull { item -> item.url == it }?.title }

		val locales: Set<String?>
			get() =
				if (isExternalCatalog) {
					buildSet {
						externalArtifacts.value.flatMapTo(this) { artifact -> artifact.sources.map { normalizeLanguage(it.language) } }
						add(null)
					}
				} else {
					repository.allMangaSources.mapTo(HashSet<String?>()) { it.locale }.also { it.add(null) }
				}

		private val hasNativeNewSources =
			repository.observeHasNewSources().stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Lazily, false)
		val hasNewSources: StateFlow<Boolean> =
			combine(hasNativeNewSources, externalRepositoryUrl) { hasNew, url -> hasNew && url == null }
				.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, false)

		val plugins: List<String>
			get() =
				if (isExternalCatalog) {
					emptyList()
				} else {
					repository.allMangaSources
						.mapNotNullTo(HashSet()) {
							(
								it as? org.draken.usagi.core.model.PluginMangaSource
									?: (it as? MangaSourceInfo)?.mangaSource as? org.draken.usagi.core.model.PluginMangaSource
							)?.jarName
						}.sorted()
				}

		private val contentTypes = MutableStateFlow<List<ContentType>>(emptyList())

		val uiState: StateFlow<SourcesCatalogUiState> =
			combine(appliedFilter, hasNewSources, contentTypes, ::SourcesCatalogUiState)
				.stateIn(
					viewModelScope + Dispatchers.Default,
					SharingStarted.Eagerly,
					SourcesCatalogUiState(
						appliedFilter = appliedFilter.value,
						hasNewSources = hasNewSources.value,
						contentTypes = contentTypes.value,
					),
				)

		val content: StateFlow<List<ListModel>> =
			combine(
				combine(searchQuery, appliedFilter, externalRepositoryUrl, externalArtifacts, externalLoading) { query, filter, url, artifacts, loading ->
					if (url != null) {
						if (loading) listOf(LoadingState) else buildExternalSourcesList(filter, query, artifacts)
					} else {
						buildSourcesList(filter, query)
					}
				},
				db.invalidationTracker.createFlow(TABLE_SOURCES),
			) { items, _ -> items }
				.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, listOf(LoadingState))

		init {
			repository.clearNewSourcesBadge()
			launchJob(Dispatchers.Default) {
				contentTypes.value = getNativeContentTypes(settings.isNsfwContentDisabled)
			}
		}

		fun openExternalRepository(url: String) {
			val normalized = url.trim()
			if (normalized.isBlank()) return
			externalRepositoryUrl.value = normalized
			externalLoading.value = true
			launchJob(Dispatchers.IO) {
				val cached = tachiyomiCatalogRepository.loadCached(normalized)
				externalArtifacts.value = cached
				contentTypes.value = getExternalContentTypes(cached)
				runCatching { tachiyomiCatalogRepository.load(normalized) }
					.onSuccess { artifacts ->
						externalArtifacts.value = artifacts
						contentTypes.value = getExternalContentTypes(artifacts)
					}
				externalLoading.value = false
			}
		}

		fun performSearch(query: String?) {
			searchQuery.value = query?.trim()
		}

		fun setLocale(value: String?) {
			appliedFilter.value = appliedFilter.value.copy(locale = value)
		}

		fun addSource(source: MangaSource) {
			launchJob(Dispatchers.Default) {
				val rollback = repository.setSourcesEnabled(setOf(source), true)
				onActionDone.call(ReversibleAction(R.string.source_enabled, rollback))
			}
		}

		suspend fun installTachiyomi(item: SourceCatalogItem.Tachiyomi): Boolean =
			tachiyomiRuntime.install(item.artifact).also { success ->
				if (success) externalArtifacts.value = externalArtifacts.value.toList()
			}

		fun getImportedTachiyomiSource(item: SourceCatalogItem.Tachiyomi): MangaSource? = tachiyomiRuntime.getSourceById(item.source.id)

		fun setContentType(
			value: ContentType,
			isAdd: Boolean,
		) {
			val filter = appliedFilter.value
			val types = EnumSet.noneOf(ContentType::class.java)
			types.addAll(filter.types)
			if (isAdd) types.add(value) else types.remove(value)
			appliedFilter.value = filter.copy(types = types)
		}

		fun setNewOnly(value: Boolean) {
			appliedFilter.value = appliedFilter.value.copy(isNewOnly = value)
		}

		fun setPlugin(value: String?) {
			appliedFilter.value = appliedFilter.value.copy(plugin = value)
		}

		private suspend fun buildSourcesList(
			filter: SourcesCatalogFilter,
			query: String?,
		): List<SourceCatalogItem> {
			val sources =
				repository.queryParserSources(
					isDisabledOnly = true,
					isNewOnly = filter.isNewOnly,
					excludeBroken = false,
					types = filter.types,
					query = query,
					locale = filter.locale,
					plugin = filter.plugin,
					sortOrder = SourcesSortOrder.ALPHABETIC,
				)
			return emptyState(sources.map { SourceCatalogItem.Source(it) }, query)
		}

		@WorkerThread
		private fun buildExternalSourcesList(
			filter: SourcesCatalogFilter,
			query: String?,
			artifacts: List<TachiyomiExtensionArtifact>,
		): List<SourceCatalogItem> {
			val installed = tachiyomiRuntime.directInstalled.value.associateBy { it.packageName }
			val sources =
				artifacts
					.asSequence()
					.filter { artifact -> filter.plugin == null || artifact.repositoryUrl == filter.plugin }
					.flatMap { artifact ->
						artifact.sources.asSequence().mapNotNull { source ->
							if (settings.isNsfwContentDisabled && source.contentType == ContentType.HENTAI) return@mapNotNull null
							if (!matchesTachiyomiCatalogSource(source, artifact, query, filter.locale, filter.types)) return@mapNotNull null
							SourceCatalogItem.Tachiyomi(source, artifact, installed[artifact.packageName])
						}
					}.sortedBy { it.displayName.lowercase(Locale.ROOT) }
					.toList()
			return emptyState(sources, query)
		}

		private fun emptyState(
			sources: List<SourceCatalogItem>,
			query: String?,
		): List<SourceCatalogItem> =
			if (sources.isNotEmpty()) {
				sources
			} else {
				listOf(
					if (query == null) {
						SourceCatalogItem.Hint(R.drawable.ic_empty_feed, R.string.no_manga_sources, R.string.no_manga_sources_catalog_text)
					} else {
						SourceCatalogItem.Hint(R.drawable.ic_empty_feed, R.string.nothing_found, R.string.no_manga_sources_found)
					},
				)
			}

		@WorkerThread
		private fun getNativeContentTypes(isNsfwDisabled: Boolean): List<ContentType> {
			val result = repository.allMangaSources.mapSortedByCount { it.contentType }
			return if (isNsfwDisabled) result.filterNot { it == ContentType.HENTAI } else result
		}

		private fun getExternalContentTypes(artifacts: List<TachiyomiExtensionArtifact>): List<ContentType> =
			artifacts
				.flatMap { it.sources }
				.map { it.contentType }
				.filterNot { settings.isNsfwContentDisabled && it == ContentType.HENTAI }
				.distinct()
				.sortedBy { it.ordinal }
	}
