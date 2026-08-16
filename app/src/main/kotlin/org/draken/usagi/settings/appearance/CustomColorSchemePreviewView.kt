package org.draken.usagi.settings.appearance

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import org.draken.usagi.core.prefs.CustomColorRole
import org.draken.usagi.core.prefs.CustomColorScheme
import org.draken.usagi.core.prefs.CustomColorSchemeStore
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
		private var colors = CustomColorSchemeStore.resolvedColors(context, scheme)

		init {
			setLayerType(View.LAYER_TYPE_SOFTWARE, null)
			isFocusable = true
		}

		fun setScheme(value: CustomColorScheme) {
			scheme = value
			colors = CustomColorSchemeStore.resolvedColors(context, value)
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
			val primary = colors.getValue(CustomColorRole.PRIMARY.key)
			val onPrimary = colors.getValue(CustomColorRole.ON_PRIMARY.key)
			val secondary = colors.getValue(CustomColorRole.SECONDARY.key)
			val surface = colors.getValue(CustomColorRole.SURFACE.key)
			val surfaceContainer = colors.getValue(CustomColorRole.SURFACE_CONTAINER.key)
			val onSurface = colors.getValue(CustomColorRole.ON_SURFACE.key)
			val outline = colors.getValue(CustomColorRole.OUTLINE.key)

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
