package org.draken.usagi.settings.sources.manage.plugins.model

import androidx.annotation.StringRes
import org.draken.usagi.list.ui.model.ListModel

sealed interface PluginManageItem : ListModel {
	data class Plugin(
		val name: String,
		val repository: String?,
		val installedTag: String?,
		val latestTag: String?,
	) : PluginManageItem {
		val displayName: String
			get() = name.removeSuffix(".jar")

		val iconUrl: String?
			get() = repository?.githubAvatarUrl()

		val hasUpdate: Boolean
			get() = !latestTag.isNullOrBlank() && latestTag != installedTag

		override fun areItemsTheSame(other: ListModel): Boolean = other is Plugin && name == other.name
	}

	data class ExternalRepository(
		val url: String,
		val repository: String,
		val title: String,
		val path: String,
	) : PluginManageItem {
		val displayName: String
			get() = title.ifBlank { path.substringBeforeLast('/').ifBlank { path } }

		val iconUrl: String?
			get() = repository.githubAvatarUrl()

		override fun areItemsTheSame(other: ListModel): Boolean = other is ExternalRepository && url == other.url
	}

	data object Importing : PluginManageItem {
		override fun areItemsTheSame(other: ListModel): Boolean = other is Importing
	}

	data class Placeholder(
		@field:StringRes val titleResId: Int,
		@field:StringRes val summaryResId: Int?,
	) : PluginManageItem {
		override fun areItemsTheSame(other: ListModel): Boolean = other is Placeholder && titleResId == other.titleResId && summaryResId == other.summaryResId
	}
}

private fun String.githubAvatarUrl(): String? {
	val owner = substringBefore('/').trim()
	return owner.takeIf { it.isNotBlank() && it != this && it.matches(GITHUB_OWNER_REGEX) }?.let { "https://github.com/$it.png" }
}

private val GITHUB_OWNER_REGEX = Regex("[A-Za-z0-9_.-]+")
