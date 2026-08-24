package org.draken.usagi.settings.sources.catalog

internal fun tachiyomiArtifactSuffix(
	sourceName: String,
	artifactName: String,
): String? = artifactName.takeUnless { it.equals(sourceName, ignoreCase = true) }
