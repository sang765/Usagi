package org.draken.usagi.explore.data

internal fun shouldPersistInSourcesCatalog(
	isExternalSource: Boolean,
	isDirectTachiyomiSource: Boolean,
): Boolean = !isExternalSource || isDirectTachiyomiSource
