package com.example.rikharthu.companion.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import timber.log.Timber
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class DpadView : View {

    companion object {
        const val CENTER = 0
        const val RIGHT = 1
        const val UP = 2
        const val LEFT = 3
        const val DOWN = 4
    }

    private val paint = Paint()
    private var newX: Float = 0f
    private var newY: Float = 0f
    private var centerX: Float = 0f
    private var centerY: Float = 0f
    private var borderRadius = 0f
    private var centerRadius = 0f
    var currentSector: Int = CENTER
        private set(newValue) {
            field = newValue
            listener?.invoke(field)
        }
    var listener: ((Int) -> Unit)? = null

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    override fun onDraw(canvas: Canvas) {
        canvas.drawCircle(newX, newY, 20f, paint.apply {
            color = Color.RED
            style = Paint.Style.FILL
        })
        canvas.drawCircle(centerX, centerY, 20f, paint.apply { color = Color.CYAN })
        canvas.drawCircle(centerX, centerY, borderRadius, paint.apply {
            color = Color.BLACK
            strokeWidth = 2f
            style = Paint.Style.STROKE
        })

        val distShort = centerRadius * sqrt(2f) / 2
        val distLong = borderRadius * sqrt(2f) / 2
        val x1 = centerX + distShort
        val y1 = centerY - distShort
        val x2 = centerX + distLong
        val y2 = centerY - distLong
        val x3 = centerX - distLong
        val y3 = centerY - distLong
        val x4 = centerX - distShort
        val y4 = centerY - distShort

        canvas.drawCircle(x1, y1, 5f, paint)
        canvas.drawCircle(x2, y2, 5f, paint)
        canvas.drawCircle(x3, y3, 5f, paint)
        canvas.drawCircle(x4, y4, 5f, paint)

        val outerBorder = RectF()
        val centerBorder = RectF()
        outerBorder.set(centerX - borderRadius, centerY - borderRadius, centerX + borderRadius, centerY + borderRadius)
        centerBorder.set(centerX - centerRadius, centerY - centerRadius, centerX + centerRadius, centerY + centerRadius)
        canvas.drawRect(outerBorder, paint.apply { color = Color.CYAN })

        val path = Path()
        path.moveTo(x1, y1)
        path.lineTo(x2, y2)
        // Ark
        path.arcTo(outerBorder, -45f, -90f)
        path.lineTo(x4, y4)
        path.arcTo(centerBorder, -135f, 90f)
        path.moveTo(x1, y1)
        path.close()

        canvas.drawPath(path, paint.apply {
            strokeWidth = 10f
            color = Color.BLUE
            style = if (currentSector == UP) {
                Paint.Style.FILL
            } else {
                Paint.Style.STROKE
            }
        })

        // Center
        canvas.drawCircle(centerX, centerY, centerRadius, paint.apply {
            style = if (currentSector == CENTER) Paint.Style.FILL else Paint.Style.STROKE
        })

        super.onDraw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var newSector = currentSector
        if (event.action == MotionEvent.ACTION_UP) {
            // Released
            newX = centerX
            newY = centerY
            newSector = CENTER
        } else {

            val displacement = calculateDisplacement(event.x, event.y)
            if (displacement < borderRadius) {
                newX = event.x
                newY = event.y
            } else {
                // TODO nullify, it's a d-pad, not a joystick
                // Out of bounds
                val ratio = borderRadius / displacement
                // Constraint
                newX = centerX + (event.x - centerX) * ratio
                newY = centerY + (event.y - centerY) * ratio
            }

            if (displacement < centerRadius) {
                Timber.d("Center")
                newSector = CENTER
            } else {

                val x = newX - centerX
                val y = centerY - newY
                var angle = 90 - Math.toDegrees(Math.atan2(x.toDouble(), y.toDouble())).toFloat()
                angle = if (angle < 0) angle + 360 else angle

                if (angle <= 45 || angle >= 315) {
                    Timber.d("Right")
                    newSector = RIGHT
                } else if (angle in 45..135) {
                    Timber.d("Top")
                    newSector = UP
                } else if (angle in 135..225) {
                    Timber.d("Left")
                    newSector = LEFT
                } else {
                    Timber.d("Down")
                    newSector = DOWN
                }
            }
        }

        if (currentSector != newSector) {
            currentSector = newSector
            invalidate()
        }

        return true
    }

    private fun calculateDisplacement(x: Float, y: Float) =
            sqrt((x - centerX).pow(2) + (y - centerY).pow(2))

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = (w / 2).toFloat()
        centerY = (h / 2).toFloat()
        borderRadius = (min(w, h) / 2).toFloat()
        centerRadius = borderRadius / 3
        Timber.d("Border radius is $borderRadius")
    }
}