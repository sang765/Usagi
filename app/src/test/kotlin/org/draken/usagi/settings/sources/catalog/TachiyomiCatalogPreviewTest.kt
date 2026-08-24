package org.draken.usagi.settings.sources.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TachiyomiCatalogPreviewTest {
	@Test
	fun `uninstalled extension is marked for preview cleanup`() {
		assertEquals("example.extension", previewPackageName("example.extension", emptyList()))
	}

	@Test
	fun `installed extension is not marked as preview`() {
		assertNull(previewPackageName("example.extension", listOf("example.extension")))
	}
}
