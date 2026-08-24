package org.draken.usagi.settings.sources.catalog

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TachiyomiCatalogActionTest {
	@Test
	fun `uninstalled extension is installed`() {
		assertFalse(shouldRemoveTachiyomiOnToggle(isInstalled = false, hasUpdate = false))
	}

	@Test
	fun `installed extension with update is updated`() {
		assertFalse(shouldRemoveTachiyomiOnToggle(isInstalled = true, hasUpdate = true))
	}

	@Test
	fun `installed extension without update is removed`() {
		assertTrue(shouldRemoveTachiyomiOnToggle(isInstalled = true, hasUpdate = false))
	}
}
