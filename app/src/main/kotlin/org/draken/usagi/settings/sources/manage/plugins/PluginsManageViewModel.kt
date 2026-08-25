package org.draken.usagi.settings.sources.manage.plugins

import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import org.draken.usagi.R
import org.draken.usagi.core.prefs.AppSettings
import org.draken.usagi.core.ui.BaseViewModel
import org.draken.usagi.settings.sources.manage.plugins.model.PluginManageItem
import javax.inject.Inject

@HiltViewModel
class PluginsManageViewModel
	@Inject
	constructor(
		private val updatePluginsProvider: UpdatePluginsProvider,
		private val pluginSideloadUseCase: PluginSideloadUseCase,
		private val tachiyomiCatalogRepository: TachiyomiCatalogRepository,
		private val settings: AppSettings,
	) : BaseViewModel() {
		val content = MutableStateFlow<List<PluginManageItem>>(emptyList())
		val selectedPlugins = MutableStateFlow<Set<String>>(emptySet())

		@Volatile
		private var pluginsSnapshot = emptyList<PluginManageItem.Plugin>()

		@Volatile
		private var externalRepositoriesSnapshot = emptyList<PluginManageItem.ExternalRepository>()

		@Volatile
		private var query = ""

		init {
			refresh()
		}

		fun refresh() {
			launchLoadingJob(Dispatchers.Default) {
				val localPlugins = loadPluginsLocal()
				pluginsSnapshot = localPlugins
				externalRepositoriesSnapshot = loadExternalRepositories()
				publishFiltered()

				if (localPlugins.isNotEmpty()) {
					val updatedPlugins =
						coroutineScope {
							localPlugins
								.map { plugin ->
									async {
										val repo = plugin.repository ?: return@async plugin
										val latest = updatePluginsProvider.requestTag(repo) ?: return@async plugin
										plugin.copy(latestTag = latest)
									}
								}.awaitAll()
						}
					pluginsSnapshot = updatedPlugins
					publishFiltered()
				}
			}
		}

		fun setQuery(value: String?) {
			query = value?.trim().orEmpty()
			publishFiltered()
		}

		fun runAutoUpdate() {
			if (settings.isAutoPluginsEnabled) {
				launchJob(Dispatchers.Default) {
					updatePluginsProvider.runAutoUpdate(settings)
				}
			}
		}

		suspend fun resolveRelease(
			input: String,
			name: String? = null,
		): ExternalPluginDto? =
			withContext(Dispatchers.Default) {
				val repository = updatePluginsProvider.resolve(input) ?: return@withContext null
				updatePluginsProvider.requestRelease(repository, name)
			}

		suspend fun resolveGithubReleases(input: String): List<ExternalPluginDto> =
			withContext(Dispatchers.Default) {
				val repository = updatePluginsProvider.resolve(input) ?: return@withContext emptyList()
				val tag = updatePluginsProvider.requestTag(repository) ?: return@withContext emptyList()
				updatePluginsProvider.requestPlugins(repository, tag)
			}

		suspend fun importFromUri(
			uri: Uri,
			fileName: String,
		): Boolean = pluginSideloadUseCase.importFromUri(uri, fileName).also { if (it) refresh() }

		suspend fun importFromGithub(
			release: ExternalPluginDto,
			fileName: String = release.fileName,
		): Boolean = pluginSideloadUseCase.importFromGithub(release, fileName).also { if (it) refresh() }

		suspend fun discoverTachiyomiIndexes(input: String): List<TachiyomiIndexFile> = withContext(Dispatchers.Default) { tachiyomiCatalogRepository.discoverIndexFiles(input) }

		suspend fun importTachiyomiIndex(index: TachiyomiIndexFile): Boolean = tachiyomiCatalogRepository.importIndex(index).also { if (it) refresh() }

		fun tachiyomiRepositories(): List<TachiyomiExternalRepository> = tachiyomiCatalogRepository.savedRepositories()

		suspend fun updatePlugin(item: PluginManageItem.Plugin): Boolean {
			val repository = item.repository ?: return false
			val release = resolveRelease(repository, item.name) ?: return false
			return if (release.tag == item.installedTag) {
				refresh()
				true
			} else {
				importFromGithub(release, item.name)
			}
		}

		fun toggleSelection(key: String) {
			val current = selectedPlugins.value
			selectedPlugins.value = if (key in current) current - key else current + key
		}

		fun toggleExternalSelection(url: String) = toggleSelection(url)

		fun clearSelection() {
			selectedPlugins.value = emptySet()
		}

		fun isSelected(jarName: String): Boolean = jarName in selectedPlugins.value

		fun isExternalSelected(url: String): Boolean = url in selectedPlugins.value

		suspend fun delete(): Boolean {
			val selected = selectedPlugins.value
			val pluginNames =
				pluginsSnapshot
					.asSequence()
					.map { it.name }
					.filter(selected::contains)
					.toSet()
			val repositoryUrls =
				externalRepositoriesSnapshot
					.asSequence()
					.map { it.url }
					.filter(selected::contains)
					.toSet()
			val pluginsDeleted = pluginNames.isEmpty() || pluginSideloadUseCase.delete(pluginNames)
			val repositoriesDeleted = repositoryUrls.all { tachiyomiCatalogRepository.removeRepository(it) }
			return (pluginsDeleted && repositoriesDeleted).also { success ->
				if (success) {
					selectedPlugins.value = emptySet()
					refresh()
				}
			}
		}

		suspend fun rename(
			item: PluginManageItem.Plugin,
			newRawName: String,
		): Boolean = pluginSideloadUseCase.rename(item.name, newRawName).also { if (it) refresh() }

		fun rename(
			item: PluginManageItem.ExternalRepository,
			newRawName: String,
		): Boolean = tachiyomiCatalogRepository.renameRepository(item.url, newRawName).also { if (it) refresh() }

		fun isInstalled(fileName: String): Boolean = pluginSideloadUseCase.isInstalled(fileName)

		private fun publishFiltered() {
			val all: List<PluginManageItem> = pluginsSnapshot + externalRepositoriesSnapshot
			if (all.isEmpty()) {
				content.value =
					listOf(
						PluginManageItem.Placeholder(
							titleResId = R.string.no_plugins,
							summaryResId = R.string.no_plugins_summary,
						),
					)
				return
			}
			val q = query
			if (q.isBlank()) {
				content.value = all
				return
			}
			val filtered =
				all.filter { item ->
					when (item) {
						is PluginManageItem.Plugin -> {
							item.name.contains(q, true) || item.repository?.contains(q, true) == true
						}

						is PluginManageItem.ExternalRepository -> {
							item.title.contains(q, true) || item.path.contains(q, true) || item.url.contains(q, true)
						}

						is PluginManageItem.Placeholder -> {
							false
						}
					}
				}
			content.value =
				filtered.ifEmpty {
					listOf(PluginManageItem.Placeholder(titleResId = R.string.nothing_found, summaryResId = null))
				}
		}

		private fun loadExternalRepositories(): List<PluginManageItem.ExternalRepository> =
			tachiyomiCatalogRepository
				.savedRepositories()
				.map { PluginManageItem.ExternalRepository(url = it.url, repository = it.repository, title = it.title, path = it.path) }

		private fun loadPluginsLocal(): List<PluginManageItem.Plugin> {
			val plugins = pluginSideloadUseCase.listInstalled()
			if (plugins.isEmpty()) return emptyList()
			val meta = updatePluginsProvider.readAndCleanDto(plugins.toSet())

			return plugins.map { fileName ->
				val itemMeta = meta[fileName]
				PluginManageItem.Plugin(
					name = fileName,
					repository = itemMeta?.repository,
					installedTag = itemMeta?.tag,
					latestTag = null,
				)
			}
		}
	}
