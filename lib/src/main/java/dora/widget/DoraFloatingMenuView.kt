package dora.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import dora.widget.floatingmenu.R
import kotlin.math.*

class DoraFloatingMenuView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var downX = 0f
    private var downY = 0f
    private var arcCircleGap = 10
    private var defaultLabels = arrayOf("A", "B", "C", "D", "E", "F", "G", "H")
    private var labels = defaultLabels
    private var centerLabel = "Start"
    private var isDragging = false
    private var touchSlop = 10
    private var arcColor = Color.BLACK
    private var centerColor = Color.BLACK
    private var menuTextColor = Color.WHITE
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

    override fun onFinishInflate() {
        super.onFinishInflate()
        touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    }

    private fun initPaints() {
        arcPaint.apply {
            style = Paint.Style.FILL
            color = arcColor
        }
        centerPaint.apply {
            style = Paint.Style.FILL
            color = centerColor
        }
        textPaint.apply {
            color = menuTextColor
            textAlign = Paint.Align.CENTER
            textSize = menuTextSize
        }
    }

    /**
     * 设置默认的文字。
     */
    fun initLabels(labels: Array<String>) {
        if (labels.size != 8) {
            return
        }
        this.defaultLabels = labels
        this.labels = labels
        invalidate()
    }

    fun resetLabels() {
        labels = defaultLabels
        invalidate()
    }

    // 清空某个 index 的 label
    fun clearLabel(index: Int) {
        if (index in labels.indices) {
            labels[index] = ""
            invalidate()
        }
    }

    // 更新某个 index 的 label
    fun updateLabel(index: Int, label: String) {
        if (index in labels.indices) {
            labels[index] = label
            invalidate()
        }
    }

    fun setCenterLabel(label: String) {
        this.centerLabel = label
        invalidate()
    }

    private fun initAttrs(context: Context, attrs: AttributeSet?) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.DoraFloatingMenuView)
        arcColor = ta.getColor(R.styleable.DoraFloatingMenuView_dview_fm_arcColor, arcColor)
        centerColor = ta.getColor(R.styleable.DoraFloatingMenuView_dview_fm_centerColor, centerColor)
        menuTextColor = ta.getColor(R.styleable.DoraFloatingMenuView_dview_fm_menuTextColor, menuTextColor)
        menuTextSize = ta.getDimension(R.styleable.DoraFloatingMenuView_dview_fm_menuTextSize, menuTextSize)
        ta.recycle()
    }

    init {
        initAttrs(context, attrs)
        initPaints()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val defaultSizeDp = 200
        val density = resources.displayMetrics.density
        val defaultSizePx = (defaultSizeDp * density).toInt()
        val width = resolveSize(defaultSizePx, widthMeasureSpec)
        val height = resolveSize(defaultSizePx, heightMeasureSpec)
        setMeasuredDimension(width, height)
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

        val rectOuter = RectF(
            centerX - outerRadius,
            centerY - outerRadius,
            centerX + outerRadius,
            centerY + outerRadius
        )

        val rectInner = RectF(
            centerX - innerRadius,
            centerY - innerRadius,
            centerX + innerRadius,
            centerY + innerRadius
        )

        for (i in labels.indices) {
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

            val lineAngle = Math.toRadians((startAngle + 45f).toDouble()) // 下一块的起始角度
            val startX = centerX + innerRadius * cos(lineAngle).toFloat()
            val startY = centerY + innerRadius * sin(lineAngle).toFloat()
            val stopX = centerX + outerRadius * cos(lineAngle).toFloat()
            val stopY = centerY + outerRadius * sin(lineAngle).toFloat()

            linePaint.color = Color.WHITE
            linePaint.strokeWidth = 4f
            canvas.drawLine(startX, startY, stopX, stopY, linePaint)
        }
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
                val upX = event.x
                val upY = event.y
                val dx = upX - centerX
                val dy = upY - centerY
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
