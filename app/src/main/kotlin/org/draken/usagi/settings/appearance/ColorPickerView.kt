package org.draken.usagi.settings.appearance

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max

/**
 * A compact HSV color picker used by the custom Material 3 color scheme editor.
 *
 * The upper area controls saturation and value. The lower strip controls hue.
 */
class ColorPickerView
	@JvmOverloads
	constructor(
		context: Context,
		attrs: AttributeSet? = null,
	) : View(context, attrs) {
		private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
		private val rect = RectF()
		private val hsv = floatArrayOf(270f, 0.6f, 0.64f)
		private val hueColors =
			intArrayOf(
				Color.RED,
				Color.MAGENTA,
				Color.BLUE,
				Color.CYAN,
				Color.GREEN,
				Color.YELLOW,
				Color.RED,
			)
		private var activeArea = ActiveArea.NONE
		private var onColorChangedListener: ((Int) -> Unit)? = null

		private enum class ActiveArea {
			NONE,
			SATURATION_VALUE,
			HUE,
		}

		fun setColor(color: Int) {
			Color.colorToHSV(color, hsv)
			invalidate()
		}

		fun getColor(): Int = Color.HSVToColor(hsv)

		fun setOnColorChangedListener(listener: ((Int) -> Unit)?) {
			onColorChangedListener = listener
		}

		override fun onDraw(canvas: Canvas) {
			super.onDraw(canvas)
			val horizontalPadding = paddingLeft.toFloat().coerceAtLeast(dp(4f))
			val top = paddingTop.toFloat()
			val bottom = height - paddingBottom.toFloat()
			val hueHeight = dp(28f)
			val gap = dp(14f)
			val squareBottom = max(top, bottom - hueHeight - gap)
			val square = RectF(horizontalPadding, top, width - paddingRight.toFloat(), squareBottom)
			val hue = RectF(horizontalPadding, squareBottom + gap, width - paddingRight.toFloat(), bottom)

			if (square.width() > 0f && square.height() > 0f) {
				val baseColor = Color.HSVToColor(floatArrayOf(hsv[0], 1f, 1f))
				paint.shader =
					LinearGradient(
						square.left,
						0f,
						square.right,
						0f,
						Color.WHITE,
						baseColor,
						Shader.TileMode.CLAMP,
					)
				canvas.drawRoundRect(square, dp(10f), dp(10f), paint)
				paint.shader =
					LinearGradient(
						0f,
						square.top,
						0f,
						square.bottom,
						0x00000000,
						Color.BLACK,
						Shader.TileMode.CLAMP,
					)
				canvas.drawRoundRect(square, dp(10f), dp(10f), paint)

				val saturationX = square.left + hsv[1] * square.width()
				val valueY = square.bottom - hsv[2] * square.height()
				drawSelector(canvas, saturationX, valueY, dp(8f))
			}

			if (hue.width() > 0f && hue.height() > 0f) {
				paint.shader =
					LinearGradient(
						hue.left,
						0f,
						hue.right,
						0f,
						hueColors,
						null as FloatArray?,
						Shader.TileMode.CLAMP,
					)
				canvas.drawRoundRect(hue, hue.height() / 2f, hue.height() / 2f, paint)
				val hueX = hue.left + hsv[0] / 360f * hue.width()
				drawSelector(canvas, hueX, hue.centerY(), dp(7f))
			}
			paint.shader = null
		}

		override fun onTouchEvent(event: MotionEvent): Boolean {
			val horizontalPadding = paddingLeft.toFloat().coerceAtLeast(dp(4f))
			val top = paddingTop.toFloat()
			val bottom = height - paddingBottom.toFloat()
			val hueHeight = dp(28f)
			val gap = dp(14f)
			val squareBottom = max(top, bottom - hueHeight - gap)
			val hueTop = squareBottom + gap
			val x = event.x.coerceIn(horizontalPadding, width - paddingRight.toFloat())

			when (event.actionMasked) {
				MotionEvent.ACTION_DOWN -> {
					activeArea =
						when {
							event.y <= squareBottom -> ActiveArea.SATURATION_VALUE
							event.y >= hueTop -> ActiveArea.HUE
							else -> ActiveArea.NONE
						}
					if (activeArea == ActiveArea.NONE) return false
					parent?.requestDisallowInterceptTouchEvent(true)
					updateColor(x, event.y, horizontalPadding, squareBottom)
					return true
				}

				MotionEvent.ACTION_MOVE -> {
					if (activeArea == ActiveArea.NONE) return false
					updateColor(x, event.y, horizontalPadding, squareBottom)
					return true
				}

				MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
					parent?.requestDisallowInterceptTouchEvent(false)
					activeArea = ActiveArea.NONE
					return true
				}
			}
			return true
		}

		private fun updateColor(
			x: Float,
			y: Float,
			left: Float,
			squareBottom: Float,
		) {
			val right = width - paddingRight.toFloat()
			when (activeArea) {
				ActiveArea.SATURATION_VALUE -> {
					val squareHeight = max(1f, squareBottom - paddingTop.toFloat())
					hsv[1] = ((x - left) / max(1f, right - left)).coerceIn(0f, 1f)
					hsv[2] = (1f - (y - paddingTop.toFloat()) / squareHeight).coerceIn(0f, 1f)
				}

				ActiveArea.HUE -> {
					hsv[0] = ((x - left) / max(1f, right - left) * 360f).coerceIn(0f, 360f)
				}

				ActiveArea.NONE -> {
					return
				}
			}
			invalidate()
			onColorChangedListener?.invoke(getColor())
		}

		private fun drawSelector(
			canvas: Canvas,
			x: Float,
			y: Float,
			radius: Float,
		) {
			paint.shader = null
			paint.style = Paint.Style.FILL
			paint.color = Color.WHITE
			canvas.drawCircle(x, y, radius, paint)
			paint.style = Paint.Style.STROKE
			paint.strokeWidth = dp(2f)
			paint.color = Color.BLACK
			canvas.drawCircle(x, y, radius, paint)
			paint.style = Paint.Style.FILL
		}

		private fun dp(value: Float): Float = value * resources.displayMetrics.density
	}
