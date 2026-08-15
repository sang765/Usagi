package org.draken.usagi.settings.appearance

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.google.android.material.color.utilities.CorePalette
import org.draken.usagi.core.prefs.CustomColorScheme
import kotlin.math.roundToInt

class CustomColorSchemePreviewView
	@JvmOverloads
	constructor(
		context: Context,
		attrs: AttributeSet? = null,
	) : View(context, attrs) {
		private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
		private val rect = RectF()
		private var scheme = CustomColorScheme(CustomColorScheme.DEFAULT_NAME, CustomColorScheme.DEFAULT_SEED_COLOR)
		private var dark = false

		init {
			setLayerType(View.LAYER_TYPE_SOFTWARE, null)
			val mode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
			dark = mode == android.content.res.Configuration.UI_MODE_NIGHT_YES
			isFocusable = true
		}

		fun setScheme(value: CustomColorScheme) {
			scheme = value
			invalidate()
		}

		override fun onMeasure(
			widthMeasureSpec: Int,
			heightMeasureSpec: Int,
		) {
			val width = MeasureSpec.getSize(widthMeasureSpec)
			setMeasuredDimension(width, (width * 0.72f).roundToInt())
		}

		override fun onDraw(canvas: Canvas) {
			super.onDraw(canvas)
			val palette = CorePalette.of(scheme.seedColor)
			val primary = palette.a1.tone(if (dark) 80 else 40)
			val onPrimary = palette.a1.tone(if (dark) 20 else 100)
			val secondary = palette.a2.tone(if (dark) 80 else 40)
			val surface = palette.n1.tone(if (dark) 6 else 98)
			val surfaceContainer = palette.n1.tone(if (dark) 12 else 94)
			val onSurface = palette.n1.tone(if (dark) 90 else 10)
			val outline = palette.n2.tone(if (dark) 60 else 50)

			canvas.drawColor(surface)
			val padding = width * 0.06f
			val radius = padding * 0.9f
			paint.color = surfaceContainer
			rect.set(padding, padding, width - padding, height - padding)
			canvas.drawRoundRect(rect, radius, radius, paint)

			paint.color = primary
			val headerHeight = height * 0.22f
			rect.set(padding, padding, width - padding, padding + headerHeight)
			canvas.drawRoundRect(rect, radius, radius, paint)

			paint.color = onPrimary
			paint.textSize = width * 0.045f
			paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
			canvas.drawText(scheme.name, padding * 2, padding + headerHeight * 0.62f, paint)

			paint.color = onSurface
			paint.typeface = android.graphics.Typeface.DEFAULT
			paint.textSize = width * 0.04f
			canvas.drawText("Preview", padding * 2, padding + headerHeight + padding * 1.7f, paint)

			val cardTop = padding + headerHeight + padding * 2.4f
			paint.color = surface
			rect.set(padding * 2, cardTop, width * 0.58f, height - padding * 2)
			canvas.drawRoundRect(rect, radius * 0.75f, radius * 0.75f, paint)
			paint.style = Paint.Style.STROKE
			paint.strokeWidth = 2f
			paint.color = outline
			canvas.drawRoundRect(rect, radius * 0.75f, radius * 0.75f, paint)
			paint.style = Paint.Style.FILL
			paint.color = onSurface
			paint.textSize = width * 0.035f
			canvas.drawText("Aa  Sample text", padding * 2.8f, cardTop + padding * 2.4f, paint)
			paint.color = secondary
			rect.set(padding * 2.8f, cardTop + padding * 3.4f, width * 0.49f, cardTop + padding * 4.1f)
			canvas.drawRoundRect(rect, padding, padding, paint)

			paint.color = primary
			rect.set(width * 0.64f, cardTop, width - padding * 2, cardTop + height * 0.2f)
			canvas.drawRoundRect(rect, radius * 0.75f, radius * 0.75f, paint)
			paint.color = onPrimary
			paint.textSize = width * 0.033f
			canvas.drawText("Button", width * 0.70f, cardTop + height * 0.12f, paint)

			paint.color = secondary
			rect.set(width * 0.64f, cardTop + height * 0.25f, width - padding * 2, cardTop + height * 0.45f)
			canvas.drawRoundRect(rect, radius * 0.75f, radius * 0.75f, paint)
			paint.color = onPrimary
			canvas.drawText("Accent", width * 0.70f, cardTop + height * 0.37f, paint)
		}
	}
