package otus.homework.customview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class CustomPieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class PieSegment(
        val category: String,
        val amount: Int,
        val color: Int,
        val percentage: Float,
        var startAngle: Float = 0f,
        var sweepAngle: Float = 0f
    )

    private var segments: List<PieSegment> = emptyList()
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 30f
        textAlign = Paint.Align.CENTER
    }

    private val gapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        style = Paint.Style.STROKE
    }

    private val rectF = RectF()
    private var onSegmentClickListener: ((String) -> Unit)? = null

    fun setData(data: Map<String, Int>) {
        val total = data.values.sum().toFloat()
        if (total == 0f) return
        
        var currentAngle = 0f

        segments = data.entries.mapIndexed { index, entry ->
            val sweepAngle = (entry.value / total) * 360f
            val percentage = (entry.value / total) * 100f
            val colorResId = resources.getIdentifier("chart_color_" + (index + 1) % 12, "color", context.packageName)
            val segment = PieSegment(
                category = entry.key,
                amount = entry.value,
                color = ContextCompat.getColor(context, colorResId),
                percentage = percentage,
                startAngle = currentAngle,
                sweepAngle = sweepAngle
            )
            currentAngle += sweepAngle
            segment
        }
        invalidate()
    }

    fun setOnSegmentClickListener(listener: (String) -> Unit) {
        onSegmentClickListener = listener
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val defaultSize = (300 * resources.displayMetrics.density).toInt()
        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> widthSize.coerceAtMost(defaultSize)
            else -> defaultSize
        }

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> heightSize.coerceAtMost(defaultSize)
            else -> defaultSize
        }

        val size = width.coerceAtMost(height)
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (segments.isEmpty()) return

        val strokeWidth = width * 0.125f
        paint.strokeWidth = strokeWidth
        
        val textSpace = 60f
        val margin = strokeWidth / 2f + textSpace
        rectF.set(margin, margin, width - margin, height - margin)

        val layerId = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)

        for (segment in segments) {
            paint.color = segment.color
            paint.style = Paint.Style.STROKE
            canvas.drawArc(rectF, segment.startAngle, segment.sweepAngle, false, paint)
        }

        if (segments.size > 1) {
            val gapWidthPx = 4 * resources.displayMetrics.density
            gapPaint.strokeWidth = gapWidthPx
            val centerX = width / 2f
            val centerY = height / 2f
            val maxRadius = width / 2f
            
            for (segment in segments) {
                val angleRad = Math.toRadians(segment.startAngle.toDouble()).toFloat()
                val stopX = centerX + cos(angleRad) * maxRadius
                val stopY = centerY + sin(angleRad) * maxRadius
                canvas.drawLine(centerX, centerY, stopX, stopY, gapPaint)
            }
        }

        canvas.restoreToCount(layerId)

        for (segment in segments) {
            val midAngleRad = Math.toRadians((segment.startAngle + segment.sweepAngle / 2f).toDouble()).toFloat()
            val textRadius = (width - margin * 2) / 2f + strokeWidth / 2f + 25f
            val textX = (width / 2f) + cos(midAngleRad) * textRadius
            val textY = (height / 2f) + sin(midAngleRad) * textRadius + (textPaint.textSize / 3f)
            
            if (segment.percentage > 2f) {
                val text = String.format(Locale.getDefault(), "%.1f%%", segment.percentage)
                canvas.drawText(text, textX, textY, textPaint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            return true
        }
        if (event.action == MotionEvent.ACTION_UP) {
            val centerX = width / 2f
            val centerY = height / 2f
            val x = event.x - centerX
            val y = event.y - centerY
            val radiusFromCenter = sqrt(x.pow(2) + y.pow(2))
            
            val strokeWidth = width * 0.125f
            val textSpace = 60f
            val margin = strokeWidth / 2f + textSpace
            
            val ringCenterRadius = (width - margin * 2) / 2f
            val outerRadius = ringCenterRadius + strokeWidth / 2f
            val innerRadius = ringCenterRadius - strokeWidth / 2f

            if (radiusFromCenter in innerRadius..outerRadius) {
                var angle = Math.toDegrees(atan2(y.toDouble(), x.toDouble())).toFloat()
                if (angle < 0) angle += 360f

                val clickedSegment = segments.find { segment ->
                    val endAngle = segment.startAngle + segment.sweepAngle
                    if (angle >= segment.startAngle && angle <= endAngle) {
                        true
                    } else if (endAngle > 360f && (angle >= segment.startAngle || angle <= endAngle % 360f)) {
                        true
                    } else {
                        false
                    }
                }
                
                clickedSegment?.let {
                    onSegmentClickListener?.invoke(it.category)
                    performClick()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onSaveInstanceState(): Parcelable {
        val bundle = Bundle()
        bundle.putParcelable("superState", super.onSaveInstanceState())

        val categories = segments.map { it.category }.toTypedArray()
        val amounts = segments.map { it.amount }.toIntArray()
        bundle.putStringArray("categories", categories)
        bundle.putIntArray("amounts", amounts)
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is Bundle) {
            val categories = state.getStringArray("categories")
            val amounts = state.getIntArray("amounts")
            if (categories != null && amounts != null && categories.size == amounts.size) {
                val data = mutableMapOf<String, Int>()
                for (i in categories.indices) {
                    data[categories[i]] = amounts[i]
                }
                setData(data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                super.onRestoreInstanceState(state.getParcelable("superState", Parcelable::class.java))
            } else {
                @Suppress("DEPRECATION")
                super.onRestoreInstanceState(state.getParcelable("superState"))
            }
        } else {
            super.onRestoreInstanceState(state)
        }
    }
}
