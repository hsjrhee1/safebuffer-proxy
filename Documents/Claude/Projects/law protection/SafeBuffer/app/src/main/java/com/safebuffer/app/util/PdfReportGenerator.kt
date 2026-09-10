package com.safebuffer.app.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.safebuffer.app.ui.evidence.LockedEvidence
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object PdfReportGenerator {

    private val dateFmt = SimpleDateFormat("yyyy년 MM월 dd일 HH:mm:ss", Locale.KOREA)
    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.KOREA)
    private val fileFmt = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA)

    fun generate(context: Context, evidence: LockedEvidence): File {
        val pageWidth = 595
        val pageHeight = 842
        val margin = 50f
        val contentWidth = (pageWidth - margin * 2).toInt()

        // Paint 설정
        val titlePaint = TextPaint().apply {
            color = Color.parseColor("#1565C0")
            textSize = 22f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val headingPaint = TextPaint().apply {
            color = Color.parseColor("#333333")
            textSize = 13f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val bodyPaint = TextPaint().apply {
            color = Color.parseColor("#222222")
            textSize = 11f
            isAntiAlias = true
        }
        val smallPaint = TextPaint().apply {
            color = Color.parseColor("#888888")
            textSize = 9f
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#DDDDDD")
            strokeWidth = 0.8f
        }

        // 전체 본문을 StaticLayout으로 미리 렌더링
        val bodyText = evidence.transcriptText.ifBlank { "(대화 내용 없음)" }
        val bodyLayout = StaticLayout.Builder
            .obtain(bodyText, 0, bodyText.length, bodyPaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(4f, 1f)
            .build()

        // 헤더 높이: 기본 200 + 원본 증명 필드 (hash 40 + stamp 40)
        val hasHash   = evidence.chunk.fileHash != null
        val hasStamp  = evidence.chunk.serverTimestamp != null
        val headerH   = 200f + (if (hasHash) 40f else 0f) + (if (hasStamp) 40f else 0f)
        val lineH = bodyLayout.height.toFloat()
        val footerH = 60f
        val usableFirst = pageHeight - margin - headerH - footerH
        val usableFull  = pageHeight - margin * 2 - footerH

        // 몇 페이지가 필요한지 계산
        val pageCount = if (lineH <= usableFirst) 1
            else 1 + kotlin.math.ceil((lineH - usableFirst) / usableFull).toInt()

        val doc = PdfDocument()

        for (pageIndex in 0 until pageCount) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = doc.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            var y = margin

            if (pageIndex == 0) {
                // 제목
                canvas.drawText("SafeBuffer 증거 리포트", margin, y + 20f, titlePaint)
                y += 38f
                canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
                y += 16f

                // 메타 정보
                canvas.drawText("생성 일시", margin, y, headingPaint)
                canvas.drawText(dateFmt.format(Date()), margin + 90f, y, bodyPaint)
                y += 20f

                canvas.drawText("증거 시작", margin, y, headingPaint)
                canvas.drawText(dateFmt.format(Date(evidence.chunk.startTimeMs)), margin + 90f, y, bodyPaint)
                y += 20f

                canvas.drawText("증거 종료", margin, y, headingPaint)
                canvas.drawText(dateFmt.format(Date(evidence.chunk.endTimeMs)), margin + 90f, y, bodyPaint)
                y += 20f

                // ── 원본 증명 정보 ──
                val hash = evidence.chunk.fileHash
                val serverTime = evidence.chunk.serverTimestamp
                val token = evidence.chunk.serverToken?.substringBefore("|") // UUID 부분만

                // 해시/토큰용 극소 폰트 (Paint는 재사용하지 않도록 별도 생성)
                val hashPaint = TextPaint().apply {
                    color = Color.parseColor("#555555")
                    textSize = 8f
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.MONOSPACE
                }

                if (hash != null) {
                    canvas.drawText("파일해시", margin, y, headingPaint)
                    // SHA-256 = 64자, 32자씩 두 줄로 나눔
                    canvas.drawText(hash.substring(0, 32), margin + 90f, y, hashPaint)
                    y += 13f
                    canvas.drawText(hash.substring(32), margin + 90f, y, hashPaint)
                    y += 18f
                }
                if (serverTime != null && token != null) {
                    canvas.drawText("서버인증", margin, y, headingPaint)
                    canvas.drawText(serverTime, margin + 90f, y, bodyPaint)
                    y += 16f
                    canvas.drawText("토큰", margin, y, headingPaint)
                    canvas.drawText(token, margin + 90f, y, hashPaint)
                    y += 18f
                }

                canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
                y += 14f
                canvas.drawText("대화 내용", margin, y, headingPaint)
                y += 18f
            }

            // StaticLayout을 캔버스에 클리핑해서 그리기
            canvas.save()
            canvas.translate(margin, y)

            // 이 페이지에서 그릴 세로 시작/끝 오프셋 계산
            val drawStart = if (pageIndex == 0) 0f
                else usableFirst + (pageIndex - 1) * usableFull
            val drawEnd = if (pageIndex == 0) usableFirst
                else drawStart + usableFull

            canvas.clipRect(0f, 0f, contentWidth.toFloat(), drawEnd - drawStart)
            canvas.translate(0f, -drawStart)
            bodyLayout.draw(canvas)
            canvas.restore()

            // 마지막 페이지 법적 고지
            if (pageIndex == pageCount - 1) {
                val footerY = pageHeight - margin - 30f
                canvas.drawLine(margin, footerY - 10f, pageWidth - margin, footerY - 10f, linePaint)
                canvas.drawText(
                    "※ 본 녹음은 통신비밀보호법 제3조에 의거 대화 당사자가 직접 녹음한 적법한 증거입니다.",
                    margin, footerY, smallPaint
                )
                val stampNote = if (evidence.chunk.serverTimestamp != null)
                    "   SHA-256 해시 + 제3자 서버 타임스탬프로 원본 무결성이 검증되었습니다."
                else
                    "   SHA-256 파일 해시가 기록되었습니다."
                canvas.drawText(stampNote, margin, footerY + 13f, smallPaint)
                canvas.drawText(
                    "   SafeBuffer 생성 / 페이지 ${pageIndex + 1}/$pageCount",
                    margin, footerY + 26f, smallPaint
                )
            }

            doc.finishPage(page)
        }

        val dir = File(context.getExternalFilesDir(null), "reports").also { it.mkdirs() }
        val file = File(dir, "SafeBuffer_${fileFmt.format(Date(evidence.chunk.startTimeMs))}.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()

        return file
    }
}
