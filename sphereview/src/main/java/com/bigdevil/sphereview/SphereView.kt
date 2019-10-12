package com.bigdevil.sphereview

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.core.animation.doOnEnd
import androidx.core.view.children
import androidx.core.view.get
import kotlin.math.*

class SphereView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    companion object {
        const val GOLDEN_RATIO = 0.6180339887498949
    }

    var mMinScale: Float

    var mMaxScale: Float

    var mMinAlpha: Float

    var mMaxElevation: Int

    var mLoopSpeed = 0

    var mLoopAngle = 0
        set(value) {
            field = value
            mLoopRadian = field * PI / 180
        }

    private var mLoopRadian = 0.0

    private var mNeedLoop = false

    private var mIsLooping = false

    private var mRadius = 0

    private var mLastX = 0

    private var mLastY = 0

    private var mOffsetX = 0

    private var mOffsetY = 0

    private var mXozTotalOffsetRadian = 0.0

    private var mYozTotalOffsetRadian = 0.0

    private var mChildCountChange = false

    private var mIsFirstLayout = true

    private val mTouchSlop by lazy { ViewConfiguration.get(context).scaledTouchSlop }

    private val mCenter by lazy { Point() }

    private val mLoopRunnable by lazy {
        object : Runnable {
            override fun run() {
                mOffsetX = (mLoopSpeed * cos(mLoopRadian)).toInt()
                mOffsetY = (mLoopSpeed * sin(mLoopRadian)).toInt()
                relayout()
                post(this)
            }
        }
    }

    private val mTempCoordinate by lazy { Coordinate3D() }

    private val mChangeAnimator by lazy {
        val animator = ValueAnimator.ofFloat(0f, 1f).setDuration(500)
        animator.addUpdateListener {
            for (i in 0 until childCount) {
                val coordinate = this[i].getTag(R.id.tag_item_coordinate) as Coordinate3D
                val oldCoordinate = this[i].getTag(R.id.tag_item_old_coordinate) as Coordinate3D

                val dx = coordinate.x - oldCoordinate.x
                val dy = coordinate.y - oldCoordinate.y
                val dz = coordinate.z - oldCoordinate.z

                mTempCoordinate.x = oldCoordinate.x + dx * it.animatedFraction
                mTempCoordinate.y = oldCoordinate.y + dy * it.animatedFraction
                mTempCoordinate.z = oldCoordinate.z + dz * it.animatedFraction
                layoutChild(this[i], mTempCoordinate)
            }
        }
        animator.doOnEnd { if (mNeedLoop) start() }
        animator
    }


    data class Coordinate3D(
        var x: Double = 0.0,
        var y: Double = 0.0,
        var z: Double = 0.0
    )

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.SphereView, defStyleAttr, 0)
        mMinScale = a.getFloat(R.styleable.SphereView_min_scale, .3f)
        mMaxScale = a.getFloat(R.styleable.SphereView_max_scale, 1f)
        mMinAlpha = a.getFloat(R.styleable.SphereView_min_alpha, .3f)
        mMaxElevation = a.getDimensionPixelSize(R.styleable.SphereView_max_elevation, dp2px(10f))
        mLoopSpeed = a.getDimensionPixelSize(R.styleable.SphereView_loop_speed, 2)
        mLoopAngle = a.getInt(R.styleable.SphereView_loop_angle, 45)
        a.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        measureChildren(widthMeasureSpec, heightMeasureSpec)
        val width = measureWidth(widthMeasureSpec)
        val height = measureHeight(heightMeasureSpec)
        mRadius = min(width, height) / 2
        mCenter.x = width / 2
        mCenter.y = height / 2
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

        if (heightMode == MeasureSpec.EXACTLY) {
            return MeasureSpec.getSize(heightMeasureSpec)
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
            return (maxHeight + minHeight) / 2 * 5
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (mIsFirstLayout || mChildCountChange) {
            for (i in 0 until childCount) {
                val coordinate = this[i].getTag(R.id.tag_item_coordinate) as Coordinate3D
                val oldCoordinate = this[i].getTag(R.id.tag_item_old_coordinate) as Coordinate3D

                val z = mRadius * ((2 * i + 1.0) / childCount - 1)
                val x = sqrt(mRadius * mRadius - z * z) * cos(2 * PI * (i + 1) * GOLDEN_RATIO)
                val y = sqrt(mRadius * mRadius - z * z) * sin(2 * PI * (i + 1) * GOLDEN_RATIO)

                oldCoordinate.x = coordinate.x
                oldCoordinate.y = coordinate.y
                oldCoordinate.z = coordinate.z

                coordinate.x = x
                coordinate.y = y
                coordinate.z = z

                if (!mIsFirstLayout) {
                    updateCoordinate(coordinate, mXozTotalOffsetRadian, mYozTotalOffsetRadian)
                } else {
                    layoutChild(this[i], coordinate)
                }
            }

            if (!mIsFirstLayout) {
                stop()
                mChangeAnimator.start()
                mChildCountChange = false
            } else {
                mIsFirstLayout = false
                mChildCountChange = false
            }
        } else {
            relayout()
        }
    }

    private fun relayout() {
        val xozOffsetRadian = -offset2Radian(mOffsetX)
        val yozOffsetRadian = -offset2Radian(mOffsetY)

        mXozTotalOffsetRadian += xozOffsetRadian
        mYozTotalOffsetRadian += yozOffsetRadian
        while (mXozTotalOffsetRadian > PI) mXozTotalOffsetRadian -= 2 * PI
        while (mXozTotalOffsetRadian < -PI) mXozTotalOffsetRadian += 2 * PI
        while (mYozTotalOffsetRadian > PI) mYozTotalOffsetRadian -= 2 * PI
        while (mYozTotalOffsetRadian < PI) mYozTotalOffsetRadian += 2 * PI

        for (child in children) {
            val coordinate = child.getTag(R.id.tag_item_coordinate) as Coordinate3D
            updateCoordinate(coordinate, xozOffsetRadian, yozOffsetRadian)
            layoutChild(child, coordinate)
        }
    }

    private fun layoutChild(child: View, coordinate: Coordinate3D) {
        child.alpha = z2Alpha(coordinate.z).toFloat()
        val scale = z2Scale(coordinate.z).toFloat()
        child.scaleX = scale
        child.scaleY = scale
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            child.z = z2Elevation(coordinate.z).toFloat()
        }

        child.layout(
            coordinate.x.toInt() + mCenter.x - child.measuredWidth / 2,
            coordinate.y.toInt() + mCenter.y - child.measuredHeight / 2,
            coordinate.x.toInt() + mCenter.x + child.measuredWidth / 2,
            coordinate.y.toInt() + mCenter.y + child.measuredHeight / 2
        )
    }

    override fun onViewAdded(child: View?) {
        mChildCountChange = true
        child?.setTag(R.id.tag_item_coordinate, Coordinate3D())
        child?.setTag(R.id.tag_item_old_coordinate, Coordinate3D())
    }

    override fun onViewRemoved(child: View?) {
        mChildCountChange = true
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        val x = event.x.toInt()
        val y = event.y.toInt()
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                stop()
                mLastX = x
                mLastY = y
            }
            MotionEvent.ACTION_MOVE -> {
                if (abs(x - mLastX) > mTouchSlop || abs(y - mLastY) > mTouchSlop) {
                    mLastX = x
                    mLastY = y
                    return true
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                if (mNeedLoop) start()
                return false
            }
        }
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x.toInt()
        val y = event.y.toInt()
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                mOffsetX = x - mLastX
                mOffsetY = y - mLastY
                mLastX = x
                mLastY = y
                mLoopRadian = atan2(mOffsetY.toDouble(), mOffsetX.toDouble())
                requestLayout()
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                if (mNeedLoop) start()
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
        mMinAlpha + (mMaxScale - mMinAlpha) * (z + mRadius) / (2 * mRadius)

    private fun z2Scale(z: Double) =
        mMinScale + (mMaxScale - mMinScale) * (z + mRadius) / (2 * mRadius)

    private fun z2Elevation(z: Double) = mMaxElevation * (z + mRadius) / (2 * mRadius)

    private fun offset2Radian(offset: Int) = PI * offset / (2 * mRadius)

    private fun dp2px(dp: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }

    private fun start() {
        if (!mIsLooping) {
            post(mLoopRunnable)
            mIsLooping = true
        }
    }

    private fun stop() {
        if (mIsLooping) {
            handler.removeCallbacks(mLoopRunnable)
            mIsLooping = false
        }
    }

    fun startLoop() {
        mNeedLoop = true
        start()
    }

    fun stopLoop() {
        mNeedLoop = false
        stop()
    }
}