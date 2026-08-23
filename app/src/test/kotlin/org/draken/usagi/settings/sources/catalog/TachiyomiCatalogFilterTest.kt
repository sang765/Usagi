package org.draken.usagi.settings.sources.catalog

import org.draken.tsukimix.core.parser.tachiyomi.TachiyomiCatalogSource
import org.draken.tsukimix.core.parser.tachiyomi.TachiyomiExtensionArtifact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tsuki.model.ContentType

class TachiyomiCatalogFilterTest {
	private val source = TachiyomiCatalogSource(1L, "MangaDex", "en-US", "https://mangadex.org", ContentType.MANGA)
	private val artifact =
		TachiyomiExtensionArtifact(
			"https://raw.example/index.json",
			"English sources",
			"eu.kanade.extension.en",
			"https://example/source.jar",
			null,
			null,
			1.0,
			1L,
			"1.0.0",
			ContentType.MANGA,
			listOf(source),
		)

	@Test
	fun `language normalization ignores region and separator`() {
		assertEquals("pt", normalizeTachiyomiLanguage("pt_BR"))
		assertEquals("en", normalizeTachiyomiLanguage("en-US"))
	}

	@Test
	fun `matching applies language type and query`() {
		assertTrue(matchesTachiyomiCatalogSource(source, artifact, "mangadex", "en", setOf(ContentType.MANGA)))
		assertFalse(matchesTachiyomiCatalogSource(source, artifact, "mangadex", "vi", setOf(ContentType.MANGA)))
		assertFalse(matchesTachiyomiCatalogSource(source, artifact, "mangadex", "en", setOf(ContentType.HENTAI)))
		assertFalse(matchesTachiyomiCatalogSource(source, artifact, "other", "en", setOf(ContentType.MANGA)))
	}
}
