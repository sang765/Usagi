package org.draken.usagi.settings.sources.manage.plugins

import java.util.Locale

internal fun tachiyomiIndexPriority(path: String): Int =
	when (path.lowercase(Locale.ROOT)) {
		"repo/index.json", "index.json" -> 0
		"repo/min.index.json", "min.index.json" -> 1
		"repo/index.min.json", "index.min.json" -> 2
		else -> 3
	}
