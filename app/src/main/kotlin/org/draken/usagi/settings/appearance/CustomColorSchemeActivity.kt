package org.draken.usagi.settings.appearance

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import org.draken.usagi.R
import org.draken.usagi.core.prefs.AppSettings
import org.draken.usagi.core.prefs.ColorScheme
import org.draken.usagi.core.prefs.CustomColorRole
import org.draken.usagi.core.prefs.CustomColorScheme
import org.draken.usagi.core.prefs.CustomColorSchemeStore
import org.draken.usagi.core.ui.BaseActivity
import org.draken.usagi.databinding.ActivityCustomColorSchemeBinding
import java.util.Locale

@AndroidEntryPoint
class CustomColorSchemeActivity : BaseActivity<ActivityCustomColorSchemeBinding>() {
	private var savedScheme: CustomColorScheme? = null
	private val watchers = mutableListOf<TextWatcher>()
	private val roleEditors = linkedMapOf<CustomColorRole, TextInputEditText>()
	private val roleLayouts = linkedMapOf<CustomColorRole, TextInputLayout>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityCustomColorSchemeBinding.inflate(layoutInflater))

		viewBinding.toolbar.setNavigationOnClickListener { finish() }
		viewBinding.platformWarning.visibility =
			if (com.google.android.material.color.DynamicColors
					.isDynamicColorAvailable()
			) {
				View.GONE
			} else {
				View.VISIBLE
			}

		savedScheme = CustomColorSchemeStore.load(this)
		val initial =
			savedScheme
				?: CustomColorScheme(CustomColorScheme.DEFAULT_NAME, CustomColorScheme.DEFAULT_SEED_COLOR)
		viewBinding.nameEdit.setText(initial.name)
		viewBinding.seedEdit.setText(formatColor(initial.seedColor))
		createRoleEditors(initial)
		viewBinding.deleteButton.visibility = if (savedScheme == null) View.GONE else View.VISIBLE
		updatePreview()

		watch(viewBinding.nameEdit)
		watch(viewBinding.seedEdit)
		viewBinding.seedLayout.setEndIconOnClickListener { showColorPicker(viewBinding.seedEdit) }
		viewBinding.resetButton.setOnClickListener { resetEditor() }
		viewBinding.saveButton.setOnClickListener { saveScheme() }
		viewBinding.deleteButton.setOnClickListener { confirmDelete() }
	}

	override fun onApplyWindowInsets(
		v: View,
		insets: androidx.core.view.WindowInsetsCompat,
	): androidx.core.view.WindowInsetsCompat {
		val bars =
			insets.getInsets(
				androidx.core.view.WindowInsetsCompat.Type
					.systemBars(),
			)
		viewBinding.root.setPadding(bars.left, bars.top, bars.right, bars.bottom)
		return insets
	}

	override fun onDestroy() {
		viewBinding.nameEdit.clearFocus()
		viewBinding.seedEdit.clearFocus()
		roleEditors.values.forEach { it.clearFocus() }
		(getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager)
			?.hideSoftInputFromWindow(viewBinding.root.windowToken, 0)
		watchers.forEach { watcher ->
			viewBinding.nameEdit.removeTextChangedListener(watcher)
			viewBinding.seedEdit.removeTextChangedListener(watcher)
			roleEditors.values.forEach { it.removeTextChangedListener(watcher) }
		}
		watchers.clear()
		super.onDestroy()
	}

	private fun createRoleEditors(initial: CustomColorScheme) {
		val colors = CustomColorSchemeStore.resolvedColors(this, initial)
		CustomColorRole.entries.forEach { role ->
			val layout =
				TextInputLayout(this).apply {
					layoutParams =
						LinearLayout
							.LayoutParams(
								ViewGroup.LayoutParams.MATCH_PARENT,
								ViewGroup.LayoutParams.WRAP_CONTENT,
							).apply { topMargin = dp(8) }
					hint = role.displayName()
					endIconMode = TextInputLayout.END_ICON_CUSTOM
					setEndIconDrawable(R.drawable.ic_color_picker)
					setEndIconContentDescription(R.string.custom_color_scheme_pick_color)
				}
			val editor =
				TextInputEditText(this).apply {
					inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
					filters = arrayOf(InputFilter.LengthFilter(9))
					setSingleLine(true)
					setText(formatColor(colors.getValue(role.key)))
				}
			layout.addView(editor)
			layout.setEndIconOnClickListener { showColorPicker(editor) }
			viewBinding.rolesContainer.addView(layout)
			roleEditors[role] = editor
			roleLayouts[role] = layout
			watch(editor)
		}
	}

	private fun watch(editor: TextInputEditText) {
		val watcher =
			object : TextWatcher {
				override fun beforeTextChanged(
					s: CharSequence?,
					start: Int,
					count: Int,
					after: Int,
				) = Unit

				override fun onTextChanged(
					s: CharSequence?,
					start: Int,
					before: Int,
					count: Int,
				) {
					updatePreview()
				}

				override fun afterTextChanged(s: Editable?) = Unit
			}
		watchers += watcher
		editor.addTextChangedListener(watcher)
	}

	private fun resetEditor() {
		val reset = CustomColorScheme(CustomColorScheme.DEFAULT_NAME, CustomColorScheme.DEFAULT_SEED_COLOR)
		viewBinding.nameEdit.setText(reset.name)
		viewBinding.seedEdit.setText(formatColor(reset.seedColor))
		val colors = CustomColorSchemeStore.resolvedColors(this, reset)
		roleEditors.forEach { (role, editor) -> editor.setText(formatColor(colors.getValue(role.key))) }
		roleLayouts.values.forEach { it.error = null }
		viewBinding.seedLayout.error = null
	}

	private fun updatePreview() {
		val seed = parseColor(viewBinding.seedEdit.text?.toString()) ?: CustomColorScheme.DEFAULT_SEED_COLOR
		val name =
			viewBinding.nameEdit.text
				?.toString()
				?.trim()
				.orEmpty()
				.ifBlank { CustomColorScheme.DEFAULT_NAME }
		val colors =
			roleEditors
				.mapNotNull { (role, editor) ->
					parseColor(editor.text?.toString())?.let { role.key to it }
				}.toMap()
		viewBinding.preview.setScheme(CustomColorScheme(name, seed, colors))
		viewBinding.seedLayout.error = null
	}

	private fun saveScheme() {
		val seed = parseColor(viewBinding.seedEdit.text?.toString())
		if (seed == null) {
			viewBinding.seedLayout.error = getString(R.string.custom_color_scheme_invalid_hex)
			return
		}
		val colors = linkedMapOf<String, Int>()
		var invalidRole: CustomColorRole? = null
		roleEditors.forEach { (role, editor) ->
			val color = parseColor(editor.text?.toString())
			if (color == null) {
				roleLayouts[role]?.error = getString(R.string.custom_color_scheme_invalid_hex)
				invalidRole = invalidRole ?: role
			} else {
				roleLayouts[role]?.error = null
				colors[role.key] = color
			}
		}
		if (invalidRole != null) return
		val name =
			viewBinding.nameEdit.text
				?.toString()
				?.trim()
				.orEmpty()
				.ifBlank { CustomColorScheme.DEFAULT_NAME }
		CustomColorSchemeStore.save(this, CustomColorScheme(name, seed, colors))
		setResult(RESULT_OK)
		Toast.makeText(this, R.string.custom_color_scheme_saved, Toast.LENGTH_SHORT).show()
		finish()
	}

	private fun confirmDelete() {
		MaterialAlertDialogBuilder(this)
			.setMessage(R.string.custom_color_scheme_delete_confirm)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(R.string.delete) { _, _ ->
				CustomColorSchemeStore.clear(this)
				setResult(RESULT_OK)
				val prefs = PreferenceManager.getDefaultSharedPreferences(this)

				if (prefs.getString(AppSettings.KEY_COLOR_THEME, null) == ColorScheme.CUSTOM.name) {
					prefs.edit().putString(AppSettings.KEY_COLOR_THEME, ColorScheme.default.name).apply()
				}
				Toast.makeText(this, R.string.custom_color_scheme_deleted, Toast.LENGTH_SHORT).show()

				finish()
			}.show()
	}

	private fun showColorPicker(target: TextInputEditText) {
		val currentColor = parseColor(target.text?.toString()) ?: CustomColorScheme.DEFAULT_SEED_COLOR
		(getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager)
			?.hideSoftInputFromWindow(target.windowToken, 0)
		target.clearFocus()
		val picker =
			ColorPickerView(this).apply {
				setColor(currentColor)
			}
		val container =
			FrameLayout(this).apply {
				setPadding(dp(24), dp(8), dp(24), 0)
				addView(
					picker,
					FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(280)),
				)
			}
		picker.setOnColorChangedListener { color ->
			target.setText(formatColor(color))
			target.setSelection(target.length())
		}
		MaterialAlertDialogBuilder(this)
			.setTitle(R.string.custom_color_scheme_pick_color)
			.setView(container)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(R.string.done, null)
			.show()
	}

	private fun CustomColorRole.displayName(): String =
		key
			.replace(Regex("([a-z])([A-Z])"), "$1 $2")
			.replaceFirstChar { it.titlecase(Locale.ROOT) }

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

	private fun parseColor(value: String?): Int? {
		val normalized = value?.trim()?.let { if (it.startsWith('#')) it else "#$it" } ?: return null
		return runCatching { Color.parseColor(normalized) or 0xff000000.toInt() }.getOrNull()
	}

	private fun formatColor(color: Int): String = String.format(Locale.ROOT, "#%06X", color and 0x00ffffff)
}
