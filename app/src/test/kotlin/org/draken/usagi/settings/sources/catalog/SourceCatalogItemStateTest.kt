package org.draken.usagi.settings.sources.catalog

import org.draken.tsukimix.core.parser.tachiyomi.DirectTachiyomiInstalled
import org.draken.tsukimix.core.parser.tachiyomi.TachiyomiCatalogSource
import org.draken.tsukimix.core.parser.tachiyomi.TachiyomiExtensionArtifact
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tsuki.model.ContentType

class SourceCatalogItemStateTest {
	@Test
	fun `installed state changes row contents without changing row identity`() {
		val source = TachiyomiCatalogSource(1L, "Example", "en", "https://example.com")
		val artifact =
			TachiyomiExtensionArtifact(
				repositoryUrl = "https://example.com/repository.json",
				name = "Example extension",
				packageName = "example.extension",
				jarUrl = "https://example.com/example.jar",
				apkUrl = "https://example.com/example.apk",
				iconUrl = null,
				extensionLib = 1.5,
				versionCode = 1L,
				versionName = "1.0",
				sources = listOf(source),
			)
		val installed =
			DirectTachiyomiInstalled(
				packageName = artifact.packageName,
				name = artifact.name,
				repositoryUrl = artifact.repositoryUrl,
				jarUrl = artifact.jarUrl,
				apkUrl = artifact.apkUrl,
				iconUrl = artifact.iconUrl,
				versionCode = 1L,
				versionName = "1.0",
				libVersion = 1.5,
				contentType = ContentType.MANGA,
				sources = listOf(source),
			)

		val notInstalledItem = SourceCatalogItem.Tachiyomi(source, artifact, null)
		val installedItem = SourceCatalogItem.Tachiyomi(source, artifact, installed)

		assertTrue(notInstalledItem.areItemsTheSame(installedItem))
		assertNotEquals(notInstalledItem, installedItem)
		assertFalse(notInstalledItem.isInstalled)
		assertTrue(installedItem.isInstalled)
		assertTrue(notInstalledItem.canInstallApk)
		assertFalse(installedItem.canInstallApk)
		assertFalse(SourceCatalogItem.Tachiyomi(source, artifact, null, isLegacyInstalled = true).canInstallApk)
	}

	@Test
	fun `loading state changes row contents without changing row identity`() {
		val source = TachiyomiCatalogSource(1L, "Example", "en", "https://example.com")
		val artifact =
			TachiyomiExtensionArtifact(
				repositoryUrl = "https://example.com/repository.json",
				name = "Example extension",
				packageName = "example.extension",
				jarUrl = "https://example.com/example.jar",
				apkUrl = "https://example.com/example.apk",
				iconUrl = null,
				extensionLib = 1.5,
				versionCode = 1L,
				versionName = "1.0",
				sources = listOf(source),
			)
		val idleItem = SourceCatalogItem.Tachiyomi(source, artifact, null)
		val loadingItem = idleItem.copy(isLoading = true)

		assertTrue(idleItem.areItemsTheSame(loadingItem))
		assertNotEquals(idleItem, loadingItem)
		assertFalse(idleItem.isLoading)
		assertTrue(loadingItem.isLoading)
	}
}
