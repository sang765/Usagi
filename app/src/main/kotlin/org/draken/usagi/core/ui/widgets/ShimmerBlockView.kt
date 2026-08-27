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
import org.draken.usagi.R

class ShimmerBlockView
	@JvmOverloads
	constructor(
		context: Context,
		attrs: AttributeSet? = null,
	) : View(context, attrs) {
		private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
		private val bounds = RectF()
		private val cornerRadius = resources.getDimension(R.dimen.margin_small)
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
			bounds.set(0f, 0f, width.toFloat(), height.toFloat())
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
			canvas.drawRoundRect(bounds, cornerRadius, cornerRadius, paint)
			paint.shader = null
		}

		fun startShimmer() {
			if (!animator.isStarted) animator.start()
		}

		fun stopShimmer() {
			if (animator.isStarted) animator.cancel()
			progress = -1f
		}
	}
