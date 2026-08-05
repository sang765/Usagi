package org.draken.usagi.browser

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContract
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.draken.usagi.R
import org.draken.usagi.core.exceptions.InteractiveActionRequiredException
import org.draken.usagi.core.nav.AppRouter
import org.draken.usagi.core.nav.router
import org.draken.usagi.core.parser.MangaParserRepository
import org.draken.usagi.core.prefs.SourceSettings
import org.draken.usagi.core.util.ext.getDisplayMessage
import org.draken.usagi.core.util.ext.printStackTraceDebug
import tsuki.config.ConfigKey
import tsuki.model.MangaSource

@AndroidEntryPoint
class BrowserActivity : BaseBrowserActivity(), DomainRedirectDialogFragment.Callback {
	private var originalDomain: String? = null
	private var currentSource: MangaSource? = null
	private var currentRepository: MangaParserRepository? = null

	override fun onCreate2(
		savedInstanceState: Bundle?,
		source: MangaSource,
		repository: MangaParserRepository?,
	) {
		currentSource = source
		currentRepository = repository
		originalDomain = repository?.domain

		setDisplayHomeAsUp(isEnabled = true, showUpAsClose = true)
		viewBinding.webView.webViewClient = BrowserClient(this, adBlock)
		lifecycleScope.launch {
			try {
				proxyProvider.applyWebViewConfig()
			} catch (e: Exception) {
				e.printStackTraceDebug()
				Snackbar.make(viewBinding.webView, e.getDisplayMessage(resources), Snackbar.LENGTH_LONG).show()
			}
			if (savedInstanceState == null) {
				val url = intent?.dataString
				if (url.isNullOrEmpty()) {
					finishAfterTransition()
				} else {
					onTitleChanged(
						intent?.getStringExtra(AppRouter.KEY_TITLE) ?: getString(R.string.loading_),
						url,
					)
					viewBinding.webView.loadUrl(url)
				}
			}
		}
	}

	override fun onLoadingStateChanged(isLoading: Boolean) {
		super.onLoadingStateChanged(isLoading)
		if (!isLoading) {
			checkForDomainRedirect()
		}
	}

	private fun checkForDomainRedirect() {
		val currentUrl = viewBinding.webView.url?.toHttpUrlOrNull() ?: return
		val currentHost = currentUrl.host
		val oldDomain = originalDomain ?: return

		if (currentHost != oldDomain) {
			showDomainRedirectDialog(oldDomain, currentHost)
		}
	}

	private fun showDomainRedirectDialog(oldDomain: String, newDomain: String) {
		val sourceName = currentSource?.name.orEmpty()
		val dialog = DomainRedirectDialogFragment.newInstance(sourceName, oldDomain, newDomain)
		dialog.show(supportFragmentManager, DomainRedirectDialogFragment.TAG)
	}

	override fun onDomainRedirectAccepted(newDomain: String) {
		val source = currentSource ?: return
		val repository = currentRepository ?: return

		try {
			val config = SourceSettings(this, source)
			config[ConfigKey.Domain] = newDomain
			repository.domain = newDomain
			originalDomain = newDomain
			Snackbar.make(viewBinding.webView, R.string.domain_redirect_accepted, Snackbar.LENGTH_SHORT).show()
		} catch (e: Exception) {
			e.printStackTraceDebug()
			Snackbar.make(viewBinding.webView, e.getDisplayMessage(resources), Snackbar.LENGTH_LONG).show()
		}
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		super.onCreateOptionsMenu(menu)
		menuInflater.inflate(R.menu.opt_browser, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean =
		when (item.itemId) {
			android.R.id.home -> {
				viewBinding.webView.stopLoading()
				finishAfterTransition()
				true
			}

			R.id.action_browser -> {
				if (!router.openExternalBrowser(viewBinding.webView.url.orEmpty(), item.title)) {
					Snackbar.make(viewBinding.webView, R.string.operation_not_supported, Snackbar.LENGTH_SHORT).show()
				}
				true
			}

			else -> {
				super.onOptionsItemSelected(item)
			}
		}

	class Contract : ActivityResultContract<InteractiveActionRequiredException, Unit>() {
		override fun createIntent(
			context: Context,
			input: InteractiveActionRequiredException,
		): Intent =
			AppRouter.browserIntent(
				context = context,
				url = input.url,
				source =
					org.draken.usagi.core.model
						.MangaSource(input.sourceName),
				title = null,
			)

		override fun parseResult(
			resultCode: Int,
			intent: Intent?,
		): Unit = Unit
	}

	companion object {
		const val TAG = "BrowserActivity"
	}
}
