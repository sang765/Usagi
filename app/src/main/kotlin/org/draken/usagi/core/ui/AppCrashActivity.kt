package org.draken.usagi.core.ui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Process.killProcess
import android.os.Process.myPid
import android.provider.MediaStore
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import org.draken.usagi.BuildConfig
import org.draken.usagi.R
import org.draken.usagi.databinding.ActivityCrashBinding
import org.draken.usagi.main.ui.MainActivity
import android.R as androidR
import com.google.android.material.R as materialR

class AppCrashActivity : BaseActivity<ActivityCrashBinding>() {
	private var headingRunnable: Runnable? = null
	private val handler = Handler(Looper.getMainLooper())
	private var crashReport = NO_TRACE
	private val saveCrashLogLauncher =
		registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
			if (uri != null) writeCrashReport(uri)
		}

	override fun onCreate(savedInstanceState: Bundle?) {
		applyTheme()
		super.onCreate(savedInstanceState)
		setContentView(ActivityCrashBinding.inflate(layoutInflater))
		val stackTrace = intent.getStringExtra(EXTRA_STACK_TRACE) ?: NO_TRACE
		crashReport =
			buildString {
				appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
				appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
				appendLine("Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
				appendLine("-------------------------------------")
				append(stackTrace)
			}
		viewBinding.crashTextView.text = crashReport
		viewBinding.buttonReport.setOnClickListener {
			runCatching {
				val intent =
					Intent(Intent.ACTION_SEND).apply {
						type = "*/*"
						putExtra(
							Intent.EXTRA_SUBJECT,
							"Usagi v${BuildConfig.VERSION_NAME} (${Build.MANUFACTURER} ${Build.MODEL}) w/ ${Build.VERSION.RELEASE}",
						)
						putExtra(Intent.EXTRA_TEXT, crashReport)
					}
				startActivity(Intent.createChooser(intent, getString(R.string.report)))
			}
		}
		viewBinding.buttonSaveCrashLog.setOnClickListener { saveCrashReport() }
		viewBinding.buttonRestart.setOnClickListener {
			startActivity(
				Intent(this, MainActivity::class.java)
					.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
			)
			finishAndRemoveTask()
		}
		setupView()
		setupText()
	}

	override fun onApplyWindowInsets(
		v: View,
		insets: WindowInsetsCompat,
	): WindowInsetsCompat {
		val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
		viewBinding.mainLayout.updatePadding(bars.left, bars.top, bars.right, bars.bottom)
		return insets
	}

	override fun onDestroy() {
		headingRunnable?.let(handler::removeCallbacks)
		super.onDestroy()
		if (isFinishing) killProcess(myPid())
	}

	private fun saveCrashReport() {
		val fileName = "usagi-crash-${System.currentTimeMillis()}.txt"
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			val values =
				ContentValues().apply {
					put(MediaStore.Downloads.DISPLAY_NAME, fileName)
					put(MediaStore.Downloads.MIME_TYPE, "text/plain")
					put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
					put(MediaStore.Downloads.IS_PENDING, 1)
				}
			val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
			if (uri == null) {
				showCrashLogSaveResult(false)
				return
			}
			runCatching {
				contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(crashReport) }
					?: error("Unable to open crash log output stream")
				contentResolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
			}.onFailure {
				contentResolver.delete(uri, null, null)
			}.onSuccess { showCrashLogSaveResult(true) }
		} else {
			saveCrashLogLauncher.launch(fileName)
		}
	}

	private fun writeCrashReport(uri: android.net.Uri) {
		runCatching {
			contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(crashReport) }
				?: error("Unable to open crash log output stream")
		}.onSuccess { showCrashLogSaveResult(true) }
			.onFailure { showCrashLogSaveResult(false) }
	}

	private fun showCrashLogSaveResult(isSuccess: Boolean) {
		Toast
			.makeText(
				this,
				getString(if (isSuccess) R.string.crash_log_saved else R.string.crash_log_save_failed),
				Toast.LENGTH_SHORT,
			).show()
	}

	private fun applyTheme() {
		val nightMode = intent.getIntExtra(EXTRA_THEME_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
		AppCompatDelegate.setDefaultNightMode(nightMode)
		val styleRes = intent.getIntExtra(EXTRA_THEME_STYLE, 0)
		if (styleRes != 0) setTheme(styleRes)
		if (intent.getBooleanExtra(EXTRA_THEME_AMOLED, false)) setTheme(R.style.ThemeOverlay_Usagi_Amoled)
	}

	private fun setupView() =
		with(viewBinding.starView) {
			val blended =
				ColorUtils.blendARGB(
					resolveColor(materialR.attr.colorPrimaryContainer),
					resolveColor(androidR.attr.colorBackground),
					0.5f,
				)
			setColorFilter(blended)
			ObjectAnimator.ofFloat(this, "rotation", -20f, 20f).apply {
				duration = 5_000
				repeatMode = ValueAnimator.REVERSE
				repeatCount = ValueAnimator.INFINITE
				start()
			}
		}

	private fun setupText() {
		var index = 0
		val titles = arrayOf(getString(R.string.crash_oops), getString(R.string.error_occurred))
		headingRunnable =
			object : Runnable {
				override fun run() {
					index = (index + 1) % titles.size
					viewBinding.heading.fadeTo(titles[index])
					handler.postDelayed(this, SWAP_DELAY)
				}
			}.also { handler.postDelayed(it, SWAP_DELAY) }
	}

	private fun resolveColor(attr: Int): Int {
		val tv = TypedValue()
		theme.resolveAttribute(attr, tv, true)
		return tv.data
	}

	private fun TextView.fadeTo(newText: String) {
		animate()
			.alpha(0f)
			.setDuration(FADE_DURATION)
			.withEndAction {
				text = newText
				animate().alpha(1f).setDuration(FADE_DURATION).start()
			}.start()
	}

	companion object {
		const val EXTRA_STACK_TRACE = "stack_trace"
		const val EXTRA_THEME_STYLE = "theme_style"
		const val EXTRA_THEME_AMOLED = "theme_amoled"
		const val EXTRA_THEME_NIGHT_MODE = "theme_night_mode"
		private const val NO_TRACE = "noStackTrace"
		private const val SWAP_DELAY = 7_500L
		private const val FADE_DURATION = 300L
	}
}
