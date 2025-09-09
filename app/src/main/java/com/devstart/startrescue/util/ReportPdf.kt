package com.devstart.startrescue.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.devstart.startrescue.AnswerRecord
import java.io.File
import java.util.Locale

/**
 * DESENHA o PDF colorido a partir de LINHAS (16 colunas fixas):
 * ["Q","Idade","Sx","Hem","TQ","Deamb","RIni","RPos","FR","TEC","Obed","Gest","Lesão","Sua","Correta","Explicação"]
 */
fun writeResultsPdfTable(
    context: Context,
    uri: Uri,
    header: String,
    linhas: List<List<String>>
) {
    // Layout da página (A4 horizontal ~ 72dpi)
    val pageW = 842; val pageH = 595; val margin = 24
    val cellPadX = 6f; val cellPadY = 6f; val lineH = 13f

    // Cores
    val HEADER_BG = Color.parseColor("#0B1220")
    val HEADER_FG = Color.WHITE
    val ROW_A = Color.parseColor("#FFFFFF")
    val ROW_B = Color.parseColor("#EEF2F6")
    val GRID = Color.parseColor("#9AA5B1")
    val TXT = Color.parseColor("#121212")
    val GREEN = Color.parseColor("#2E7D32")
    val YELLOW = Color.parseColor("#F9A825")
    val RED = Color.parseColor("#C62828")
    val BLACK = Color.parseColor("#000000")

    fun colorForLabel(lbl: String): Int = when (lbl.trim().uppercase(Locale.getDefault())) {
        "VERDE" -> GREEN
        "AMARELO" -> YELLOW
        "VERMELHO" -> RED
        "ÓBITO", "OBITO" -> BLACK
        else -> TXT
    }

    // Índices (16 colunas)
    val IDX_Q = 0; val IDX_IDADE = 1; val IDX_SX = 2; val IDX_HEM = 3; val IDX_TQ = 4
    val IDX_DEAMB = 5; val IDX_RINI = 6; val IDX_RPOS = 7; val IDX_FR = 8; val IDX_TEC = 9
    val IDX_OBED = 10; val IDX_GEST = 11; val IDX_LES = 12; val IDX_SUA = 13; val IDX_COR = 14; val IDX_EXP = 15

    // Larguras relativas
    val cols = listOf(
        "Q" to 0.6f,  "Idade" to 0.8f,  "Sx" to 0.7f,  "Hem" to 0.9f,  "TQ" to 0.8f,
        "Deamb" to 1.0f, "RIni" to 0.9f, "RPos" to 0.9f, "FR" to 0.8f, "TEC" to 0.9f,
        "Obed" to 0.9f, "Gest" to 0.9f, "Lesão" to 2.8f,
        "Sua" to 1.6f, "Correta" to 1.6f, "Explicação" to 3.2f
    )
    val weightSum = cols.sumOf { it.second.toDouble() }.toFloat()
    val tableW = pageW - margin * 2
    val colW = cols.map { (it.second / weightSum) * tableW }

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9.5f; color = TXT }
    val linePaint = Paint().apply { color = GRID; strokeWidth = 0.8f }
    val bgPaint = Paint()

    fun wrapSmart(text: String, maxWidth: Float): List<String> {
        if (text.isBlank()) return listOf("")
        val out = mutableListOf<String>()
        var cur = StringBuilder()
        val words = text.replace("\n", " ").split(Regex("\\s+"))
        for (word in words) {
            val proposed = if (cur.isEmpty()) word else cur.toString() + " " + word
            if (paint.measureText(proposed) <= maxWidth) {
                if (cur.isEmpty()) cur.append(word) else { cur.append(' '); cur.append(word) }
            } else {
                if (cur.isNotEmpty()) { out += cur.toString(); cur = StringBuilder() }
                var start = 0
                while (start < word.length) {
                    var end = start + 1
                    while (end <= word.length && paint.measureText(word.substring(start, end)) <= maxWidth) end++
                    val chunk = word.substring(start, end - 1)
                    if (chunk.isNotEmpty()) out += chunk
                    start = end - 1
                }
            }
        }
        if (cur.isNotEmpty()) out += cur.toString()
        return out
    }

    fun drawCellText(canvas: Canvas, x: Float, yTop: Float, w: Float, lines: List<String>, color: Int) {
        val tp = Paint(paint); tp.color = color
        var y = yTop + cellPadY + lineH
        for (ln in lines) { canvas.drawText(ln, x + cellPadX, y - 3f, tp); y += lineH }
    }

    val doc = PdfDocument()
    var pageNum = 1
    fun newPage(): Pair<Canvas, PdfDocument.Page> {
        val info = PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create()
        val page = doc.startPage(info); pageNum += 1
        return page.canvas to page
    }

    var (canvas, page) = newPage()
    var y = margin.toFloat()

    // Cabeçalho do relatório
    paint.textSize = 14f; paint.color = TXT
    canvas.drawText(header, margin.toFloat(), y + 2, paint)
    y += 22f; paint.textSize = 9.5f

    // Cabeçalho da tabela
    val headerRow: List<String> = if (linhas.isNotEmpty()) linhas.first() else emptyList()

    var x = margin.toFloat()
    bgPaint.color = HEADER_BG
    val headerH = (lineH + cellPadY * 2)
    canvas.drawRect(RectF(x, y, (margin + tableW).toFloat(), y + headerH), bgPaint)
    val hp = Paint(paint).apply { color = HEADER_FG }

    var cx = x
    val headerTexts = if (headerRow.size >= 16) headerRow else headerRow + List(16 - headerRow.size) { "" }
    for (i in 0 until 16) {
        val title = headerTexts.getOrNull(i) ?: ""
        canvas.drawText(title, cx + cellPadX, y + cellPadY + lineH - 3f, hp)
        cx += colW[i]
    }
    y += headerH

    // Corpo
    val dataRows = if (linhas.size > 1) linhas.drop(1) else emptyList()

    dataRows.forEachIndexed { idx, row ->
        val cells = if (row.size >= 16) row.take(16) else row + List(16 - row.size) { "" }

        val toLines: (Int) -> List<String> = { i ->
            val w = colW[i] - cellPadX * 2
            when (i) {
                IDX_LES, IDX_SUA, IDX_COR, IDX_EXP -> wrapSmart(cells[i], w)
                else -> listOf(cells[i])
            }
        }

        val data = (0 until 16).map { toLines(it) }
        val rowLines = data.maxOf { it.size }
        val rowH = rowLines * 13f + cellPadY * 2

        if (y + rowH > pageH - margin) {
            doc.finishPage(page)
            val np = newPage(); canvas = np.first; page = np.second
            y = margin.toFloat()
        }

        bgPaint.color = if (idx % 2 == 0) ROW_A else ROW_B
        canvas.drawRect(RectF(margin.toFloat(), y, (margin + tableW).toFloat(), y + rowH), bgPaint)

        var cx2 = margin.toFloat()
        (0 until 16).forEach { i ->
            val w = colW[i]
            val color = when (i) {
                IDX_SUA -> colorForLabel(cells[i])
                IDX_COR -> colorForLabel(cells[i])
                else -> TXT
            }
            drawCellText(canvas, cx2, y, w, data[i], color)
            canvas.drawLine(cx2, y, cx2, y + rowH, linePaint)
            cx2 += w
        }
        canvas.drawLine(margin + tableW.toFloat(), y, margin + tableW.toFloat(), y + rowH, linePaint)
        canvas.drawLine(margin.toFloat(), y + rowH, margin + tableW.toFloat(), y + rowH, linePaint)

        y += rowH
    }

    doc.finishPage(page)
    context.contentResolver.openOutputStream(uri)?.use { out -> doc.writeTo(out) }
    doc.close()
}

