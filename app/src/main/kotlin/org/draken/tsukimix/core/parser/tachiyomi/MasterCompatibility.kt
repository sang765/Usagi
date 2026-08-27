@file:Suppress("unused")

package org.draken.tsukimix.core.parser.tachiyomi

import android.content.Context
import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import org.draken.tsukimix.core.parser.external.ExtensionBridge
import org.draken.tsukimix.core.parser.external.ExtensionLoader
import org.draken.tsukimix.core.parser.external.ExtensionManager
import org.draken.tsukimix.core.parser.external.ExtensionProvider
import org.draken.tsukimix.core.parser.external.ExtensionSourceSettings
import org.draken.tsukimix.core.parser.external.NativeExtManager
import org.draken.tsukimix.core.parser.external.model.ExtArtifact
import org.draken.tsukimix.core.parser.external.model.ExtInstalled
import org.draken.tsukimix.core.parser.external.model.ExtSource
import org.draken.tsukimix.core.parser.external.model.Manga
import org.draken.tsukimix.core.parser.external.model.MangaResult
import java.util.Locale

/**
 * Source-level aliases for the package reorganization in TsukiMix master.
 * Usagi keeps these names temporarily so feature code can migrate without
 * duplicating the runtime implementation or changing behavior unnecessarily.
 */
typealias TachiyomiInjektBridge = ExtensionBridge
typealias TachiyomiExtensionLoader = ExtensionLoader
typealias TachiyomiExtensionManager = ExtensionManager
typealias TachiyomiExtensionCatalogProvider = ExtensionProvider
typealias DirectTachiyomiExtensionManager = NativeExtManager
typealias DirectTachiyomiInstalled = ExtInstalled
typealias TachiyomiExtensionArtifact = ExtArtifact
typealias TachiyomiCatalogSource = ExtSource

/** Compatibility facade for the settings object renamed in TsukiMix master. */
object TachiyomiSourceSettings {
	const val KEY_DOMAIN = ExtensionSourceSettings.KEY_DOMAIN
	const val KEY_OVERRIDE_BASE_URL = ExtensionSourceSettings.KEY_OVERRIDE_BASE_URL

	fun preferences(
		context: Context,
		source: Manga,
	) = ExtensionSourceSettings.preferences(context, source)

	fun browserUrl(
		context: Context,
		source: Manga,
	) = ExtensionSourceSettings.browserUrl(context, source)

	fun refreshDomainOverride(
		context: Context,
		source: Manga,
	) = ExtensionSourceSettings.refreshDomainOverride(context, source)

	fun mergeDomainPreference(
		context: Context,
		source: Manga,
	) = ExtensionSourceSettings.mergeDomainPreference(context, source)

	fun isSlowdownEnabled(
		context: Context,
		source: Manga,
	) = ExtensionSourceSettings.isSlowdownEnabled(context, source)
}

/** Compatibility extension retained for existing source settings call sites. */
fun TachiyomiExtensionManager.addLangToPref(
	screen: PreferenceScreen,
	source: Manga,
	title: CharSequence,
	onChanged: () -> Unit,
) {
	val variants = getLanguage(source).distinctBy { it.locale.lowercase(Locale.ROOT) }.sortedBy { it.languageDisplayName }
	if (variants.size <= 1) return
	ListPreference(screen.context).apply {
		key = "language"
		order = 1
		isPersistent = false
		isIconSpaceReserved = false
		entries = variants.map { it.languageDisplayName }.toTypedArray()
		entryValues = variants.map { it.locale }.toTypedArray()
		value = getActiveLanguage(source) ?: variants.first().locale
		summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
		this.title = title
		dialogTitle = title
		setOnPreferenceChangeListener { _, newValue ->
			val lang = newValue as? String ?: return@setOnPreferenceChangeListener false
			setActiveLanguage(source, lang)
			onChanged()
			true
		}
		screen.addPreference(this)
	}
}
