package org.draken.usagi.settings.sources.manage.plugins

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

internal object TachiyomiExtensionJsonParser {
	fun parse(
		repository: String,
		indexUrl: String,
		raw: String,
	): List<TachiyomiExtensionMetadata> =
		runCatching {
			val root = JSONTokener(raw).nextValue()
			val entries =
				when (root) {
					is JSONObject -> {
						root.optJSONObject("extensionList")?.optJSONArray("extensions")
							?: root.optJSONArray("extensions")
					}

					is JSONArray -> {
						root
					}

					else -> {
						null
					}
				}
					?: return emptyList()
			val repositoryName = (root as? JSONObject)?.optString("name").orEmpty()

			buildList {
				for (index in 0 until entries.length()) {
					val entry = entries.optJSONObject(index) ?: continue
					val resources = entry.optJSONObject("resources")
					val name = entry.optString("name").ifBlank { entry.optString("pkg") }
					val packageName =
						entry.optString("packageName").ifBlank { entry.optString("pkg") }
					if (name.isBlank() && packageName.isBlank()) continue
					add(
						TachiyomiExtensionMetadata(
							id = packageName.ifBlank { name },
							name = name.ifBlank { packageName },
							packageName = packageName,
							versionName =
								entry.optString("versionName").ifBlank {
									entry.optString("version").ifBlank { null }
								},
							versionCode =
								entry.optString("versionCode").ifBlank {
									entry.optString("code").ifBlank { null }
								},
							languages = languages(entry),
							contentWarning =
								entry.optString("contentWarning").ifBlank {
									if (entry.optInt("nsfw", 0) != 0) "NSFW" else null
								},
							iconUrl = resources?.optString("iconUrl").takeUnless { it.isNullOrBlank() },
							apkUrl =
								resources?.optString("apkUrl").takeUnless { it.isNullOrBlank() }
									?: entry.optString("apk").takeUnless { it.isNullOrBlank() },
							jarUrl = resources?.optString("jarUrl").takeUnless { it.isNullOrBlank() },
							repository = repository,
							indexUrl = indexUrl,
							repositoryName = repositoryName,
						),
					)
				}
			}.distinctBy { it.id }
		}.getOrDefault(emptyList())

	private fun languages(entry: JSONObject): List<String> =
		buildList {
			entry
				.optString("language")
				.ifBlank { entry.optString("lang") }
				.takeIf { it.isNotBlank() }
				?.let(::add)
			val sources = entry.optJSONArray("sources") ?: return@buildList
			for (index in 0 until sources.length()) {
				val source = sources.optJSONObject(index) ?: continue
				val language = source.optString("language").ifBlank { source.optString("lang") }
				if (language.isNotBlank()) add(language)
			}
		}.distinct()
}

data class TachiyomiExtensionMetadata(
	val id: String,
	val name: String,
	val packageName: String,
	val versionName: String?,
	val versionCode: String?,
	val languages: List<String>,
	val contentWarning: String?,
	val iconUrl: String?,
	val apkUrl: String?,
	val jarUrl: String?,
	val repository: String,
	val indexUrl: String,
	val repositoryName: String,
)