/**
 * ADAPTADOR: converte respostas -> linhas (com Sx = M/F) e chama writeResultsPdfTable.
 * Garante que SALVAR e ENVIAR usem a MESMA renderização.
 */
fun writeResultsPdfTableFromAnswers(
    context: Context,
    uri: Uri,
    titulo: String,
    respostas: List<AnswerRecord>
) {
    fun sxShort(sexo: String) = when (sexo.trim().lowercase()) {
        "feminino" -> "F"
        "masculino" -> "M"
        else -> if (sexo.startsWith("f", ignoreCase = true)) "F" else "M"
    }
    fun sn(b: Boolean) = if (b) "S" else "N"

    val headerRow = listOf(
        "Q","Idade","Sx","Hem","TQ","Deamb","RIni","RPos",
        "FR","TEC","Obed","Gest","Lesão","Sua","Correta","Explicação"
    )

    val dataRows = respostas.map { r ->
        val p = r.patient
        val rPosStr = if (p.respIni) "—" else if (p.respPos) "S" else "N"
        listOf(
            r.i.toString(),           // Q
            p.idade.toString(),       // **IDADE**
            sxShort(p.sexo),          // Sx -> M/F
            sn(p.hemExs),             // Hem
            sn(r.tqApplied),          // TQ
            sn(p.deambula),           // Deamb
            sn(p.respIni),            // RIni
            rPosStr,                  // RPos
            p.fr.toString(),          // FR
            "%.1f".format(p.tec),     // TEC
            sn(p.obedece),            // Obed
            sn(p.gestante34),         // Gest
            p.distr,                  // Lesão
            r.sua,                    // Sua
            r.correta,                // Correta
            r.explic                  // Explicação
        )
    }

    writeResultsPdfTable(context, uri, titulo, listOf(headerRow) + dataRows)
}