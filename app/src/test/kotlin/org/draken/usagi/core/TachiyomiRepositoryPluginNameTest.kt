package org.draken.usagi.core

import org.junit.Assert.assertEquals
import org.junit.Test

class TachiyomiRepositoryPluginNameTest {
	@Test
	fun `raw github repository uses owner as plugin name`() {
		assertEquals(
			"FiorenMas",
			tachiyomiRepositoryPluginName(
				"https://raw.githubusercontent.com/FiorenMas/mihon-extensions/repo/index.json",
				"Mimi",
			),
		)
	}

	@Test
	fun `jsdelivr repository uses owner as plugin name`() {
		assertEquals(
			"Keiyoushi",
			tachiyomiRepositoryPluginName(
				"https://cdn.jsdelivr.net/gh/Keiyoushi/extensions@repo/index.json",
				"MangaDex",
			),
		)
	}

	@Test
	fun `unknown repository uses extension name fallback`() {
		assertEquals(
			"Mimi",
			tachiyomiRepositoryPluginName("https://example.com/catalog.json", "Mimi"),
		)
	}
}
