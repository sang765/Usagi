package org.draken.usagi.settings.sources.manage.plugins

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.draken.tsukimix.core.parser.tachiyomi.TachiyomiExtensionArtifact
import org.draken.tsukimix.core.parser.tachiyomi.TachiyomiExtensionCatalogProvider
import org.draken.usagi.core.network.BaseHttpClient
import org.json.JSONArray
import org.json.JSONObject
import tsuki.util.await
import java.io.File
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Repository boundary for imported Tachiyomi/Mihon catalog indexes. */
@Singleton
class TachiyomiCatalogRepository
	@Inject
	constructor(
		@ApplicationContext private val context: Context,
		@BaseHttpClient private val httpClient: OkHttpClient,
		private val catalogProvider: TachiyomiExtensionCatalogProvider,
	) {
		private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

		suspend fun discoverIndexFiles(input: String): List<TachiyomiIndexFile> =
			withContext(Dispatchers.IO) {
				runCatching {
					directIndexFile(input)?.let { return@runCatching listOf(it) }
					val repository = parseRepository(input) ?: return@runCatching emptyList()
					val repositoryJson = getJson("https://api.github.com/repos/${repository.owner}/${repository.name}")
					val branch = repositoryJson.optString("default_branch").ifBlank { "main" }
					val tree =
						getJson(
							"https://api.github.com/repos/${repository.owner}/${repository.name}/git/trees/${Uri.encode(branch)}?recursive=1",
						)
					val entries = tree.optJSONArray("tree") ?: JSONArray()
					buildList {
						for (index in 0 until entries.length()) {
							val entry = entries.optJSONObject(index) ?: continue
							val path = entry.optString("path")
							if (entry.optString("type") != "blob" || !path.endsWith(".json", ignoreCase = true)) continue
							val encodedPath = path.split('/').joinToString("/") { Uri.encode(it) }
							add(
								TachiyomiIndexFile(
									repository = "${repository.owner}/${repository.name}",
									title = repository.owner,
									path = path,
									url = "https://raw.githubusercontent.com/${repository.owner}/${repository.name}/${Uri.encode(branch)}/$encodedPath",
								),
							)
						}
					}.sortedBy { it.path.lowercase(Locale.ROOT) }
				}.getOrDefault(emptyList())
			}

		suspend fun importIndex(index: TachiyomiIndexFile): Boolean =
			withContext(Dispatchers.IO) {
				runCatching {
					val url =
						index.url.trim().takeIf { it.startsWith("http://") || it.startsWith("https://") }
							?: return@runCatching false
					val artifacts = catalogProvider.load(url)
					if (artifacts.isEmpty() && !catalogProvider.lastLoadError.isNullOrBlank()) {
						return@runCatching false
					}
					catalogProvider.saveRepository(url)
					catalogProvider.setRepositoryName(url, index.title)
					val repositories = savedRepositoryUrls().toMutableSet()
					repositories.add(url)
					preferences.edit {
						putStringSet(KEY_REPOSITORIES, repositories)
						putString("$KEY_PATH:$url", index.path)
						putString("$KEY_TITLE:$url", index.title)
					}
					true
				}.getOrDefault(false)
			}

		fun savedRepositories(): List<TachiyomiExternalRepository> =
			savedRepositoryUrls()
				.map { url ->
					TachiyomiExternalRepository(
						url = url,
						repository = repositoryLabel(url),
						title = preferences.getString("$KEY_TITLE:$url", catalogProvider.repositoryName(url)) ?: repositoryOwner(url),
						path = preferences.getString("$KEY_PATH:$url", repositoryPath(url)) ?: repositoryPath(url),
					)
				}.sortedBy { it.title.lowercase(Locale.ROOT) }

		suspend fun loadCached(url: String): List<TachiyomiExtensionArtifact> =
			withContext(Dispatchers.IO) {
				catalogProvider.loadSavedCached().filter { it.repositoryUrl == url }
			}

		suspend fun load(url: String): List<TachiyomiExtensionArtifact> =
			withContext(Dispatchers.IO) {
				catalogProvider.load(url)
			}

		suspend fun downloadApk(artifact: TachiyomiExtensionArtifact): File? =
			withContext(Dispatchers.IO) {
				val url = artifact.apkUrl?.trim()?.takeIf { it.startsWith("http://") || it.startsWith("https://") } ?: return@withContext null
				val target = File(context.cacheDir, "tachiyomi-${artifact.packageName}.apk")
				runCatching {
					val request = Request.Builder().url(url).build()
					httpClient.newCall(request).await().use { response ->
						if (!response.isSuccessful) throw IOException("APK download failed with HTTP ${response.code}")
						val body = response.body ?: throw IOException("APK response body is empty")
						target.outputStream().use { output -> body.byteStream().use { input -> input.copyTo(output) } }
					}
					target
				}.onFailure { target.delete() }.getOrNull()
			}

		fun renameRepository(
			url: String,
			title: String,
		): Boolean =
			runCatching {
				val value = title.trim()
				if (value.isBlank()) return false
				catalogProvider.setRepositoryName(url, value)
				preferences.edit { putString("$KEY_TITLE:$url", value) }
			}.isSuccess

		fun removeRepository(url: String): Boolean =
			runCatching {
				catalogProvider.removeRepository(url)
				preferences.edit {
					putStringSet(KEY_REPOSITORIES, savedRepositoryUrls() - url)
					remove("$KEY_TITLE:$url")
					remove("$KEY_PATH:$url")
				}
			}.isSuccess

		private suspend fun getJson(url: String): JSONObject {
			val request =
				Request
					.Builder()
					.url(url)
					.header("Accept", "application/vnd.github+json")
					.header("X-GitHub-Api-Version", "2022-11-28")
					.build()
			return httpClient.newCall(request).await().use { response ->
				if (!response.isSuccessful) throw IOException("GitHub returned HTTP ${response.code}")
				JSONObject(response.body?.string().orEmpty())
			}
		}

		private fun savedRepositoryUrls(): Set<String> = preferences.getStringSet(KEY_REPOSITORIES, emptySet()).orEmpty().mapNotNullTo(HashSet()) { it }

		private fun directIndexFile(input: String): TachiyomiIndexFile? {
			val value = input.trim()
			if (!value.startsWith("http://") && !value.startsWith("https://")) return null
			val uri = Uri.parse(value)
			val segments = uri.pathSegments
			val path = segments.lastOrNull()?.takeIf { it.endsWith(".json", ignoreCase = true) } ?: return null
			val normalizedUrl =
				if (uri.host.equals("github.com", ignoreCase = true) && segments.size >= 4 && segments[2].equals("blob", ignoreCase = true)) {
					"https://raw.githubusercontent.com/${segments[0]}/${segments[1]}/${segments.drop(3).joinToString("/")}"
				} else {
					value
				}
			val repository =
				segments
					.take(2)
					.filter { it.isNotBlank() }
					.joinToString("/")
					.ifBlank { uri.host.orEmpty() }
			return TachiyomiIndexFile(repository = repository, title = segments.firstOrNull().orEmpty().ifBlank { path.removeSuffix(".json") }, path = path, url = normalizedUrl)
		}

		private fun parseRepository(input: String): GithubRepository? {
			val value = input.trim().removeSuffix("/").removeSuffix(".git")
			val parts =
				if (value.startsWith("http://") || value.startsWith("https://")) {
					Uri.parse(value).pathSegments
				} else {
					value.split('/')
				}
			if (parts.size < 2 || parts[0].isBlank() || parts[1].isBlank()) return null
			return GithubRepository(parts[0], parts[1])
		}

		private fun repositoryOwner(url: String): String =
			Uri
				.parse(url)
				.pathSegments
				.firstOrNull()
				.orEmpty()
				.ifBlank { url }

		private fun repositoryLabel(url: String): String =
			Uri
				.parse(url)
				.pathSegments
				.take(2)
				.joinToString("/")
				.ifBlank { url }

		private fun repositoryPath(url: String): String =
			Uri
				.parse(url)
				.pathSegments
				.drop(2)
				.joinToString("/")
				.ifBlank { "index.json" }

		private data class GithubRepository(
			val owner: String,
			val name: String,
		)

		private companion object {
			const val PREFERENCES = "tachiyomi_catalog"
			const val KEY_REPOSITORIES = "repositories"
			const val KEY_TITLE = "title"
			const val KEY_PATH = "path"
		}
	}

data class TachiyomiIndexFile(
	val repository: String,
	val title: String,
	val path: String,
	val url: String,
)

data class TachiyomiExternalRepository(
	val url: String,
	val repository: String,
	val title: String,
	val path: String,
)
