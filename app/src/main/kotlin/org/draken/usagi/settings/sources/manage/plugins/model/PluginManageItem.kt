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

		val hasUpdate: Boolean
			get() = !latestTag.isNullOrBlank() && latestTag != installedTag

		override fun areItemsTheSame(other: ListModel): Boolean = other is Plugin && name == other.name
	}

	data class Remote(
		val id: String,
		val name: String,
		val packageName: String,
		val versionName: String?,
		val versionCode: String?,
		val languages: List<String>,
		val contentWarning: String?,
		val repository: String,
		val indexUrl: String,
	) : PluginManageItem {
		val displayName: String
			get() = name

		override fun areItemsTheSame(other: ListModel): Boolean = other is Remote && id == other.id && indexUrl == other.indexUrl
	}

	data class Placeholder(
		@field:StringRes val titleResId: Int,
		@field:StringRes val summaryResId: Int?,
	) : PluginManageItem {
		override fun areItemsTheSame(other: ListModel): Boolean = other is Placeholder && titleResId == other.titleResId && summaryResId == other.summaryResId
	}
}
