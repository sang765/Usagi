package org.draken.usagi.core.ui.widgets

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.graphics.ColorUtils
import com.google.android.material.color.MaterialColors

class ShimmerBlockView
	@JvmOverloads
	constructor(
		context: Context,
		attrs: AttributeSet? = null,
	) : View(context, attrs) {
		private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
		private val block = RectF()
		private val density = resources.displayMetrics.density
		private val cornerRadius = 6f * density
		private val baseColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainerHighest, Color.GRAY)
		private val highlightColor =
			ColorUtils.blendARGB(
				baseColor,
				MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface, Color.WHITE),
				0.55f,
			)
		private var progress = -1f
		private val animator =
			ValueAnimator.ofFloat(-1.5f, 1.5f).apply {
				duration = 1_100L
				interpolator = LinearInterpolator()
				repeatCount = ValueAnimator.INFINITE
				addUpdateListener {
					progress = it.animatedValue as Float
					invalidate()
				}
			}

		override fun onAttachedToWindow() {
			super.onAttachedToWindow()
			startShimmer()
		}

		override fun onDetachedFromWindow() {
			stopShimmer()
			super.onDetachedFromWindow()
		}

		override fun onDraw(canvas: Canvas) {
			val horizontalPadding = 2f * density
			val verticalPadding = 8f * density
			val iconSize = (height - verticalPadding * 2).coerceAtMost(32f * density)
			val textStart = iconSize + 12f * density
			val titleEnd = textStart + 140f * density
			val metadataEnd = textStart + 220f * density
			val shineCenter = width * progress
			paint.shader =
				LinearGradient(
					shineCenter - height,
					0f,
					shineCenter + height,
					height.toFloat(),
					intArrayOf(baseColor, highlightColor, baseColor),
					floatArrayOf(0.2f, 0.5f, 0.8f),
					Shader.TileMode.CLAMP,
				)

			drawBlock(canvas, horizontalPadding, verticalPadding, horizontalPadding + iconSize, verticalPadding + iconSize)
			drawBlock(canvas, textStart, verticalPadding + 1f * density, titleEnd, verticalPadding + 15f * density)
			drawBlock(canvas, textStart, verticalPadding + 22f * density, metadataEnd, verticalPadding + 34f * density)
			paint.shader = null
		}

		private fun drawBlock(
			canvas: Canvas,
			left: Float,
			top: Float,
			right: Float,
			bottom: Float,
		) {
			val inset = density * 2f
			block.set(left, top, right.coerceAtMost(width - inset), bottom.coerceAtMost(height - inset))
			canvas.drawRoundRect(block, cornerRadius, cornerRadius, paint)
		}

		fun startShimmer() {
			if (!animator.isStarted) animator.start()
		}

		fun stopShimmer() {
			if (animator.isStarted) animator.cancel()
			progress = -1f
		}
	}
