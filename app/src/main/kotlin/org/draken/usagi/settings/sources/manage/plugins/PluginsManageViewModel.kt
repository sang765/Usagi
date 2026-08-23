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
		private val settings: AppSettings,
	) : BaseViewModel() {
		val content = MutableStateFlow<List<PluginManageItem>>(emptyList())
		val selectedPlugins = MutableStateFlow<Set<String>>(emptySet())

		@Volatile
		private var pluginsSnapshot = emptyList<PluginManageItem.Plugin>()

		@Volatile
		private var query = ""

		init {
			refresh()
		}

		fun refresh() {
			launchLoadingJob(Dispatchers.Default) {
				val localPlugins = loadPluginsLocal()
				pluginsSnapshot = localPlugins
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

		fun toggleSelection(jarName: String) {
			val current = selectedPlugins.value
			selectedPlugins.value = if (jarName in current) current - jarName else current + jarName
		}

		fun clearSelection() {
			selectedPlugins.value = emptySet()
		}

		fun isSelected(jarName: String): Boolean = jarName in selectedPlugins.value

		suspend fun delete(): Boolean =
			pluginSideloadUseCase.delete(selectedPlugins.value).also {
				if (it) {
					selectedPlugins.value = emptySet()
					refresh()
				}
			}

		suspend fun rename(
			item: PluginManageItem.Plugin,
			newRawName: String,
		): Boolean = pluginSideloadUseCase.rename(item.name, newRawName).also { if (it) refresh() }

		fun isInstalled(fileName: String): Boolean = pluginSideloadUseCase.isInstalled(fileName)

		private fun publishFiltered() {
			val all = pluginsSnapshot
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
				all.filter { plugin ->
					plugin.name.contains(q, true) ||
						plugin.repository?.contains(q, true) == true
				}
			content.value =
				filtered.ifEmpty {
					listOf(PluginManageItem.Placeholder(titleResId = R.string.nothing_found, summaryResId = null))
				}
		}

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
