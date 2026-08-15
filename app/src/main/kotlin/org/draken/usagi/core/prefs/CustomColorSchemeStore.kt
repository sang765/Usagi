package org.draken.usagi.core.prefs

import android.content.Context
import android.content.res.Configuration
import androidx.preference.PreferenceManager
import com.google.android.material.color.ColorResourcesOverride
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import com.google.android.material.color.MaterialColorUtilitiesHelper
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.SchemeContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.draken.usagi.R
import org.json.JSONObject

/**
 * Stores the user-defined Material 3 color scheme.
 *
 * Material's content-based dynamic color resource override is used when it is available. The
 * scheme is kept as a seed rather than a long list of derived roles so that it remains portable
 * across Material Components updates and light/dark modes.
 */
data class CustomColorScheme(
	val name: String,
	val seedColor: Int,
) {
	companion object {
		const val DEFAULT_NAME = "Custom"
		const val DEFAULT_SEED_COLOR = 0xff6750a4.toInt()
	}
}

object CustomColorSchemeStore {
	private const val KEY_SCHEME = "custom_color_scheme"
	private const val JSON_NAME = "name"
	private const val JSON_SEED = "seed"

	private val prewarmScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
	private var prewarmJob: Job? = null

	@Volatile private var cachedPalette: CachedPalette? = null

	fun load(context: Context): CustomColorScheme? {
		val raw = PreferenceManager.getDefaultSharedPreferences(context).getString(KEY_SCHEME, null)
		return raw?.let { value ->
			runCatching {
				val json = JSONObject(value)
				CustomColorScheme(
					name =
						json
							.optString(JSON_NAME, CustomColorScheme.DEFAULT_NAME)
							.trim()
							.ifBlank { CustomColorScheme.DEFAULT_NAME },
					seedColor = json.getInt(JSON_SEED),
				)
			}.getOrNull()
		}
	}

	fun save(
		context: Context,
		scheme: CustomColorScheme,
	) {
		val json =
			JSONObject()
				.put(JSON_NAME, scheme.name.trim().ifBlank { CustomColorScheme.DEFAULT_NAME })
				.put(JSON_SEED, scheme.seedColor)
		PreferenceManager
			.getDefaultSharedPreferences(context)
			.edit()
			.putString(KEY_SCHEME, json.toString())
			.apply()
		cachedPalette = null
		prewarmAsync(context)
	}

	fun clear(context: Context) {
		PreferenceManager
			.getDefaultSharedPreferences(context)
			.edit()
			.remove(KEY_SCHEME)
			.apply()
		cachedPalette = null
	}

	fun prewarmAsync(context: Context) {
		if (!DynamicColors.isDynamicColorAvailable()) return
		val applicationContext = context.applicationContext
		prewarmJob?.cancel()
		prewarmJob =
			prewarmScope.launch {
				val scheme = load(applicationContext) ?: return@launch
				getOrCreateColorValues(applicationContext, scheme)
			}
	}

	fun wrapContext(
		context: Context,
		colorScheme: ColorScheme,
	): Context {
		if (colorScheme != ColorScheme.CUSTOM) return context
		val applicationContext = context.applicationContext
		val scheme = load(applicationContext) ?: return context
		val colorValues = getOrCreateColorValues(applicationContext, scheme)
		if (colorValues != null) {
			ColorResourcesOverride.getInstance()?.let { override ->
				return override.wrapContextIfPossible(context, colorValues)
			}
		}
		return runCatching {
			DynamicColors.wrapContextIfAvailable(
				context,
				DynamicColorsOptions
					.Builder()
					.setContentBasedSource(scheme.seedColor)
					.setThemeOverlay(R.style.ThemeOverlay_Usagi_Monet)
					.build(),
			)
		}.getOrDefault(context)
	}

	private fun getOrCreateColorValues(
		context: Context,
		scheme: CustomColorScheme,
	): Map<Int, Int>? {
		if (!DynamicColors.isDynamicColorAvailable()) return null
		val isDark = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
		cachedPalette?.let { cached ->
			if (cached.seedColor == scheme.seedColor && cached.isDark == isDark) {
				return cached.colorValues
			}
		}
		return synchronized(this) {
			cachedPalette?.let { cached ->
				if (cached.seedColor == scheme.seedColor && cached.isDark == isDark) {
					return@synchronized cached.colorValues
				}
			}
			runCatching {
				val dynamicScheme =
					SchemeContent(
						Hct.fromInt(scheme.seedColor),
						isDark,
						0.0,
					)
				val colorValues = MaterialColorUtilitiesHelper.createColorResourcesIdsToColorValues(dynamicScheme)
				cachedPalette = CachedPalette(scheme.seedColor, isDark, colorValues)
				colorValues
			}.getOrNull()
		}
	}

	private data class CachedPalette(
		val seedColor: Int,
		val isDark: Boolean,
		val colorValues: Map<Int, Int>,
	)
}
