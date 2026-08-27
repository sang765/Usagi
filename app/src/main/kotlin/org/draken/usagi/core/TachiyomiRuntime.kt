package org.draken.usagi.core

import dagger.Lazy
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.draken.tsukimix.core.parser.external.model.MangaResult
import org.draken.tsukimix.core.parser.tachiyomi.DirectTachiyomiExtensionManager
import org.draken.tsukimix.core.parser.tachiyomi.DirectTachiyomiInstalled
import org.draken.tsukimix.core.parser.tachiyomi.TachiyomiExtensionArtifact
import org.draken.tsukimix.core.parser.tachiyomi.TachiyomiExtensionManager
import org.draken.tsukimix.core.parser.tachiyomi.model.TachiyomiMangaSource
import org.draken.usagi.core.model.MangaSourceRegistry
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** Application-scoped boundary for both installed and direct Tachiyomi runtimes. */
@Singleton
class TachiyomiRuntime
	@Inject
	constructor(
		private val manager: TachiyomiExtensionManager,
		private val directManagerProvider: Lazy<DirectTachiyomiExtensionManager>,
	) {
		private var directManagerReady = false
		private val directSourceNames = ConcurrentHashMap.newKeySet<String>()

		private val directManager: DirectTachiyomiExtensionManager
			get() {
				directManagerReady = true
				return directManagerProvider.get()
			}

		val installedExtensions: StateFlow<List<MangaResult.Success>>
			get() = manager.installedExtensions

		val failedExtensions: StateFlow<List<MangaResult.Error>>
			get() = manager.failedExtensions

		val isLoading: StateFlow<Boolean>
			get() = manager.isLoading

		val isReady: StateFlow<Boolean>
			get() = manager.isReady

		val sources: StateFlow<List<TachiyomiMangaSource>>
			get() = manager.sources

		val directSources: StateFlow<List<TachiyomiMangaSource>>
			get() = directManager.sources

		val directInstalled: StateFlow<List<DirectTachiyomiInstalled>>
			get() = directManager.installed

		val directPluginNames: Map<String, String>
			get() =
				directInstalled.value.associate { installed ->
					installed.packageName to tachiyomiRepositoryPluginName(installed.repositoryUrl, installed.name)
				}

		suspend fun ensureReady(forceRefresh: Boolean = false) {
			manager.ensureReady(forceRefresh)
			ensureDirectReady(forceRefresh)
		}

		suspend fun ensureDirectReady(forceRefresh: Boolean = false) {
			directManager.ensureReady(forceRefresh)
			publishActiveSources()
		}

		/** Keeps the legacy installed-extension runtime synchronized for the application lifetime. */
		suspend fun start(): Nothing =
			coroutineScope {
				launch {
					manager.sources.collectLatest { publishActiveSources() }
				}
				manager.ensureReady()
				awaitCancellation()
			}

		fun getSourceById(sourceId: Long): TachiyomiMangaSource? = directManager.getSourceById(sourceId) ?: manager.getSourceById(sourceId)

		fun getSourceByName(name: String): TachiyomiMangaSource? = directManager.getSourceByName(name) ?: manager.getSourceByName(name)

		fun resolve(source: TachiyomiMangaSource): TachiyomiMangaSource = if (directManager.owns(source)) directManager.resolve(source) else manager.resolve(source)

		suspend fun install(artifact: TachiyomiExtensionArtifact): Boolean {
			val success = directManager.install(artifact)
			if (success) ensureDirectReady(forceRefresh = true)
			return success
		}

		suspend fun remove(packageName: String): Boolean {
			val success = directManager.remove(packageName)
			if (success) ensureDirectReady(forceRefresh = true)
			return success
		}

		private fun publishActiveSources() {
			val nativeSources = MangaSourceRegistry.sources.filterNot { it is TachiyomiMangaSource }
			val directSources = if (directManagerReady) directManager.getActiveSources() else emptyList()
			directSourceNames.clear()
			directSourceNames.addAll(directSources.map { it.name })
			val tachiyomiSources = (manager.getActiveSources() + directSources).distinctBy { it.name }
			MangaSourceRegistry.publish(nativeSources + tachiyomiSources)
		}

		fun isDirectSource(sourceName: String): Boolean = sourceName in directSourceNames

		fun isDirectPackage(packageName: String): Boolean = directInstalled.value.any { it.packageName == packageName }

		fun isLegacyApkPackage(packageName: String): Boolean =
			isLegacyTachiyomiPackage(
				packageName,
				directInstalled.value.map { it.packageName }.toSet(),
				installedExtensions.value.map { it.pkgName }.toSet(),
			)
	}

internal fun isLegacyTachiyomiPackage(
	packageName: String,
	directPackages: Set<String>,
	legacyPackages: Set<String>,
): Boolean = packageName !in directPackages && packageName in legacyPackages

internal fun tachiyomiRepositoryPluginName(
	repositoryUrl: String,
	fallback: String,
): String {
	val value = repositoryUrl.trim()
	val withoutScheme = value.substringAfter("://", value)
	val authority = withoutScheme.substringBefore('/').lowercase()
	val path = withoutScheme.substringAfter('/', "").split('/').filter { it.isNotBlank() }
	val owner =
		when {
			authority == "raw.githubusercontent.com" || authority == "github.com" -> path.firstOrNull()
			authority == "cdn.jsdelivr.net" && path.firstOrNull().equals("gh", true) -> path.getOrNull(1)
			else -> null
		}
	return owner?.takeIf { it.isNotBlank() } ?: fallback
}
