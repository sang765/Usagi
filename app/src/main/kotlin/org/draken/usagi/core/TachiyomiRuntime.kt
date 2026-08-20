package org.draken.usagi.core

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import org.draken.tsukimix.core.parser.tachiyomi.TachiyomiExtensionManager
import org.draken.tsukimix.core.parser.tachiyomi.model.TachiyomiLoadResult
import org.draken.tsukimix.core.parser.tachiyomi.model.TachiyomiMangaSource
import org.draken.usagi.core.model.MangaSourceRegistry
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Application-scoped boundary for the Tachiyomi runtime.
 *
 * UI controllers should consume this facade through a ViewModel instead of
 * depending on the extension manager or publishing directly to the source
 * registry. The manager itself remains owned by the application DI graph.
 */
@Singleton
class TachiyomiRuntime
	@Inject
	constructor(
		private val manager: TachiyomiExtensionManager,
	) {
		val installedExtensions: StateFlow<List<TachiyomiLoadResult.Success>>
			get() = manager.installedExtensions

		val failedExtensions: StateFlow<List<TachiyomiLoadResult.Error>>
			get() = manager.failedExtensions

		val isLoading: StateFlow<Boolean>
			get() = manager.isLoading

		val isReady: StateFlow<Boolean>
			get() = manager.isReady

		val sources: StateFlow<List<TachiyomiMangaSource>>
			get() = manager.sources

		suspend fun ensureReady(forceRefresh: Boolean = false) {
			manager.ensureReady(forceRefresh)
			publishActiveSources()
		}

		/**
		 * Keeps the shared source registry synchronized for the application
		 * lifetime. The returned collection is cancelled with the caller's scope.
		 */
		suspend fun start() {
			manager.ensureReady()
			manager.sources.collectLatest { publishActiveSources() }
		}

		fun getSourceById(sourceId: Long): TachiyomiMangaSource? = manager.getSourceById(sourceId)

		fun resolve(source: TachiyomiMangaSource): TachiyomiMangaSource = manager.resolve(source)

		private fun publishActiveSources() {
			val nativeSources = MangaSourceRegistry.sources.filterNot { it is TachiyomiMangaSource }
			MangaSourceRegistry.publish(nativeSources + manager.getActiveSources())
		}
	}
