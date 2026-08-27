package org.draken.usagi.settings.sources.manage.plugins

import org.junit.Assert.assertEquals
import org.junit.Test

class GithubImportStrategyTest {
	@Test
	fun `no releases use Tachiyomi catalog discovery`() {
		assertEquals(GithubImportKind.NO_RELEASES, githubImportKind(0))
	}

	@Test
	fun `one to three jar releases are Usagi plugins`() {
		assertEquals(GithubImportKind.USAGI_PLUGIN, githubImportKind(1))
		assertEquals(GithubImportKind.USAGI_PLUGIN, githubImportKind(3))
	}

	@Test
	fun `more than three jar releases are Tachiyomi repositories`() {
		assertEquals(GithubImportKind.TACHIYOMI_REPOSITORY, githubImportKind(4))
	}
}
