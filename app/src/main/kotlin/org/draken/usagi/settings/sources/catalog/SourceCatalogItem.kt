package org.draken.usagi.settings.sources.catalog

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.draken.tsukimix.core.parser.tachiyomi.DirectTachiyomiInstalled
import org.draken.tsukimix.core.parser.tachiyomi.TachiyomiCatalogSource
import org.draken.tsukimix.core.parser.tachiyomi.TachiyomiExtensionArtifact
import org.draken.usagi.R
import org.draken.usagi.core.model.titleResId
import org.draken.usagi.list.ui.model.ListModel
import tsuki.model.ContentType
import tsuki.model.MangaSource
import java.util.Locale

sealed interface SourceCatalogItem : ListModel {
	data class Source(
		val source: MangaSource,
	) : SourceCatalogItem {
		override fun areItemsTheSame(other: ListModel): Boolean = other is Source && other.source == source
	}

	data class Tachiyomi(
		val source: TachiyomiCatalogSource,
		val artifact: TachiyomiExtensionArtifact,
		val installed: DirectTachiyomiInstalled?,
		val isLegacyInstalled: Boolean = false,
		val isLoading: Boolean = false,
	) : SourceCatalogItem {
		val isInstalled: Boolean
			get() = installed != null

		val canInstallApk: Boolean
			get() =
				artifact.apkUrl?.trim()?.let { it.startsWith("http://") || it.startsWith("https://") } == true &&
					installed == null &&
					!isLegacyInstalled

		val hasUpdate: Boolean

			get() {
				val availableVersion = artifact.versionCode ?: return false
				val installedVersion = installed?.versionCode ?: return false
				return availableVersion > installedVersion
			}

		val displayName: String
			get() = source.name

		fun description(context: Context): String {
			val contentType = context.getString(source.contentType.titleResId)
			val language =
				if (source.language.equals("all", true)) {
					context.getString(R.string.various_languages)
				} else {
					val locale = Locale.forLanguageTag(source.language.replace('_', '-'))
					locale.getDisplayName(locale).takeIf { it.isNotBlank() } ?: source.language
				}
			val artifactSuffix = tachiyomiArtifactSuffix(source.name, artifact.name)
			return if (artifactSuffix == null) "$contentType, $language" else "$contentType, $language • $artifactSuffix"
		}

		val isNsfw: Boolean
			get() = source.contentType == ContentType.HENTAI

		override fun areItemsTheSame(other: ListModel): Boolean = other is Tachiyomi && artifact.packageName == other.artifact.packageName && source.id == other.source.id
	}

	data class Hint(
		@field:DrawableRes val icon: Int,
		@field:StringRes val title: Int,
		@field:StringRes val text: Int,
	) : SourceCatalogItem {
		override fun areItemsTheSame(other: ListModel): Boolean = other is Hint && other.title == title
	}
}
