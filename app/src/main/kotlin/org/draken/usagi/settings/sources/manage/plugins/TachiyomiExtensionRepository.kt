package org.draken.usagi.settings.sources.manage.plugins

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.draken.usagi.core.network.BaseHttpClient
import org.json.JSONArray
import tsuki.util.await
import tsuki.util.runCatchingCancellable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TachiyomiExtensionRepository
	@Inject
	constructor(
		@BaseHttpClient private val httpClient: OkHttpClient,
		private val updatePluginsProvider: UpdatePluginsProvider,
	) {
		suspend fun findJsonFiles(input: String): List<TachiyomiIndexFile> =
			withContext(Dispatchers.IO) {
				val repository = updatePluginsProvider.resolve(input) ?: return@withContext emptyList()
				val (owner, repo) = updatePluginsProvider.splitRepository(repository) ?: return@withContext emptyList()
				val url =
					HttpUrl
						.Builder()
						.scheme("https")
						.host("api.github.com")
						.addPathSegments("repos/$owner/$repo/contents")
						.build()
				val request =
					Request
						.Builder()
						.get()
						.url(url)
						.header("Accept", "application/vnd.github+json")
						.build()
				runCatchingCancellable {
					httpClient.newCall(request).await().use { response ->
						if (!response.isSuccessful) return@use emptyList()
						parseFiles(repository, response.body.string())
					}
				}.getOrDefault(emptyList())
			}

		suspend fun loadIndex(file: TachiyomiIndexFile): List<TachiyomiExtensionMetadata> =
			withContext(Dispatchers.IO) {
				runCatchingCancellable {
					val request =
						Request
							.Builder()
							.get()
							.url(file.rawUrl)
							.build()
					httpClient.newCall(request).await().use { response ->
						if (!response.isSuccessful) return@use emptyList()
						TachiyomiExtensionJsonParser.parse(file.repository, file.rawUrl, response.body.string())
					}
				}.getOrDefault(emptyList())
			}

		private fun parseFiles(
			repository: String,
			raw: String,
		): List<TachiyomiIndexFile> =
			runCatching {
				val entries = JSONArray(raw)
				buildList {
					for (index in 0 until entries.length()) {
						val entry = entries.optJSONObject(index) ?: continue
						if (entry.optString("type") != "file") continue
						val path = entry.optString("path")
						val rawUrl = entry.optString("download_url")
						if (path.endsWith(".json", ignoreCase = true) && rawUrl.startsWith("https://")) {
							add(TachiyomiIndexFile(repository, path, rawUrl))
						}
					}
				}.take(MAX_INDEX_FILES)
			}.getOrDefault(emptyList())

		private companion object {
			const val MAX_INDEX_FILES = 32
		}
	}

data class TachiyomiIndexFile(
	val repository: String,
	val path: String,
	val rawUrl: String,
)
