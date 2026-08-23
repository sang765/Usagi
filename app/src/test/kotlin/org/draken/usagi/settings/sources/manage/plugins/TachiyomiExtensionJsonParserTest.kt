package org.draken.usagi.settings.sources.manage.plugins

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TachiyomiExtensionJsonParserTest {
	@Test
	fun parsesFullIndexMetadata() {
		val raw =
			"""
			{
			  "name": "Example repo",
			  "extensionList": {
			    "extensions": [
			      {
			        "name": "Example",
			        "packageName": "eu.example.extension",
			        "versionName": "1.2.3",
			        "versionCode": "123",
			        "contentWarning": "CONTENT_WARNING_SAFE",
			        "resources": {
			          "jarUrl": "https://example.test/example.jar"
			        },
			        "sources": [
			          { "language": "en" },
			          { "language": "vi" }
			        ]
			      }
			    ]
			  }
			}
			""".trimIndent()

		val result = TachiyomiExtensionJsonParser.parse("owner/repository", "https://raw.example/index.json", raw)

		assertEquals(1, result.size)
		assertEquals("Example", result.single().name)
		assertEquals("eu.example.extension", result.single().packageName)
		assertEquals(listOf("en", "vi"), result.single().languages)
		assertEquals("Example repo", result.single().repositoryName)
	}

	@Test
	fun parsesCompactIndexMetadataWithoutJarUrl() {
		val raw =
			"""
			[
			  {
			    "name": "Compact",
			    "pkg": "eu.compact.extension",
			    "version": "2.0.0",
			    "code": 20,
			    "lang": "all",
			    "apk": "compact.apk",
			    "sources": [{ "lang": "vi" }]
			  }
			]
			""".trimIndent()

		val result = TachiyomiExtensionJsonParser.parse("owner/repository", "https://raw.example/index.min.json", raw)

		assertEquals(1, result.size)
		assertEquals("Compact", result.single().name)
		assertEquals("eu.compact.extension", result.single().packageName)
		assertEquals("2.0.0", result.single().versionName)
		assertEquals(listOf("all", "vi"), result.single().languages)
		assertTrue(result.single().jarUrl == null)
	}

	@Test
	fun malformedJsonReturnsEmptyList() {
		assertTrue(TachiyomiExtensionJsonParser.parse("owner/repository", "https://raw.example/broken.json", "not-json").isEmpty())
	}
}
