package com.safebuffer.app.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

/**
 * 구간 선택 파형.
 *
 * ★ 축 방향: 왼쪽 끝 = (표시 범위)만큼 과거, 오른쪽 끝 = 지금.
 *   표시 범위는 위의 1시간 / 1일 / 전체 버튼이 정한다.
 *   재생은 왼쪽(오래된 소리)에서 시작해 오른쪽(지금)으로 흐른다.
 *
 * 말소리가 있던 구간은 진하게 그려서 무음을 피해 고를 수 있게 한다.
 * 양쪽 손잡이를 끌어 구간을 정하고, 재생 중에는 진행 위치가 선으로 표시된다.
 *
 * ★ 좌우에 여백(sidePad)을 둔다. 손잡이가 화면 끝에 붙으면 그 바깥을 누를 수 없어
 *   특히 오른쪽 손잡이를 오른손으로 잡기 어렵다. 여백을 두고, 여백 영역을 눌러도
 *   손잡이가 잡히게 한다.
 */
class BufferTimelineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val bucketCount = 140
    private var buckets = IntArray(bucketCount) { -1 }

    /** 선택 구간. 0 = 표시 범위 끝(왼쪽, 과거), 1 = 지금(오른쪽) */
    private var selNear = 0f
    private var selFar = 1f

    /** 재생 위치 (0~1). 음수면 재생 중 아님 */
    private var playhead = -1f

    private val minGap = 0.01f
    private var dragging = 0   // 0 없음, 1 왼쪽(최근), 2 오른쪽(과거)

    /** 좌우 여백(px). 손잡이가 이 여백 안쪽에서 움직여 끝에 붙지 않는다 */
    private val sidePad = 40f

    var onRangeChanged: ((nearRatio: Float, farRatio: Float) -> Unit)? = null

    private val quietPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2A3050")
    }
    private val voicePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7F77DD")
    }
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#181826")
    }
    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B00B0B14")
    }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#A5A0F0")
    }
    private val playPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF6B6B")
    }

    /** 파형이 실제로 그려지는 가로 영역 (여백 제외) */
    private fun plotLeft() = sidePad
    private fun plotRight() = width - sidePad
    private fun plotWidth() = (plotRight() - plotLeft()).coerceAtLeast(1f)

    /** 비율(0~1) → 화면 x px */
    private fun ratioToX(r: Float) = plotLeft() + r * plotWidth()
    /** 화면 x px → 비율(0~1) */
    private fun xToRatio(x: Float) = ((x - plotLeft()) / plotWidth()).coerceIn(0f, 1f)

    fun setBuckets(data: IntArray) {
        buckets = if (data.size == bucketCount) data
                  else IntArray(bucketCount) { i ->
                      data.getOrElse(i * data.size / bucketCount) { -1 }
                  }
        invalidate()
    }

    fun setSelection(near: Float, far: Float) {
        selNear = near.coerceIn(0f, 1f)
        selFar = far.coerceIn(selNear + minGap, 1f)
        invalidate()
    }

    fun setPlayhead(ratio: Float) {
        playhead = ratio
        invalidate()
    }

    fun selectionNear(): Float = selNear
    fun selectionFar(): Float = selFar
    fun bucketCount(): Int = bucketCount

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (width <= 0) return false
        val r = xToRatio(event.x)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // 손잡이 실제 위치(px)와의 거리로 판정 — 여백을 눌러도 가까운 손잡이가 잡힌다
                val dNear = abs(event.x - ratioToX(selNear))
                val dFar  = abs(event.x - ratioToX(selFar))
                dragging = if (dNear <= dFar) 1 else 2
                parent?.requestDisallowInterceptTouchEvent(true)
                applyDrag(r); return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragging != 0) { applyDrag(r); return true }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragging = 0
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun applyDrag(r: Float) {
        if (dragging == 1) selNear = r.coerceIn(0f, selFar - minGap)
        else                selFar = r.coerceIn(selNear + minGap, 1f)
        invalidate()
        onRangeChanged?.invoke(selNear, selFar)
    }

    override fun onDraw(canvas: Canvas) {
        val h = height.toFloat()
        val left = plotLeft()
        val pw = plotWidth()
        if (h <= 0f || pw <= 0f) return

        val barW = pw / bucketCount
        val gap = (barW * 0.2f).coerceAtMost(1.5f)

        for (i in 0 until bucketCount) {
            val x = left + i * barW
            val v = buckets[i]
            if (v < 0) {
                canvas.drawRect(x + gap, h * 0.48f, x + barW - gap, h * 0.52f, emptyPaint)
                continue
            }
            val barH = (h * 0.12f) + (h * 0.78f) * (v / 100f)
            val top = (h - barH) / 2f
            canvas.drawRect(
                x + gap, top, x + barW - gap, top + barH,
                if (v >= 12) voicePaint else quietPaint
            )
        }

        val nx = ratioToX(selNear)
        val fx = ratioToX(selFar)

        // 선택 밖은 어둡게 (여백까지 전부)
        if (nx > 0f) canvas.drawRect(0f, 0f, nx, h, dimPaint)
        if (fx < width.toFloat()) canvas.drawRect(fx, 0f, width.toFloat(), h, dimPaint)

        // 재생 위치
        if (playhead in 0f..1f) {
            val px = ratioToX(playhead)
            canvas.drawRect(px - 1.5f, 0f, px + 1.5f, h, playPaint)
        }

        // 손잡이 — 두껍게. 세로 막대 + 가운데 큰 그립
        val bar = 5f
        val capW = 12f
        val capR = 6f
        for (hx in listOf(nx, fx)) {
            canvas.drawRect(hx - bar, 0f, hx + bar, h, handlePaint)
            canvas.drawRoundRect(
                hx - capW, h / 2 - h * 0.30f, hx + capW, h / 2 + h * 0.30f,
                capR, capR, handlePaint
            )
        }
    }
}
