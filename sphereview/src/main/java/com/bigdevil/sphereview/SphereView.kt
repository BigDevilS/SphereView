package com.bigdevil.sphereview

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Point
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.core.animation.doOnEnd
import androidx.core.view.children
import kotlin.math.*

class SphereView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    companion object {
        private const val GOLDEN_RATIO = 0.6180339887498949
    }

    var minScale = 0f
        set(value) {
            field = value
            configChange()
        }

    var maxScale = 0f
        set(value) {
            field = value
            configChange()
        }

    var minAlpha = 0f
        set(value) {
            field = value
            configChange()
        }

    var maxElevation = 0
        set(value) {
            field = value
            configChange()
        }

    var loopSpeed = 0f

    var loopAngle = 0
        set(value) {
            field = value
            loopRadian = field * PI / 180
        }

    private var loopRadian = 0.0
    private var needLoop = false
    private var isLooping = false

    private var radius = 0

    private var lastX = 0f
    private var lastY = 0f
    private var offsetX = 0f
    private var offsetY = 0f

    private var xozTotalOffsetRadian = 0.0
    private var yozTotalOffsetRadian = 0.0

    private var childCountChange = false

    private var skipLayout = false

    private var tracePointerId = 0

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private val center by lazy { Point() }

    private val loopRunnable by lazy {
        object : Runnable {
            override fun run() {
                offsetX = (loopSpeed * cos(loopRadian)).toFloat()
                offsetY = (loopSpeed * sin(loopRadian)).toFloat()
                applyTranslate()
                postDelayed(this, 10)
            }
        }
    }

    private val tempCoordinate by lazy { Coordinate3D() }

    private val changeAnimator by lazy {
        ValueAnimator.ofFloat(0f, 1f).setDuration(500).apply {
            addUpdateListener {
                for (child in children) {
                    val coordinate = child.getTag(R.id.tag_item_coordinate) as Coordinate3D
                    val oldCoordinate = child.getTag(R.id.tag_item_old_coordinate) as Coordinate3D

                    val dx = coordinate.x - oldCoordinate.x
                    val dy = coordinate.y - oldCoordinate.y
                    val dz = coordinate.z - oldCoordinate.z

                    tempCoordinate.x = oldCoordinate.x + dx * it.animatedFraction
                    tempCoordinate.y = oldCoordinate.y + dy * it.animatedFraction
                    tempCoordinate.z = oldCoordinate.z + dz * it.animatedFraction
                    translateChild(child, tempCoordinate)
                }
            }
            doOnEnd { if (needLoop) this@SphereView.start() }
        }
    }


    data class Coordinate3D(
        var x: Double = 0.0,
        var y: Double = 0.0,
        var z: Double = 0.0
    )

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.SphereView, defStyleAttr, 0)
        minScale = a.getFloat(R.styleable.SphereView_min_scale, .3f)
        maxScale = a.getFloat(R.styleable.SphereView_max_scale, 1f)
        minAlpha = a.getFloat(R.styleable.SphereView_min_alpha, .3f)
        maxElevation = a.getDimensionPixelSize(R.styleable.SphereView_max_elevation, 10f.dp.toInt())
        loopSpeed = a.getFloat(R.styleable.SphereView_loop_speed, 1f)
        loopAngle = a.getInt(R.styleable.SphereView_loop_angle, 45)
        a.recycle()
    }


    fun startLoop() {
        needLoop = true
        start()
    }

    fun stopLoop() {
        needLoop = false
        stop()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        measureChildren(widthMeasureSpec, heightMeasureSpec)
        val width = measureWidth(widthMeasureSpec)
        val height = measureHeight(heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    private fun measureWidth(widthMeasureSpec: Int): Int {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)

        if (widthMode == MeasureSpec.EXACTLY) {
            return MeasureSpec.getSize(widthMeasureSpec)
        } else {
            var maxWidth = 0
            var minWidth = Int.MAX_VALUE
            for (child in children) {
                if (maxWidth < child.measuredWidth) {
                    maxWidth = child.measuredWidth
                }
                if (minWidth > child.measuredWidth) {
                    minWidth = child.measuredWidth
                }
            }
            return (maxWidth + minWidth) / 2 * 3
        }
    }

    private fun measureHeight(heightMeasureSpec: Int): Int {
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        return if (heightMode == MeasureSpec.EXACTLY) {
            MeasureSpec.getSize(heightMeasureSpec)
        } else {
            var maxHeight = 0
            var minHeight = Int.MAX_VALUE
            for (child in children) {
                if (maxHeight < child.measuredHeight) {
                    maxHeight = child.measuredHeight
                }
                if (minHeight > child.measuredHeight) {
                    minHeight = child.measuredHeight
                }
            }
            (maxHeight + minHeight) / 2 * 5
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (skipLayout) {
            return
        }
        skipLayout = true
        for ((index, child) in children.withIndex()) {
            child.layout(
                center.x - child.measuredWidth / 2,
                center.y - child.measuredHeight / 2,
                center.x + child.measuredWidth / 2,
                center.y + child.measuredHeight / 2
            )

            val coordinate = child.getTag(R.id.tag_item_coordinate) as Coordinate3D
            val oldCoordinate = child.getTag(R.id.tag_item_old_coordinate) as Coordinate3D

            val z = radius * ((2 * index + 1.0) / childCount - 1)
            val x = sqrt(radius * radius - z * z) * cos(2 * PI * (index + 1) * GOLDEN_RATIO)
            val y = sqrt(radius * radius - z * z) * sin(2 * PI * (index + 1) * GOLDEN_RATIO)

            oldCoordinate.x = coordinate.x
            oldCoordinate.y = coordinate.y
            oldCoordinate.z = coordinate.z

            coordinate.x = x
            coordinate.y = y
            coordinate.z = z

            if (childCountChange) {
                updateCoordinate(coordinate, xozTotalOffsetRadian, yozTotalOffsetRadian)
            } else {
                translateChild(child, coordinate)
            }
        }

        if (childCountChange) {
            stop()
            changeAnimator.start()
            childCountChange = false
        }
    }

    private fun applyTranslate() {
        val xozOffsetRadian = -offset2Radian(offsetX)
        val yozOffsetRadian = -offset2Radian(offsetY)

        xozTotalOffsetRadian += xozOffsetRadian
        yozTotalOffsetRadian += yozOffsetRadian
        while (xozTotalOffsetRadian > PI) xozTotalOffsetRadian -= 2 * PI
        while (xozTotalOffsetRadian < -PI) xozTotalOffsetRadian += 2 * PI
        while (yozTotalOffsetRadian > PI) yozTotalOffsetRadian -= 2 * PI
        while (yozTotalOffsetRadian < -PI) yozTotalOffsetRadian += 2 * PI

        for (child in children) {
            val coordinate = child.getTag(R.id.tag_item_coordinate) as Coordinate3D
            updateCoordinate(coordinate, xozOffsetRadian, yozOffsetRadian)
            translateChild(child, coordinate)
        }
    }

    private fun translateChild(child: View, coordinate: Coordinate3D) {
        child.alpha = z2Alpha(coordinate.z).toFloat()
        val scale = z2Scale(coordinate.z).toFloat()
        child.scaleX = scale
        child.scaleY = scale
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            child.z = z2Elevation(coordinate.z).toFloat()
        }

        child.translationX = coordinate.x.toFloat()
        child.translationY = coordinate.y.toFloat()
    }

    override fun onViewAdded(child: View) {
        childCountChange = true
        child.setTag(R.id.tag_item_coordinate, Coordinate3D())
        child.setTag(R.id.tag_item_old_coordinate, Coordinate3D())
        skipLayout = false
    }

    override fun onViewRemoved(child: View) {
        childCountChange = true
        skipLayout = false
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        radius = min(width, height) / 2
        center.x = width / 2
        center.y = height / 2
        skipLayout = false
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                stop()
                lastX = x
                lastY = y
            }
            MotionEvent.ACTION_MOVE -> {
                if (abs(x - lastX) > touchSlop || abs(y - lastY) > touchSlop) {
                    lastX = x
                    lastY = y
                    return true
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                if (needLoop) start()
                return false
            }
        }
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                stop()
                val pointerIndex = event.actionIndex
                if (pointerIndex != -1) {
                    tracePointerId = event.getPointerId(pointerIndex)
                    lastX = event.getX(pointerIndex)
                    lastY = event.getY(pointerIndex)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val pointerIndex = event.findPointerIndex(tracePointerId)
                if (pointerIndex != -1) {
                    val pointerX = event.getX(pointerIndex)
                    val pointerY = event.getY(pointerIndex)
                    offsetX = (pointerX - lastX) / 2
                    offsetY = (pointerY - lastY) / 2
                    lastX = pointerX
                    lastY = pointerY
                    loopRadian = atan2(offsetY.toDouble(), offsetX.toDouble())
                    applyTranslate()
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val actionPointId = event.getPointerId(event.actionIndex)
                if (actionPointId == tracePointerId) {
                    val tracePointerIndex = if (event.actionIndex == event.pointerCount - 1) {
                        event.pointerCount - 2
                    } else {
                        event.pointerCount - 1
                    }
                    if (tracePointerIndex != -1) {
                        tracePointerId = event.getPointerId(tracePointerIndex)
                        lastX = event.getX(tracePointerIndex)
                        lastY = event.getY(tracePointerIndex)
                    }
                }

            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                if (needLoop) start()
                return false
            }
        }
        return true
    }

    private fun updateCoordinate(
        coordinate: Coordinate3D,
        xozOffsetRadian: Double,
        yozOffsetRadian: Double
    ) {
        // 先处理xoz平面
        val newX = coordinate.x * cos(xozOffsetRadian) - coordinate.z * sin(xozOffsetRadian)
        var newZ = coordinate.x * sin(xozOffsetRadian) + coordinate.z * cos(xozOffsetRadian)

        // 再处理yoz平面
        val newY = coordinate.y * cos(yozOffsetRadian) - newZ * sin(yozOffsetRadian)
        newZ = coordinate.y * sin(yozOffsetRadian) + newZ * cos(yozOffsetRadian)

        coordinate.x = newX
        coordinate.y = newY
        coordinate.z = newZ
    }

    private fun z2Alpha(z: Double) =
        minAlpha + (1f - minAlpha) * (z + radius) / (2 * radius)

    private fun z2Scale(z: Double) =
        minScale + (maxScale - minScale) * (z + radius) / (2 * radius)

    private fun z2Elevation(z: Double) = maxElevation * (z + radius) / (2 * radius)

    private fun offset2Radian(offset: Float) = PI * offset / (2 * radius)

    private fun configChange() {
        if (!isLooping) {
            offsetX = 0f
            offsetY = 0f
            requestLayout()
        }
    }

    private fun start() {
        if (!isLooping) {
            post(loopRunnable)
            isLooping = true
        }
    }

    private fun stop() {
        if (isLooping) {
            handler.removeCallbacks(loopRunnable)
            isLooping = false
        }
    }

    private val Float.dp: Float
        get() = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this,
            Resources.getSystem().displayMetrics
        )
}