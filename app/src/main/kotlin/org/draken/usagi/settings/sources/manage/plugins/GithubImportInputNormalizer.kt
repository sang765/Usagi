package org.draken.usagi.settings.sources.manage.plugins

internal fun normalizeGithubImportInput(value: String): String = value.trim().replace(Regex("\\s+"), "/")
