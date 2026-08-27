package org.draken.usagi.settings.sources.manage.plugins

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.draken.usagi.core.db.MangaDatabase
import org.draken.usagi.core.model.PluginKeyResolver
import org.draken.usagi.core.parser.MangaDynamicRepository
import org.draken.usagi.core.parser.PluginFileLoader
import org.draken.usagi.filter.data.SavedFiltersRepository
import tsuki.util.runCatchingCancellable
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the side effects of installing and reloading local Tsuki plugin JARs.
 * UI code supplies validated input and observes the returned result; it does
 * not manage files, classloaders, or registry normalization.
 */
@Singleton
class PluginSideloadUseCase
	@Inject
	constructor(
		@ApplicationContext private val context: Context,
		private val database: MangaDatabase,
		private val savedFiltersRepository: SavedFiltersRepository,
		private val mangaDynamicRepository: MangaDynamicRepository,
		private val pluginKeyResolver: PluginKeyResolver,
		private val updatePluginsProvider: UpdatePluginsProvider,
	) {
		suspend fun importFromUriResult(
			uri: Uri,
			fileName: String,
		): Result<Unit> =
			withContext(Dispatchers.IO) {
				val safeName = PluginFileLoader.resolve(fileName)
				runCatchingCancellable {
					val pluginsDir = mangaDynamicRepository.getDir()
					PluginFileLoader.copyFromUri(context, uri, File(pluginsDir, safeName))
					updatePluginsProvider.clearDto(safeName)
					reload(pluginsDir)
				}
			}

		suspend fun importFromUri(
			uri: Uri,
			fileName: String,
		): Boolean = importFromUriResult(uri, fileName).isSuccess

		suspend fun importFromGithubResult(
			release: ExternalPluginDto,
			fileName: String = release.fileName,
		): Result<Unit> =
			withContext(Dispatchers.IO) {
				updatePluginsProvider.installPluginResult(release, PluginFileLoader.resolve(fileName))
			}

		suspend fun importFromGithub(
			release: ExternalPluginDto,
			fileName: String = release.fileName,
		): Boolean = importFromGithubResult(release, fileName).isSuccess

		fun listInstalled(): List<String> = mangaDynamicRepository.get().sorted()

		fun isInstalled(fileName: String): Boolean = File(mangaDynamicRepository.getDir(), PluginFileLoader.resolve(fileName)).exists()

		suspend fun delete(fileNames: Set<String>): Boolean =
			withContext(Dispatchers.IO) {
				if (fileNames.isEmpty()) return@withContext false
				var allSuccess = true
				for (fileName in fileNames) {
					try {
						mangaDynamicRepository.delete(fileName)
						updatePluginsProvider.clearDto(fileName)
					} catch (_: Throwable) {
						allSuccess = false
					}
				}
				if (allSuccess) reload(mangaDynamicRepository.getDir())
				allSuccess
			}

		suspend fun rename(
			oldName: String,
			newRawName: String,
		): Boolean =
			withContext(Dispatchers.IO) {
				val newName = PluginFileLoader.resolve(newRawName)
				if (newName == oldName) return@withContext true
				val pluginsDir = mangaDynamicRepository.getDir()
				val old = File(pluginsDir, oldName)
				val new = File(pluginsDir, newName)
				if (new.exists()) return@withContext false
				runCatchingCancellable {
					if (old.exists() && old.renameTo(new)) {
						updatePluginsProvider.renameDto(oldName, newName)
						reload(pluginsDir)
						true
					} else {
						false
					}
				}.getOrDefault(false)
			}

		private suspend fun reload(pluginsDir: File) {
			mangaDynamicRepository.load(pluginsDir)
			pluginKeyResolver.normalize(database, savedFiltersRepository)
		}
	}
