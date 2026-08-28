package org.draken.usagi.settings.sources.manage.plugins

import org.junit.Assert.assertEquals
import org.junit.Test

class TachiyomiIndexPriorityTest {
	@Test
	fun `index json is preferred first`() {
		assertEquals(0, tachiyomiIndexPriority("index.json"))
		assertEquals(0, tachiyomiIndexPriority("repo/index.json"))
	}

	@Test
	fun `min index json is preferred after index json`() {
		assertEquals(1, tachiyomiIndexPriority("min.index.json"))
		assertEquals(1, tachiyomiIndexPriority("repo/min.index.json"))
	}

	@Test
	fun `legacy minified index and other json are fallback candidates`() {
		assertEquals(2, tachiyomiIndexPriority("index.min.json"))
		assertEquals(2, tachiyomiIndexPriority("repo/index.min.json"))
		assertEquals(3, tachiyomiIndexPriority("catalogs/extensions.json"))
	}
}
