package org.draken.usagi.settings.sources.catalog

internal fun shouldRemoveTachiyomiOnToggle(
	isInstalled: Boolean,
	hasUpdate: Boolean,
): Boolean = isInstalled && !hasUpdate
