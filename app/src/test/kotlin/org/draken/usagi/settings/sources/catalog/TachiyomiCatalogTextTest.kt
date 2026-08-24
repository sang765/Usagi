package org.draken.usagi.settings.sources.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TachiyomiCatalogTextTest {
	@Test
	fun `matching source and artifact names have no suffix`() {
		assertNull(tachiyomiArtifactSuffix("Akuma", "Akuma"))
		assertNull(tachiyomiArtifactSuffix("Akuma", "akuma"))
	}

	@Test
	fun `different source and artifact names keep suffix`() {
		assertEquals("Keiyoushi", tachiyomiArtifactSuffix("MangaDex", "Keiyoushi"))
	}
}
