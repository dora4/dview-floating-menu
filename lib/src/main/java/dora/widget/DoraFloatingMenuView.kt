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

/**
 * 自定义八扇形浮动菜单控件。
 *
 * 支持：
 * 1. 设置每个扇形的文字和背景颜色
 * 2. 设置中心圆文字和颜色
 * 3. 中心圆和扇形点击回调
 */
class DoraFloatingMenuView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ------------------ 触摸控制相关 ------------------
    private var downX = 0f
    private var downY = 0f
    private var isDragging = false
    private var touchSlop = 10
    private var arcCircleGap = 10 // 内外圆间距

    // ------------------ 扇形文字与颜色 ------------------
    private var defaultLabels = arrayOf("A", "B", "C", "D", "E", "F", "G", "H") // 默认文字，从上面开始顺时针排列
    private var labels = defaultLabels // 当前文字
    private var defaultArcColor = Color.BLACK
    private var arcColors: Array<Int> = Array(8) { defaultArcColor } // 每个扇形颜色

    // ------------------ 中心圆 ------------------
    private var centerLabel = "Start" // 中心圆文字
    private var centerColor: Int = Color.BLACK // 中心圆填充颜色

    // ------------------ 文本与画笔 ------------------
    private var menuTextColor: Int = Color.WHITE
    private var menuTextSize = 40f

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val centerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { // 中心圆边框
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = 4f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = menuTextColor
        textAlign = Paint.Align.CENTER
        textSize = menuTextSize
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { // 扇形分割线
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = 4f
    }

    // ------------------ 圆心与半径 ------------------
    private var centerX = 0f
    private var centerY = 0f
    private var outerRadius = 0f
    private var innerRadius = 0f

    // ------------------ 回调 ------------------
    var onSectorClick: ((index: Int) -> Unit)? = null
    var onCenterClick: (() -> Unit)? = null

    init {
        arcPaint.style = Paint.Style.FILL
        centerPaint.style = Paint.Style.FILL
    }

    // ------------------ 扇形操作方法 ------------------

    /**
     * 同时更新某个扇形的文字和颜色
     * @param index 扇形索引（0~7）
     * @param label 扇形文字
     * @param color 扇形背景颜色
     */
    fun updateArc(index: Int, label: String, color: Int) {
        updateArcLabel(index, label)
        updateArcColor(index, color)
    }

    fun clearArc(index: Int) {
        clearArcLabel(index)
        updateArcColor(index, defaultArcColor)
    }

    fun setDefaultArcColor(color: Int) {
        this.defaultArcColor = color
        this.arcColors = Array(8) { color }
        invalidate()
    }

    /** 设置某个扇形背景颜色 */
    fun updateArcColor(index: Int, color: Int) {
        if (index in 0..7) {
            arcColors[index] = color
            invalidate()
        }
    }

    /** 设置某个扇形文字 */
    fun updateArcLabel(index: Int, label: String) {
        if (index in labels.indices) {
            labels[index] = label
            invalidate()
        }
    }

    /** 清除扇形文字，恢复默认文字 */
    fun clearArcLabel(index: Int) {
        if (index in labels.indices) {
            labels[index] = defaultLabels[index]
            invalidate()
        }
    }

    /** 批量设置所有扇形文字 */
    fun setArcLabels(labels: Array<String>) {
        if (labels.size != 8) return
        this.defaultLabels = labels
        this.labels = labels
        invalidate()
    }

    /** 恢复所有扇形文字为默认值 */
    fun resetArcLabels() {
        labels = defaultLabels
        invalidate()
    }

    // ------------------ 中心圆操作 ------------------

    /** 设置中心圆文字 */
    fun setCenterLabel(label: String) {
        centerLabel = label
        invalidate()
    }

    /** 设置中心圆填充颜色 */
    fun setCenterColor(color: Int) {
        centerColor = color
        invalidate()
    }

    /** 同时设置中心圆文字和颜色 */
    fun setCenterLabelAndColor(label: String, color: Int) {
        setCenterLabel(label)
        setCenterColor(color)
    }

    // ------------------ 测量与布局 ------------------

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        outerRadius = min(centerX, centerY) - arcCircleGap
        innerRadius = outerRadius / 2.5f
    }

    // ------------------ 绘制 ------------------

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val rectOuter = RectF(centerX - outerRadius, centerY - outerRadius, centerX + outerRadius, centerY + outerRadius)
        val rectInner = RectF(centerX - innerRadius, centerY - innerRadius, centerX + innerRadius, centerY + innerRadius)

        // 绘制8个扇形
        for (i in labels.indices) {
            arcPaint.color = arcColors[i]
            val startAngle = i * 45f - 90f
            canvas.drawArc(rectOuter, startAngle, 45f, true, arcPaint)

            // 挖空中心圆
            arcPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            canvas.drawArc(rectInner, startAngle, 45f, true, arcPaint)
            arcPaint.xfermode = null

            // 绘制扇形文字
            val midAngle = Math.toRadians((startAngle + 22.5).toDouble())
            val textRadius = (outerRadius + innerRadius) / 2
            val textX = (centerX + textRadius * cos(midAngle)).toFloat()
            val textY = (centerY + textRadius * sin(midAngle)).toFloat() - (textPaint.descent() + textPaint.ascent()) / 2
            canvas.drawText(labels[i], textX, textY, textPaint)

            // 绘制扇形分割线
            val lineAngle = Math.toRadians((startAngle + 45f).toDouble())
            val startX = centerX + innerRadius * cos(lineAngle).toFloat()
            val startY = centerY + innerRadius * sin(lineAngle).toFloat()
            val stopX = centerX + outerRadius * cos(lineAngle).toFloat()
            val stopY = centerY + outerRadius * sin(lineAngle).toFloat()
            canvas.drawLine(startX, startY, stopX, stopY, linePaint)
        }

        // 绘制中心圆边框
        canvas.drawCircle(centerX, centerY, innerRadius - 10, centerBorderPaint)
        // 绘制中心圆填充
        canvas.drawCircle(centerX, centerY, innerRadius - 10, centerPaint)
        // 绘制中心文字
        val centerTextY = centerY - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(centerLabel, centerX, centerTextY, textPaint)
    }

    // ------------------ 触摸事件 ------------------

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
                // 点击中心圆
                if (dist <= innerRadius - arcCircleGap) {
                    onCenterClick?.invoke()
                    return true
                }
                // 点击扇形区域
                else if (dist <= outerRadius) {
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
