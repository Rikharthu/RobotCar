package com.example.rikharthu.companion.views

import android.annotation.SuppressLint
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

    init {
        setLayerType(LAYER_TYPE_HARDWARE, paint)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        val bmp = Bitmap.createBitmap(canvas.width, canvas.height, Bitmap.Config.ARGB_8888) // TODO use other format
        val overlayCanvas = Canvas(bmp)
        // Guidelines
        val limitLine = Path()
        paint.strokeWidth = 16f
        paint.style = Paint.Style.STROKE
        paint.color = Color.RED
        limitLine.moveTo(centerX + centerRadius, centerY)
        limitLine.lineTo(centerX + borderRadius, centerY)
        val _matrix = Matrix().apply { setRotate(-45f, centerX, centerY) }
        limitLine.transform(_matrix)
        _matrix.setRotate(-90f, centerX, centerY)
        for (i in 1..4) {
            overlayCanvas.drawPath(limitLine, paint)
            limitLine.transform(_matrix)
        }
        // Circles
        overlayCanvas.drawCircle(centerX, centerY, centerRadius, paint)
        overlayCanvas.drawCircle(centerX, centerY, borderRadius, paint)

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

        val outerBorder = RectF()
        val centerBorder = RectF()
        outerBorder.set(centerX - borderRadius, centerY - borderRadius, centerX + borderRadius, centerY + borderRadius)
        centerBorder.set(centerX - centerRadius, centerY - centerRadius, centerX + centerRadius, centerY + centerRadius)

        val path = Path()
        path.moveTo(x1, y1)
        path.lineTo(x2, y2)
        // Ark
        path.arcTo(outerBorder, -45f, -90f)
        path.lineTo(x4, y4)
        path.arcTo(centerBorder, -135f, 90f)
        path.moveTo(x1, y1)
        path.close()

        paint.style = Paint.Style.FILL_AND_STROKE
        paint.strokeWidth = 1f
        paint.isAntiAlias = true
        canvas.drawPath(path, paint.apply {
            color = if (currentSector == UP) {
                Color.LTGRAY
            } else {
                Color.GRAY
            }
        })
        path.transform(Matrix().apply { preRotate(90f, centerX, centerY) })
        canvas.drawPath(path, paint.apply {
            color = if (currentSector == RIGHT) {
                Color.LTGRAY
            } else {
                Color.GRAY
            }
        })
        path.transform(Matrix().apply { preRotate(180f, centerX, centerY) })
        canvas.drawPath(path, paint.apply {
            color = if (currentSector == LEFT) {
                Color.LTGRAY
            } else {
                Color.GRAY
            }
        })
        path.transform(Matrix().apply { preRotate(-90f, centerX, centerY) })
        canvas.drawPath(path, paint.apply {
            color = if (currentSector == DOWN) {
                Color.LTGRAY
            } else {
                Color.GRAY
            }
        })

        // Center
        canvas.drawCircle(centerX, centerY, centerRadius, paint.apply {
            color = if (currentSector == CENTER) {
                Color.LTGRAY
            } else {
                Color.GRAY
            }
        })

        canvas.drawBitmap(bmp, 0f, 0f, Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT) })

        super.onDraw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var newSector: Int
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