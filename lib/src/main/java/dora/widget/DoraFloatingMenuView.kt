package dora.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

class DoraFloatingMenuView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var downX = 0f
    private var downY = 0f
    private var arcCircleGap = 10

    private var defaultLabels = arrayOf("A", "B", "C", "D", "E", "F", "G", "H")
    private var labels = defaultLabels
    private var centerLabel = "Start"

    private var isDragging = false
    private var touchSlop = 10

    // 扇形颜色数组
    private var arcColors: Array<Int> = Array(8) { Color.BLACK }

    // 中心圆颜色
    private var centerColor: Int = Color.BLACK
    private var menuTextColor: Int = Color.WHITE
    private var menuTextSize = 40f

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = 4f
    }

    private var centerX = 0f
    private var centerY = 0f
    private var outerRadius = 0f
    private var innerRadius = 0f

    var onSectorClick: ((index: Int) -> Unit)? = null
    var onCenterClick: (() -> Unit)? = null

    init {
        arcPaint.style = Paint.Style.FILL
        centerPaint.style = Paint.Style.FILL
        textPaint.apply {
            color = menuTextColor
            textAlign = Paint.Align.CENTER
            textSize = menuTextSize
        }
    }

    /** 设置某个扇形的背景色 */
    fun setArcColor(index: Int, color: Int) {
        if (index in 0..7) {
            arcColors[index] = color
            invalidate()
        }
    }

    /** 设置中心圆背景色 */
    fun setCenterColor(color: Int) {
        centerColor = color
        invalidate()
    }

    /** 初始化所有文字 */
    fun setLabels(labels: Array<String>) {
        if (labels.size != 8) return
        this.defaultLabels = labels
        this.labels = labels
        invalidate()
    }

    /** 恢复所有文字到初始值 */
    fun resetLabels() {
        labels = defaultLabels
        invalidate()
    }

    fun updateLabel(index: Int, label: String) {
        if (index in labels.indices) {
            labels[index] = label
            invalidate()
        }
    }

    fun clearLabel(index: Int) {
        if (index in labels.indices) {
            labels[index] = ""
            invalidate()
        }
    }

    fun setCenterLabel(label: String) {
        centerLabel = label
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        outerRadius = min(centerX, centerY) - arcCircleGap
        innerRadius = outerRadius / 2.5f
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val rectOuter = RectF(centerX - outerRadius, centerY - outerRadius, centerX + outerRadius, centerY + outerRadius)
        val rectInner = RectF(centerX - innerRadius, centerY - innerRadius, centerX + innerRadius, centerY + innerRadius)

        for (i in labels.indices) {
            // 使用对应扇形颜色
            arcPaint.color = arcColors[i]

            val startAngle = i * 45f - 90f
            canvas.drawArc(rectOuter, startAngle, 45f, true, arcPaint)

            arcPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            canvas.drawArc(rectInner, startAngle, 45f, true, arcPaint)
            arcPaint.xfermode = null

            val midAngle = Math.toRadians((startAngle + 22.5).toDouble())
            val textRadius = (outerRadius + innerRadius) / 2
            val textX = (centerX + textRadius * cos(midAngle)).toFloat()
            val textY = (centerY + textRadius * sin(midAngle)).toFloat() - (textPaint.descent() + textPaint.ascent()) / 2
            canvas.drawText(labels[i], textX, textY, textPaint)

            val lineAngle = Math.toRadians((startAngle + 45f).toDouble())
            val startX = centerX + innerRadius * cos(lineAngle).toFloat()
            val startY = centerY + innerRadius * sin(lineAngle).toFloat()
            val stopX = centerX + outerRadius * cos(lineAngle).toFloat()
            val stopY = centerY + outerRadius * sin(lineAngle).toFloat()
            canvas.drawLine(startX, startY, stopX, stopY, linePaint)
        }

        // 中心圆
        centerPaint.color = centerColor
        canvas.drawCircle(centerX, centerY, innerRadius - 10, centerPaint)

        val centerTextY = centerY - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(centerLabel, centerX, centerTextY, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                isDragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - downX
                val dy = event.y - downY
                if (!isDragging && hypot(dx, dy) > touchSlop) {
                    isDragging = true
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (isDragging) {
                    isDragging = false
                    return true
                }
                val dx = event.x - centerX
                val dy = event.y - centerY
                val dist = hypot(dx, dy)
                if (dist <= innerRadius - arcCircleGap) {
                    onCenterClick?.invoke()
                    return true
                } else if (dist <= outerRadius) {
                    val angle = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 360) % 360
                    val fixedAngle = (angle + 90) % 360
                    val index = (fixedAngle / 45).toInt()
                    onSectorClick?.invoke(index)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }
}
