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
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale



fun writeTextToUri(context: Context, uri: Uri, text: String) {
    context.contentResolver.openOutputStream(uri)?.use { out ->
        OutputStreamWriter(out, Charsets.UTF_8).use { w -> w.write(text) }
    }
}

fun answersToCsv(
    respostas: List<AnswerRecord>,
    nome: String, setor: String, ra: String, email: String?
): String {
    val sb = StringBuilder()
    fun w(line: String) { sb.append(line).append('\n') }
    w("Nome;$nome"); w("Setor/Instituição;$setor"); w("RA/Registro;$ra"); w("E-mail;${email ?: "-"}")
    w("")
    w("Q;Idade;Sexo;HemExs;TQ;Deambula;RespIni;RespPos;FR;TEC;Obedece;Gest34;Lesão;Sua;Correta;Acertou;Explicação")
    for (r in respostas) {
        // dentro do for (r in respostas) {
        val p = r.patient

        val sexoCurto = when (p.sexo.trim().lowercase()) {
            "feminino" -> "F"
            "masculino" -> "M"
            else -> if (p.sexo.startsWith("F", ignoreCase = true)) "F" else "M"
        }

        w(listOf(
            r.i,
            p.idade,
            sexoCurto, // <- aqui!
            if (p.hemExs) "SIM" else "NÃO",
            if (r.tqApplied) "SIM" else "NÃO",
            if (p.deambula) "SIM" else "NÃO",
            if (p.respIni) "SIM" else "NÃO",
            if (p.respPos) "SIM" else "NÃO",
            if (p.fr > 0) p.fr.toString() else "—",
            String.format(Locale.getDefault(), "%.1f", p.tec),
            if (p.obedece) "SIM" else "NÃO",
            if (p.gestante34) "SIM" else "NÃO",
            p.distr.replace(';', ','),
            r.sua, r.correta,
            if (r.acertou) "✔" else "✘",
            r.explic.replace(';', ',')
        ).joinToString(";"))

    }
    return sb.toString()
}

/* ---- PDF (tabela com quebra de linha, cores e sem sobreposição) ---- */
fun writeResultsPdfTable(context: Context, uri: Uri, titulo: String, respostas: List<AnswerRecord>) {
    val pageW = 842; val pageH = 595; val margin = 24
    val cellPadX = 6f; val cellPadY = 6f; val lineH = 13f
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
    fun colorForLabel(lbl: String): Int = when (lbl.uppercase(Locale.getDefault())) {
        "VERDE" -> GREEN; "AMARELO" -> YELLOW; "VERMELHO" -> RED; "ÓBITO", "OBITO" -> BLACK; else -> TXT
    }

    val cols = listOf(
        "Q" to 0.6f,  "Id" to 0.8f,  "Sx" to 0.7f,  "Hem" to 0.9f,  "TQ" to 0.8f,
        "Deamb" to 1.0f, "RIni" to 0.9f, "RPos" to 0.9f, "FR" to 0.8f, "TEC" to 0.9f,
        "Obed" to 0.9f, "Gest" to 0.9f, "Lesão" to 2.8f,
        "Sua" to 1.6f, "Correta" to 1.6f, "✓" to 0.7f, "Explicação" to 3.2f
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
        for (word in text.split(Regex("\\s+"))) {
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

    paint.textSize = 14f; paint.color = TXT
    canvas.drawText(titulo, margin.toFloat(), y + 2, paint)
    y += 22f; paint.textSize = 9.5f

    var x = margin.toFloat()
    bgPaint.color = HEADER_BG
    val headerH = (lineH + cellPadY * 2)
    canvas.drawRect(RectF(x, y, (margin + tableW).toFloat(), y + headerH), bgPaint)
    val hp = Paint(paint).apply { color = HEADER_FG }
    var cx = x
    cols.forEachIndexed { i, (title, _) ->
        canvas.drawText(title, cx + cellPadX, y + cellPadY + lineH - 3f, hp)
        cx += colW[i]
    }
    y += headerH

    respostas.forEachIndexed { idx, r ->
        val p = r.patient
        val sexoCurto = when (p.sexo.trim().lowercase()) {
            "feminino" -> "F"
            "masculino" -> "M"
            else -> if (p.sexo.startsWith("F", ignoreCase = true)) "F" else "M"
        }
        val data = listOf(
            listOf("${r.i}"), listOf("${p.idade}"), listOf(sexoCurto),                    // <-- AQUI trocado (antes era listOf("${p.sexo}"))

            listOf(if (p.hemExs) "S" else "N"), listOf(if (r.tqApplied) "S" else "N"),
            listOf(if (p.deambula) "S" else "N"), listOf(if (p.respIni) "S" else "N"),
            listOf(if (p.respPos) "S" else "N"), listOf(if (p.fr > 0) "${p.fr}" else "—"),
            listOf(String.format(Locale.getDefault(), "%.1f", p.tec)),
            listOf(if (p.obedece) "S" else "N"),
            listOf(if (p.gestante34) "S" else "N"),
            wrapSmart(p.distr,  colW[12] - cellPadX * 2),
            wrapSmart(r.sua,    colW[13] - cellPadX * 2),
            wrapSmart(r.correta,colW[14] - cellPadX * 2),
            listOf(if (r.acertou) "✔" else "✘"),
            wrapSmart(r.explic, colW[16] - cellPadX * 2)
        )
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
        data.forEachIndexed { i, lines ->
            val w = colW[i]
            val color = when (i) {
                13 -> colorForLabel(r.sua)
                14 -> colorForLabel(r.correta)
                15 -> if (r.acertou) GREEN else RED
                else -> TXT
            }
            drawCellText(canvas, cx2, y, w, lines, color)
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

/* ---- Compartilhar PDF (chooser: Gmail/Outlook/WhatsApp/Drive…) ---- */
fun sharePdf(
    context: Context,
    toEmail: String?,
    subject: String,
    body: String,
    header: String,
    respostas: List<AnswerRecord>,
    filename: String = "triagem_${timestamp()}.pdf"
) {
    val file = File(context.cacheDir, filename)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    writeResultsPdfTable(context, uri, header, respostas)

    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        if (!toEmail.isNullOrBlank()) putExtra(Intent.EXTRA_EMAIL, arrayOf(toEmail))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "Compartilhar PDF"))
}
