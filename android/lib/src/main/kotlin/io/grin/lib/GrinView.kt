package io.grin.lib

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class GrinView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val renderer = GrinBitmapRenderer()
    private var bitmap = null as android.graphics.Bitmap?
    private var player: GrinPlayer? = null
    private var imageWidth = 0
    private var imageHeight = 0
    private var resumeOnAttach = false

    fun load(file: GrinFile) {
        ensurePlayer().load(file)
        imageWidth = safeInt(file.header.width, "Width")
        imageHeight = safeInt(file.header.height, "Height")
        requestLayout()
        invalidate()
    }

    fun play() {
        ensurePlayer().play()
    }

    fun pause() {
        player?.pause()
    }

    fun stop() {
        player?.stop()
    }

    fun seek(tick: Long) {
        ensurePlayer().seek(tick)
    }

    fun getCurrentFrame(): DisplayBuffer {
        return ensurePlayer().getCurrentFrame()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val currentPlayer = player ?: return
        val buffer = currentPlayer.getCurrentFrame()
        val rendered = renderer.render(buffer, bitmap)
        bitmap = rendered

        val target = computeTargetRect(rendered.width, rendered.height)
        canvas.drawBitmap(rendered, null, target, null)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            if (player != null) {
                togglePlayback()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (resumeOnAttach) {
            player?.play()
            resumeOnAttach = false
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (player != null) {
            resumeOnAttach = true
            player?.pause()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (imageWidth == 0 || imageHeight == 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        val aspect = imageWidth.toFloat() / imageHeight.toFloat()

        var measuredWidth = width
        var measuredHeight = (width / aspect).toInt()
        if (measuredHeight > height) {
            measuredHeight = height
            measuredWidth = (height * aspect).toInt()
        }
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    private fun ensurePlayer(): GrinPlayer {
        if (player == null) {
            player = GrinPlayer(onFrameRendered = { postInvalidateOnAnimation() })
        }
        return player!!
    }

    private fun togglePlayback() {
        val current = player ?: return
        if (current.isPlaying()) {
            current.pause()
        } else {
            current.play()
        }
    }

    private fun computeTargetRect(bitmapWidth: Int, bitmapHeight: Int): Rect {
        val viewWidth = width
        val viewHeight = height
        val aspect = bitmapWidth.toFloat() / bitmapHeight.toFloat()
        var targetWidth = viewWidth
        var targetHeight = (viewWidth / aspect).toInt()
        if (targetHeight > viewHeight) {
            targetHeight = viewHeight
            targetWidth = (viewHeight * aspect).toInt()
        }
        val left = (viewWidth - targetWidth) / 2
        val top = (viewHeight - targetHeight) / 2
        return Rect(left, top, left + targetWidth, top + targetHeight)
    }
}

private fun safeInt(value: Long, label: String): Int {
    require(value <= Int.MAX_VALUE.toLong()) { "$label exceeds Int range" }
    require(value >= 0L) { "$label must be non-negative" }
    return value.toInt()
}
