package org.draken.usagi.browser

import android.app.Dialog
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.draken.usagi.R

class DomainRedirectDialogFragment : DialogFragment() {
	private var gestureDetector: GestureDetector? = null

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		val sourceName = arguments?.getString(ARG_SOURCE_NAME).orEmpty()
		val oldDomain = arguments?.getString(ARG_OLD_DOMAIN).orEmpty()
		val newDomain = arguments?.getString(ARG_NEW_DOMAIN).orEmpty()

		val message = getString(R.string.domain_redirect_message, sourceName, oldDomain, newDomain)

		return MaterialAlertDialogBuilder(requireContext(), theme)
			.setTitle(R.string.domain_redirect_title)
			.setMessage(message)
			.setPositiveButton(R.string.yes) { _, _ ->
				(parentFragment as? Callback)?.onDomainRedirectAccepted(newDomain)
					?: (activity as? Callback)?.onDomainRedirectAccepted(newDomain)
			}
			.setNegativeButton(R.string.no) { dialog, _ ->
				dialog.dismiss()
			}
			.create()
			.also { dialog ->
				dialog.setOnShowListener {
					setupSwipeGesture(dialog)
				}
			}
	}

	private fun setupSwipeGesture(dialog: AlertDialog) {
		val rootView = dialog.findViewById<View>(android.R.id.content) ?: return
		gestureDetector = GestureDetector(
			requireContext(),
			object : GestureDetector.SimpleOnGestureListener() {
				private val SWIPE_THRESHOLD = 100
				private val SWIPE_VELOCITY_THRESHOLD = 100

				override fun onFling(
					e1: MotionEvent?,
					e2: MotionEvent,
					velocityX: Float,
					velocityY: Float,
				): Boolean {
					if (e1 == null) return false
					val diffX = e2.x - e1.x
					val diffY = e2.y - e1.y
					val absDiffX = kotlin.math.abs(diffX)
					val absDiffY = kotlin.math.abs(diffY)

					if (absDiffX > SWIPE_THRESHOLD && absDiffX > absDiffY && absDiffX > kotlin.math.abs(velocityX)) {
						// Swipe left or right -> dismiss (no)
						dialog.dismiss()
						return true
					}
					if (absDiffY > SWIPE_THRESHOLD && absDiffY > absDiffX && absDiffY > kotlin.math.abs(velocityY)) {
						// Swipe down -> dismiss (no)
						dialog.dismiss()
						return true
					}
					return false
				}
			},
		)

		rootView.setOnTouchListener { _, event ->
			gestureDetector?.onTouchEvent(event) ?: false
		}
	}

	interface Callback {
		fun onDomainRedirectAccepted(newDomain: String)
	}

	companion object {
		const val TAG = "DomainRedirectDialog"
		private const val ARG_SOURCE_NAME = "source_name"
		private const val ARG_OLD_DOMAIN = "old_domain"
		private const val ARG_NEW_DOMAIN = "new_domain"

		fun newInstance(
			sourceName: String,
			oldDomain: String,
			newDomain: String,
		): DomainRedirectDialogFragment =
			DomainRedirectDialogFragment().apply {
				arguments =
					bundleOf(
						ARG_SOURCE_NAME to sourceName,
						ARG_OLD_DOMAIN to oldDomain,
						ARG_NEW_DOMAIN to newDomain,
					)
			}
	}
}
