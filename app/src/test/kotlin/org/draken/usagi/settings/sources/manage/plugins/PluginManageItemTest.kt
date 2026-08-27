package org.draken.usagi.settings.sources.manage.plugins

import org.draken.usagi.settings.sources.manage.plugins.model.PluginManageItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PluginManageItemTest {
	@Test
	fun `online plugin repository gets github owner avatar`() {
		val item = PluginManageItem.Plugin("online.jar", "FiorenMas/mihon-extensions", "v1", null)

		assertEquals("https://github.com/FiorenMas.png", item.iconUrl)
	}

	@Test
	fun `local plugin without persisted repository keeps generic icon`() {
		val item = PluginManageItem.Plugin("local.jar", null, null, null)

		assertNull(item.iconUrl)
	}

	@Test
	fun `external repository gets github owner avatar`() {
		val item =
			PluginManageItem.ExternalRepository(
				url = "https://raw.githubusercontent.com/FiorenMas/mihon-extensions/repo/index.json",
				repository = "FiorenMas/mihon-extensions",
				title = "Fioren",
				path = "index.json",
			)

		assertEquals("https://github.com/FiorenMas.png", item.iconUrl)
	}
}
