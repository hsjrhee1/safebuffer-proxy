package com.safebuffer.app.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max

class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private var amplitudes: List<Float> = emptyList()
    private var progress: Float = 0f
    private var onSeekListener: ((Float) -> Unit)? = null

    private val playedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFF")
    }
    private val unplayedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E4D7A")
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF6B6B")
        strokeWidth = 3f
    }
    private val emptyLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E3A5A")
        strokeWidth = 2f
    }

    fun setAmplitudes(amps: List<Float>) {
        amplitudes = amps
        invalidate()
    }

    fun setProgress(p: Float) {
        progress = p.coerceIn(0f, 1f)
        invalidate()
    }

    fun setOnSeekListener(listener: (Float) -> Unit) {
        onSeekListener = listener
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                if (amplitudes.isEmpty()) return false
                val frac = (event.x / width.toFloat()).coerceIn(0f, 1f)
                onSeekListener?.invoke(frac)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        if (amplitudes.isEmpty()) {
            // 로딩 중: 중앙에 가는 선
            canvas.drawLine(0f, h / 2f, w, h / 2f, emptyLinePaint)
            return
        }

        val barCount = amplitudes.size
        val barWidth = w / barCount
        val gap = max(0.8f, barWidth * 0.18f)
        val playedX = w * progress

        amplitudes.forEachIndexed { i, amp ->
            val x = i * barWidth
            val barH = max(3f, amp * h * 0.85f)
            val top = (h - barH) / 2f
            val isPlayed = (x + barWidth / 2f) <= playedX
            canvas.drawRect(x + gap, top, x + barWidth - gap, top + barH,
                if (isPlayed) playedPaint else unplayedPaint)
        }

        // 재생 헤드
        if (progress > 0f) {
            canvas.drawLine(playedX, 0f, playedX, h, progressPaint)
        }
    }
}
