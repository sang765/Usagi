package org.draken.usagi.core.prefs

import androidx.annotation.ColorRes
import org.draken.usagi.R

/** Material 3 color roles exposed by the custom scheme editor. */
enum class CustomColorRole(
	val key: String,
	@ColorRes val resourceId: Int,
) {
	PRIMARY("primary", R.color.usagi_primary),
	ON_PRIMARY("onPrimary", R.color.usagi_onPrimary),
	PRIMARY_CONTAINER("primaryContainer", R.color.usagi_primaryContainer),
	ON_PRIMARY_CONTAINER("onPrimaryContainer", R.color.usagi_onPrimaryContainer),
	SECONDARY("secondary", R.color.usagi_secondary),
	ON_SECONDARY("onSecondary", R.color.usagi_onSecondary),
	SECONDARY_CONTAINER("secondaryContainer", R.color.usagi_secondaryContainer),
	ON_SECONDARY_CONTAINER("onSecondaryContainer", R.color.usagi_onSecondaryContainer),
	TERTIARY("tertiary", R.color.usagi_tertiary),
	ON_TERTIARY("onTertiary", R.color.usagi_onTertiary),
	TERTIARY_CONTAINER("tertiaryContainer", R.color.usagi_tertiaryContainer),
	ON_TERTIARY_CONTAINER("onTertiaryContainer", R.color.usagi_onTertiaryContainer),
	ERROR("error", R.color.usagi_error),
	ON_ERROR("onError", R.color.usagi_onError),
	ERROR_CONTAINER("errorContainer", R.color.usagi_errorContainer),
	ON_ERROR_CONTAINER("onErrorContainer", R.color.usagi_onErrorContainer),
	BACKGROUND("background", R.color.usagi_background),
	ON_BACKGROUND("onBackground", R.color.usagi_onBackground),
	SURFACE("surface", R.color.usagi_surface),
	ON_SURFACE("onSurface", R.color.usagi_onSurface),
	SURFACE_VARIANT("surfaceVariant", R.color.usagi_surfaceVariant),
	ON_SURFACE_VARIANT("onSurfaceVariant", R.color.usagi_onSurfaceVariant),
	OUTLINE("outline", R.color.usagi_outline),
	OUTLINE_VARIANT("outlineVariant", R.color.usagi_outlineVariant),
	SCRIM("scrim", R.color.usagi_scrim),
	INVERSE_SURFACE("inverseSurface", R.color.usagi_inverseSurface),
	INVERSE_ON_SURFACE("inverseOnSurface", R.color.usagi_inverseOnSurface),
	INVERSE_PRIMARY("inversePrimary", R.color.usagi_inversePrimary),
	PRIMARY_FIXED("primaryFixed", R.color.usagi_primaryFixed),
	ON_PRIMARY_FIXED("onPrimaryFixed", R.color.usagi_onPrimaryFixed),
	PRIMARY_FIXED_DIM("primaryFixedDim", R.color.usagi_primaryFixedDim),
	ON_PRIMARY_FIXED_VARIANT("onPrimaryFixedVariant", R.color.usagi_onPrimaryFixedVariant),
	SECONDARY_FIXED("secondaryFixed", R.color.usagi_secondaryFixed),
	ON_SECONDARY_FIXED("onSecondaryFixed", R.color.usagi_onSecondaryFixed),
	SECONDARY_FIXED_DIM("secondaryFixedDim", R.color.usagi_secondaryFixedDim),
	ON_SECONDARY_FIXED_VARIANT("onSecondaryFixedVariant", R.color.usagi_onSecondaryFixedVariant),
	TERTIARY_FIXED("tertiaryFixed", R.color.usagi_tertiaryFixed),
	ON_TERTIARY_FIXED("onTertiaryFixed", R.color.usagi_onTertiaryFixed),
	TERTIARY_FIXED_DIM("tertiaryFixedDim", R.color.usagi_tertiaryFixedDim),
	ON_TERTIARY_FIXED_VARIANT("onTertiaryFixedVariant", R.color.usagi_onTertiaryFixedVariant),
	SURFACE_DIM("surfaceDim", R.color.usagi_surfaceDim),
	SURFACE_BRIGHT("surfaceBright", R.color.usagi_surfaceBright),
	SURFACE_CONTAINER_LOWEST("surfaceContainerLowest", R.color.usagi_surfaceContainerLowest),
	SURFACE_CONTAINER_LOW("surfaceContainerLow", R.color.usagi_surfaceContainerLow),
	SURFACE_CONTAINER("surfaceContainer", R.color.usagi_surfaceContainer),
	SURFACE_CONTAINER_HIGH("surfaceContainerHigh", R.color.usagi_surfaceContainerHigh),
	SURFACE_CONTAINER_HIGHEST("surfaceContainerHighest", R.color.usagi_surfaceContainerHighest),
	;

	companion object {
		fun fromKey(key: String): CustomColorRole? = entries.firstOrNull { it.key == key }
	}
}
