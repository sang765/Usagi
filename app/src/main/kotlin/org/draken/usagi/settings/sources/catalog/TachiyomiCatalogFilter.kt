package org.draken.usagi.settings.sources.catalog

import org.draken.tsukimix.core.parser.tachiyomi.TachiyomiCatalogSource
import org.draken.tsukimix.core.parser.tachiyomi.TachiyomiExtensionArtifact
import tsuki.model.ContentType
import java.util.Locale

internal fun normalizeTachiyomiLanguage(value: String): String =
	value
		.trim()
		.replace('_', '-')
		.substringBefore('-')
		.lowercase(Locale.ROOT)

internal fun matchesTachiyomiCatalogSource(
	source: TachiyomiCatalogSource,
	artifact: TachiyomiExtensionArtifact,
	query: String?,
	locale: String?,
	types: Set<ContentType>,
): Boolean {
	if (locale != null && normalizeTachiyomiLanguage(source.language) != normalizeTachiyomiLanguage(locale)) return false
	if (types.isNotEmpty() && source.contentType !in types) return false
	if (!query.isNullOrBlank() && !source.name.contains(query, true) && !artifact.name.contains(query, true) && !artifact.packageName.contains(query, true)) return false
	return true
}
