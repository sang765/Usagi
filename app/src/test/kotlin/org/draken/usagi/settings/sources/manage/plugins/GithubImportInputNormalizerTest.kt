package org.draken.usagi.settings.sources.manage.plugins

import org.junit.Assert.assertEquals
import org.junit.Test

class GithubImportInputNormalizerTest {
	@Test
	fun `converts spaces between repository parts to slash`() {
		assertEquals("user/repo", normalizeGithubImportInput("user repo"))
	}

	@Test
	fun `removes trailing spaces after converting repository parts`() {
		assertEquals("user/repo", normalizeGithubImportInput("user repo "))
	}

	@Test
	fun `collapses multiple spaces and preserves url`() {
		assertEquals("user/repo", normalizeGithubImportInput("user   repo"))
		assertEquals("https://github.com/user/repo", normalizeGithubImportInput("https://github.com/user/repo"))
	}
}
