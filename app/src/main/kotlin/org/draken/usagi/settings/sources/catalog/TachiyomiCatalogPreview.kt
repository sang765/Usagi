package org.draken.usagi.settings.sources.catalog

internal fun previewPackageName(
	packageName: String,
	installedPackageNames: Collection<String>,
): String? = packageName.takeUnless { it in installedPackageNames }
