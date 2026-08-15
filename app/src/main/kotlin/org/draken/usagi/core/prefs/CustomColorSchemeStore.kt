package org.draken.usagi.core.prefs

import android.content.Context
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
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
	}

	fun clear(context: Context) {
		PreferenceManager
			.getDefaultSharedPreferences(context)
			.edit()
			.remove(KEY_SCHEME)
			.apply()
	}

	fun wrapContext(
		context: Context,
		colorScheme: ColorScheme,
	): Context {
		if (colorScheme != ColorScheme.CUSTOM) return context
		val scheme = load(context) ?: return context
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
}
