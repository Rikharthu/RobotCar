package com.example.rikharthu.companion.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import timber.log.Timber

private const val DEFAULT_LOOP_INTERVAL = 50L

class JoystickView : SurfaceView, SurfaceHolder.Callback, View.OnTouchListener {

    var baseColor: Int = Color.rgb(120, 120, 120)
    var handleColor: Int = Color.rgb(80, 80, 80)
    var hatColor: Int = Color.rgb(255, 0, 0)
    var loopInterval: Long = DEFAULT_LOOP_INTERVAL
    var listener: ((angle: Float, strength: Float) -> Unit)? = null


    private lateinit var reportThread: Thread
    private lateinit var drawThread: DrawThread
    private var newX: Float = 0f
    private var newY: Float = 0f
    private var centerX: Float = 0f
    private var centerY: Float = 0f
    private var handleWidth: Float = 0f
    private var handleRadius: Float = 0f
    private var hatRadius: Float = 0f
    private var baseRadius: Float = 0f
    private var surfaceHeight: Float = 0f
    private var surfaceWidth: Float = 0f

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        Timber.d("init")
        holder.addCallback(this)
        setOnTouchListener(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Timber.d("Surface created")
        drawThread = DrawThread(holder)
        drawThread.isRunning = true
        drawThread.start()
        reportThread = Thread({
            while (true) {
                // TODO calculate angle, sometimes interrupt this thread to pause and etc
                val x = newX - centerX
                val y = centerY - newY
                val abs = Math.sqrt(Math.pow(x.toDouble(), 2.0) + Math.pow(y.toDouble(), 2.0))
                var strength = abs / baseRadius
                if (strength > 1) {
                    strength = 1.0
                }
                var angle = Math.toDegrees(Math.atan2(x.toDouble(), y.toDouble()))
                angle = 90 - angle
                angle = if (angle < 0) angle + 360 else angle
                listener?.invoke(angle.toFloat(), strength.toFloat())
                Thread.sleep(loopInterval)
            }
        })
        reportThread.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Timber.d("Surface changed, width=$width, height=$height")
        surfaceWidth = width.toFloat()
        surfaceHeight = height.toFloat()
        centerX = (width / 2).toFloat()
        centerY = (height / 2).toFloat()
        newX = centerX
        newY = centerY
        val smallestSide = Math.min(surfaceWidth, surfaceHeight)
        hatRadius = smallestSide / 5
        baseRadius = smallestSide / 3
        handleWidth = smallestSide / 10
        handleRadius = handleWidth * 1.1f
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Timber.d("Surface destroyed")
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (v == this) {
            if (event.action != MotionEvent.ACTION_UP) {
                // TODO add comments
                val displacement = Math.sqrt(
                        Math.pow((event.x - centerX).toDouble(), 2.0)
                                + Math.pow((event.y - centerY).toDouble(), 2.0))
                if (displacement < baseRadius) {
                    newY = event.y
                    newX = event.x
                } else {
                    // Out of bounds
                    val ratio = baseRadius / displacement
                    val constrainedX = centerX + (event.x - centerX) * ratio
                    val constrainedY = centerY + (event.y - centerY) * ratio
                    newX = constrainedX.toFloat()
                    newY = constrainedY.toFloat()
                }
                // TODO invoke listener?
            } else {
                // Released
                if (newX != centerX || newY != centerY) {
                    newX = centerX
                    newY = centerY
                }
            }
        }
        return true
    }

    private inner class DrawThread(
            private val holder: SurfaceHolder
    ) : Thread() {

        var isRunning = false
        private var oldX: Float = 0f
        private var oldY: Float = 0f

        override fun run() {
            var canvas: Canvas?
            while (isRunning) {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    // Draw here
                    // FIXME not working properly if not drawing all the time
                    if (true || oldX != newX || oldY != newY) {
                        drawJoystick(canvas)
                        oldX = newX; oldY = newY
                    }
                    holder.unlockCanvasAndPost(canvas)
                }
            }
        }

        private fun drawJoystick(canvas: Canvas) {
            val colors = Paint()
            // Clear
            canvas.drawColor(Color.WHITE)
            // Border area
            colors.style = Paint.Style.STROKE
            colors.color = Color.BLACK
            canvas.drawCircle(centerX, centerY, baseRadius, colors)
            colors.style = Paint.Style.FILL
            // Base
            colors.color = baseColor
            canvas.drawCircle(centerX, centerY, baseRadius / 2, colors)
            // Handle
            colors.color = handleColor
            // TODO why radius / 2 ?
            canvas.drawCircle(centerX, centerY, handleRadius / 2, colors)
            colors.strokeWidth = handleWidth
            canvas.drawLine(centerX, centerY, newX, newY, colors)
            // Hat
            colors.color = hatColor
            canvas.drawCircle(newX, newY, hatRadius, colors)

        }
    }
}
