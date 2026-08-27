package org.draken.usagi.settings.sources.manage.plugins

internal enum class GithubImportKind {
	NO_RELEASES,
	USAGI_PLUGIN,
	TACHIYOMI_REPOSITORY,
}

internal fun githubImportKind(jarReleaseCount: Int): GithubImportKind =
	when {
		jarReleaseCount > 3 -> GithubImportKind.TACHIYOMI_REPOSITORY
		jarReleaseCount > 0 -> GithubImportKind.USAGI_PLUGIN
		else -> GithubImportKind.NO_RELEASES
	}
