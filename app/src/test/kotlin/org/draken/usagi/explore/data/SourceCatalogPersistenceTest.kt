package org.draken.usagi.explore.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceCatalogPersistenceTest {
	@Test
	fun `native sources are persisted`() {
		assertTrue(shouldPersistInSourcesCatalog(isExternalSource = false, isDirectTachiyomiSource = false))
	}

	@Test
	fun `legacy external providers stay outside native catalog`() {
		assertFalse(shouldPersistInSourcesCatalog(isExternalSource = true, isDirectTachiyomiSource = false))
	}

	@Test
	fun `installed direct tachiyomi sources are persisted`() {
		assertTrue(shouldPersistInSourcesCatalog(isExternalSource = true, isDirectTachiyomiSource = true))
	}
}
