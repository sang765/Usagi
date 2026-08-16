package org.draken.usagi.core.prefs

import android.content.Context
import android.content.res.Configuration
import androidx.preference.PreferenceManager
import com.google.android.material.color.ColorResourcesOverride
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.utilities.CorePalette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

/** A Material 3 scheme with explicit role values and a legacy seed fallback. */
data class CustomColorScheme(
	val name: String,
	val seedColor: Int,
	val colors: Map<String, Int> = emptyMap(),
) {
	companion object {
		const val DEFAULT_NAME = "Custom"
		const val DEFAULT_SEED_COLOR = 0xff6750a4.toInt()
	}

	fun color(role: CustomColorRole): Int? = colors[role.key]
}

object CustomColorSchemeStore {
	private const val KEY_SCHEME = "custom_color_scheme"
	private const val JSON_NAME = "name"
	private const val JSON_SEED = "seed"
	private const val JSON_COLORS = "colors"

	private val prewarmScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
	private var prewarmJob: Job? = null

	@Volatile private var cachedPalette: CachedPalette? = null

	fun load(context: Context): CustomColorScheme? {
		val raw = PreferenceManager.getDefaultSharedPreferences(context).getString(KEY_SCHEME, null)
		return raw?.let { value ->
			runCatching {
				val json = JSONObject(value)
				val colorsJson = json.optJSONObject(JSON_COLORS)
				val colors =
					buildMap {
						CustomColorRole.entries.forEach { role ->
							if (colorsJson?.has(role.key) == true) {
								put(role.key, colorsJson.getInt(role.key))
							}
						}
					}
				CustomColorScheme(
					name =
						json
							.optString(JSON_NAME, CustomColorScheme.DEFAULT_NAME)
							.trim()
							.ifBlank { CustomColorScheme.DEFAULT_NAME },
					seedColor = json.optInt(JSON_SEED, CustomColorScheme.DEFAULT_SEED_COLOR),
					colors = colors,
				)
			}.getOrNull()
		}
	}

	fun save(
		context: Context,
		scheme: CustomColorScheme,
	) {
		val colorsJson = JSONObject()
		scheme.colors.forEach { (key, color) -> colorsJson.put(key, color) }
		val json =
			JSONObject()
				.put(JSON_NAME, scheme.name.trim().ifBlank { CustomColorScheme.DEFAULT_NAME })
				.put(JSON_SEED, scheme.seedColor)
				.put(JSON_COLORS, colorsJson)
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
		val scheme = load(context.applicationContext) ?: return context
		val colorValues = getOrCreateColorValues(context.applicationContext, scheme)
		if (colorValues != null) {
			ColorResourcesOverride.getInstance()?.let { override ->
				return override.wrapContextIfPossible(context, colorValues)
			}
		}
		return context
	}

	private fun getOrCreateColorValues(
		context: Context,
		scheme: CustomColorScheme,
	): Map<Int, Int>? {
		if (!DynamicColors.isDynamicColorAvailable()) return null
		val isDark =
			context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
				Configuration.UI_MODE_NIGHT_YES
		val signature = scheme.hashCode()
		cachedPalette?.let { cached ->
			if (cached.signature == signature && cached.isDark == isDark) {
				return cached.colorValues
			}
		}
		return synchronized(this) {
			cachedPalette?.let { cached ->
				if (cached.signature == signature && cached.isDark == isDark) {
					return@synchronized cached.colorValues
				}
			}
			val colors = resolveColors(scheme, isDark)
			val colorValues =
				CustomColorRole.entries.associate { role ->
					role.resourceId to (colors[role.key] ?: 0xff000000.toInt())
				}
			cachedPalette = CachedPalette(signature, isDark, colorValues)
			colorValues
		}
	}

	fun resolvedColors(
		context: Context,
		scheme: CustomColorScheme,
	): Map<String, Int> {
		val isDark =
			context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
				Configuration.UI_MODE_NIGHT_YES
		return resolveColors(scheme, isDark)
	}

