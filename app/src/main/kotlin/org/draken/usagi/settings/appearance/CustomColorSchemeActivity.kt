package org.draken.usagi.settings.appearance

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import org.draken.usagi.R
import org.draken.usagi.core.prefs.AppSettings
import org.draken.usagi.core.prefs.ColorScheme
import org.draken.usagi.core.prefs.CustomColorScheme
import org.draken.usagi.core.prefs.CustomColorSchemeStore
import org.draken.usagi.core.ui.BaseActivity
import org.draken.usagi.databinding.ActivityCustomColorSchemeBinding
import java.util.Locale

@AndroidEntryPoint
class CustomColorSchemeActivity : BaseActivity<ActivityCustomColorSchemeBinding>() {
	private var savedScheme: CustomColorScheme? = null
	private var watcher: TextWatcher? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityCustomColorSchemeBinding.inflate(layoutInflater))

		viewBinding.toolbar.setNavigationOnClickListener { finish() }
		viewBinding.platformWarning.visibility =
			if (DynamicColors.isDynamicColorAvailable()) View.GONE else View.VISIBLE

		savedScheme = CustomColorSchemeStore.load(this)
		val initial = savedScheme ?: CustomColorScheme(CustomColorScheme.DEFAULT_NAME, CustomColorScheme.DEFAULT_SEED_COLOR)
		viewBinding.nameEdit.setText(initial.name)
		viewBinding.seedEdit.setText(formatColor(initial.seedColor))
		viewBinding.deleteButton.visibility = if (savedScheme == null) View.GONE else View.VISIBLE
		updatePreview()

		watcher =
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
				) = updatePreview()

				override fun afterTextChanged(s: Editable?) = Unit
			}.also { listener ->
				viewBinding.nameEdit.addTextChangedListener(listener)
				viewBinding.seedEdit.addTextChangedListener(listener)
			}

		viewBinding.seedLayout.setEndIconOnClickListener { showColorPicker() }
		viewBinding.resetButton.setOnClickListener {
			viewBinding.nameEdit.setText(CustomColorScheme.DEFAULT_NAME)
			viewBinding.seedEdit.setText(formatColor(CustomColorScheme.DEFAULT_SEED_COLOR))
			viewBinding.seedLayout.error = null
		}
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
		(getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager)
			?.hideSoftInputFromWindow(viewBinding.root.windowToken, 0)
		watcher?.let { listener ->
			viewBinding.nameEdit.removeTextChangedListener(listener)
			viewBinding.seedEdit.removeTextChangedListener(listener)
		}
		watcher = null
		super.onDestroy()
	}

	private fun updatePreview() {
		val seed = parseColor(viewBinding.seedEdit.text?.toString()) ?: CustomColorScheme.DEFAULT_SEED_COLOR
		val name =
			viewBinding.nameEdit.text
				?.toString()
				?.trim()
				.orEmpty()
				.ifBlank { CustomColorScheme.DEFAULT_NAME }
		viewBinding.preview.setScheme(CustomColorScheme(name, seed))
		viewBinding.seedLayout.error = null
	}

	private fun saveScheme() {
		val seed = parseColor(viewBinding.seedEdit.text?.toString())
		if (seed == null) {
			viewBinding.seedLayout.error = getString(R.string.custom_color_scheme_invalid_hex)
			return
		}
		val name =
			viewBinding.nameEdit.text
				?.toString()
				?.trim()
				.orEmpty()
				.ifBlank { CustomColorScheme.DEFAULT_NAME }
		CustomColorSchemeStore.save(this, CustomColorScheme(name, seed))
		Toast.makeText(this, R.string.custom_color_scheme_saved, Toast.LENGTH_SHORT).show()
		finish()
	}

	private fun confirmDelete() {
		MaterialAlertDialogBuilder(this)
			.setMessage(R.string.custom_color_scheme_delete_confirm)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(R.string.delete) { _, _ ->
				CustomColorSchemeStore.clear(this)
				val prefs = PreferenceManager.getDefaultSharedPreferences(this)
				if (prefs.getString(AppSettings.KEY_COLOR_THEME, null) == ColorScheme.CUSTOM.name) {
					prefs.edit().putString(AppSettings.KEY_COLOR_THEME, ColorScheme.default.name).apply()
				}
				Toast.makeText(this, R.string.custom_color_scheme_deleted, Toast.LENGTH_SHORT).show()

				finish()
			}.show()
	}

	private fun showColorPicker() {
		val currentColor = parseColor(viewBinding.seedEdit.text?.toString()) ?: CustomColorScheme.DEFAULT_SEED_COLOR
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
			viewBinding.seedEdit.setText(formatColor(color))
			viewBinding.seedEdit.setSelection(viewBinding.seedEdit.length())
		}
		MaterialAlertDialogBuilder(this)
			.setTitle(R.string.custom_color_scheme_pick_color)
			.setView(container)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(R.string.done, null)
			.show()
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

	private fun parseColor(value: String?): Int? {
		val normalized = value?.trim()?.let { if (it.startsWith('#')) it else "#$it" } ?: return null
		return runCatching { Color.parseColor(normalized) or 0xff000000.toInt() }.getOrNull()
	}

	private fun formatColor(color: Int): String = String.format(Locale.ROOT, "#%06X", color and 0x00ffffff)
}
