package com.safebuffer.app.util

import android.content.Context
import com.safebuffer.app.ui.evidence.LockedEvidence
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 외부 라이브러리 없이 ZIP+XML로 .docx 생성.
 * Word / 한글 / LibreOffice 모두 호환.
 */
object DocxReportGenerator {

    private val dateFmt = SimpleDateFormat("yyyy년 MM월 dd일 HH:mm:ss", Locale.KOREA)
    private val fileFmt = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA)

    fun generate(context: Context, evidence: LockedEvidence): File {
        // filesDir 사용 — getExternalFilesDir()가 null인 기기에서도 안전
        val dir = File(context.filesDir, "reports").also { it.mkdirs() }
        val file = File(dir, "SafeBuffer_${fileFmt.format(Date(evidence.chunk.startTimeMs))}.docx")

        ZipOutputStream(file.outputStream().buffered()).use { zos ->
            fun entry(name: String, content: String) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            entry("[Content_Types].xml", contentTypes())
            entry("_rels/.rels", rels())
            entry("word/_rels/document.xml.rels", documentRels())
            entry("word/document.xml", documentXml(evidence))
            entry("word/settings.xml", settings())
        }

        return file
    }

    // ── DOCX 구조 XML ────────────────────────────────────────────

    private fun contentTypes() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
  <Override PartName="/word/settings.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.settings+xml"/>
</Types>"""

    private fun rels() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""

    private fun documentRels() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/settings" Target="settings.xml"/>
</Relationships>"""

    private fun settings() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:settings xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:defaultTabStop w:val="720"/>
</w:settings>"""

    // ── 본문 XML ─────────────────────────────────────────────────

    private fun documentXml(evidence: LockedEvidence): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:body>""")

        // 제목
        sb.append(para("SafeBuffer 증거 리포트", bold = true, size = 32, color = "1565C0"))
        sb.append(para(""))

        // 메타 정보
        sb.append(labelValue("생성 일시", dateFmt.format(Date())))
        sb.append(labelValue("증거 시작", dateFmt.format(Date(evidence.chunk.startTimeMs))))
        sb.append(labelValue("증거 종료", dateFmt.format(Date(evidence.chunk.endTimeMs))))

        // 원본 증명
        val hash = evidence.chunk.fileHash
        val serverTime = evidence.chunk.serverTimestamp
        val token = evidence.chunk.serverToken?.substringBefore("|")

        if (hash != null || serverTime != null) {
            sb.append(para(""))
            sb.append(para("▣ 원본 증명", bold = true, size = 24, color = "1A4A8A"))
        }
        if (hash != null) {
            sb.append(labelValue("파일 해시 (SHA-256)", hash, mono = true))
        }
        if (serverTime != null) {
            sb.append(labelValue("서버 인증 시각", serverTime))
        }
        if (token != null) {
            sb.append(labelValue("서버 토큰", token, mono = true))
        }

        // 대화 내용
        sb.append(para(""))
        sb.append(para("▣ 대화 내용", bold = true, size = 24, color = "1A4A8A"))
        sb.append(para(""))

        val transcriptText = evidence.transcriptText.ifBlank { "(대화 내용 없음)" }
        transcriptText.split("\n").forEach { line ->
            sb.append(para(xmlEscape(line)))
        }

        // 법적 고지
        sb.append(para(""))
        sb.append(divider())
        sb.append(para(
            "※ 본 녹음은 통신비밀보호법 제3조에 의거 대화 당사자가 직접 녹음한 적법한 증거입니다.",
            size = 18, color = "888888"
        ))
        val stampNote = if (serverTime != null)
            "   SHA-256 해시 + 제3자 서버 타임스탬프로 원본 무결성이 검증되었습니다."
        else "   SHA-256 파일 해시가 기록되었습니다."
        sb.append(para(stampNote, size = 18, color = "888888"))
        sb.append(para("   SafeBuffer 생성", size = 18, color = "888888"))

        sb.append("<w:sectPr/>")
        sb.append("</w:body></w:document>")
        return sb.toString()
    }

    // ── XML 빌더 헬퍼 ────────────────────────────────────────────

    /** 단순 단락 */
    private fun para(
        text: String,
        bold: Boolean = false,
        size: Int = 22,          // half-points (22 = 11pt)
        color: String? = null,
        mono: Boolean = false
    ): String {
        val rpr = buildString {
            if (bold) append("<w:b/>")
            append("<w:sz w:val=\"$size\"/>")
            if (color != null) append("<w:color w:val=\"$color\"/>")
            if (mono) append("<w:rFonts w:ascii=\"Courier New\" w:hAnsi=\"Courier New\"/>")
        }
        return "<w:p><w:r><w:rPr>$rpr</w:rPr><w:t xml:space=\"preserve\">$text</w:t></w:r></w:p>"
    }

    /** 레이블(굵게) + 값 한 줄 */
    private fun labelValue(label: String, value: String, mono: Boolean = false): String {
        val rpr = buildString {
            append("<w:sz w:val=\"22\"/>")
            if (mono) append("<w:rFonts w:ascii=\"Courier New\" w:hAnsi=\"Courier New\"/>")
        }
        return """<w:p>
  <w:r><w:rPr><w:b/><w:sz w:val="22"/></w:rPr><w:t xml:space="preserve">${xmlEscape(label)}:  </w:t></w:r>
  <w:r><w:rPr>$rpr</w:rPr><w:t xml:space="preserve">${xmlEscape(value)}</w:t></w:r>
</w:p>"""
    }

    /** 수평선 (단락 아래 테두리) */
    private fun divider() = """<w:p><w:pPr><w:pBdr>
  <w:bottom w:val="single" w:sz="4" w:space="1" w:color="DDDDDD"/>
</w:pBdr></w:pPr></w:p>"""

    private fun xmlEscape(text: String) = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