	private fun resolveColors(
		scheme: CustomColorScheme,
		isDark: Boolean,
	): Map<String, Int> {
		val palette = CorePalette.of(scheme.seedColor)
		val a1 = palette.a1
		val a2 = palette.a2
		val a3 = palette.a3
		val n1 = palette.n1
		val n2 = palette.n2
		val error = palette.error
		val primary = a1.tone(if (isDark) 80 else 40)
		val onPrimary = a1.tone(if (isDark) 20 else 100)
		val primaryContainer = a1.tone(if (isDark) 30 else 90)
		val onPrimaryContainer = a1.tone(if (isDark) 90 else 10)
		val secondary = a2.tone(if (isDark) 80 else 40)
		val onSecondary = a2.tone(if (isDark) 20 else 100)
		val secondaryContainer = a2.tone(if (isDark) 30 else 90)
		val onSecondaryContainer = a2.tone(if (isDark) 90 else 10)
		val tertiary = a3.tone(if (isDark) 80 else 40)
		val onTertiary = a3.tone(if (isDark) 20 else 100)
		val tertiaryContainer = a3.tone(if (isDark) 30 else 90)
		val onTertiaryContainer = a3.tone(if (isDark) 90 else 10)
		val background = n1.tone(if (isDark) 6 else 98)
		val onBackground = n1.tone(if (isDark) 90 else 10)
		val surface = background
		val onSurface = onBackground
		val surfaceVariant = n2.tone(if (isDark) 30 else 90)
		val onSurfaceVariant = n2.tone(if (isDark) 80 else 30)
		val outline = n2.tone(if (isDark) 60 else 50)
		val outlineVariant = n2.tone(if (isDark) 30 else 80)
		val inverseSurface = n1.tone(if (isDark) 90 else 20)
		val inverseOnSurface = n1.tone(if (isDark) 20 else 95)
		val inversePrimary = a1.tone(if (isDark) 40 else 80)
		return buildMap {
			put("primary", primary)
			put("onPrimary", onPrimary)
			put("primaryContainer", primaryContainer)
			put("onPrimaryContainer", onPrimaryContainer)
			put("secondary", secondary)
			put("onSecondary", onSecondary)
			put("secondaryContainer", secondaryContainer)
			put("onSecondaryContainer", onSecondaryContainer)
			put("tertiary", tertiary)
			put("onTertiary", onTertiary)
			put("tertiaryContainer", tertiaryContainer)
			put("onTertiaryContainer", onTertiaryContainer)
			put("error", error.tone(if (isDark) 80 else 40))
			put("onError", error.tone(if (isDark) 20 else 100))
			put("errorContainer", error.tone(if (isDark) 30 else 90))
			put("onErrorContainer", error.tone(if (isDark) 90 else 10))
			put("background", background)
			put("onBackground", onBackground)
			put("surface", surface)
			put("onSurface", onSurface)
			put("surfaceVariant", surfaceVariant)
			put("onSurfaceVariant", onSurfaceVariant)
			put("outline", outline)
			put("outlineVariant", outlineVariant)
			put("scrim", 0xff000000.toInt())
			put("inverseSurface", inverseSurface)
			put("inverseOnSurface", inverseOnSurface)
			put("inversePrimary", inversePrimary)
			put("primaryFixed", a1.tone(90))
			put("onPrimaryFixed", a1.tone(10))
			put("primaryFixedDim", a1.tone(80))
			put("onPrimaryFixedVariant", a1.tone(30))
			put("secondaryFixed", a2.tone(90))
			put("onSecondaryFixed", a2.tone(10))
			put("secondaryFixedDim", a2.tone(80))
			put("onSecondaryFixedVariant", a2.tone(30))
			put("tertiaryFixed", a3.tone(90))
			put("onTertiaryFixed", a3.tone(10))
			put("tertiaryFixedDim", a3.tone(80))
			put("onTertiaryFixedVariant", a3.tone(30))
			put("surfaceDim", n1.tone(if (isDark) 6 else 87))
			put("surfaceBright", n1.tone(if (isDark) 24 else 98))
			put("surfaceContainerLowest", n1.tone(if (isDark) 4 else 100))
			put("surfaceContainerLow", n1.tone(if (isDark) 10 else 96))
			put("surfaceContainer", n1.tone(if (isDark) 12 else 94))
			put("surfaceContainerHigh", n1.tone(if (isDark) 17 else 92))
			put("surfaceContainerHighest", n1.tone(if (isDark) 22 else 90))
		}.toMutableMap().apply {
			putAll(scheme.colors)
		}
	}

	private data class CachedPalette(
		val signature: Int,
		val isDark: Boolean,
		val colorValues: Map<Int, Int>,
	)
}
