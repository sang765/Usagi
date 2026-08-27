package org.draken.usagi.settings.sources.catalog

import tsuki.model.ContentType

/** Immutable state rendered by the catalog filter controls. */
data class SourcesCatalogUiState(
	val appliedFilter: SourcesCatalogFilter,
	val hasNewSources: Boolean,
	val contentTypes: List<ContentType>,
	val isExternalLoading: Boolean,
)
