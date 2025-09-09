package com.devstart.startrescue.util

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlin.math.ceil

/**
 * Gera um PDF A4 com TABELA RESPONSIVA a partir de linhas de texto.
 * A 1ª linha de "linhas" é o cabeçalho da tabela.
 *
 * @param header Título do relatório.
 * @param linhas Linhas da tabela; a primeira linha deve conter os nomes das colunas.
 */

/* =================== INTERNAL =================== */

private fun buildHtml(title: String, rows: List<List<String>>): String {
    val css = """
      :root{
        --ink:#0f172a; --muted:#64748b; --border:#e5e7eb;
        --thead:#0f172a; --thead-ink:#ffffff;
        --row:#ffffff; --row-alt:#f8fafc;
        --green:#22c55e; --yellow:#f59e0b; --red:#ef4444; --black:#111827;
      }
      *{box-sizing:border-box}
      body{
        margin:18px; color:var(--ink);
        font-family:system-ui,-apple-system,Segoe UI,Roboto,Ubuntu,Arial,sans-serif;
        -webkit-print-color-adjust:exact; print-color-adjust:exact;
      }
      h1{margin:0 0 10px 0; font-size:18pt}
      .meta{color:var(--muted); margin:4px 0 12px 0; font-size:10pt}
      .wrap{width:100%}
      table{width:100%; border-collapse:collapse; table-layout:fixed}
      th,td{
        border:1px solid var(--border);
        padding:6px 8px; font-size:9pt; vertical-align:top; word-wrap:break-word
      }
      thead th{background:var(--thead); color:var(--thead-ink); font-weight:700}
      tbody tr:nth-child(odd) td{background:var(--row)}
      tbody tr:nth-child(even) td{background:var(--row-alt)}

      .tag{display:inline-block; padding:2px 6px; border-radius:8px; color:#fff; font-weight:700; font-size:8pt}
      .tag.green{background:var(--green)} .tag.yellow{background:var(--yellow)}
      .tag.red{background:var(--red)} .tag.black{background:var(--black)}

      thead{display:table-header-group}
      tfoot{display:table-footer-group}
      tr{page-break-inside:avoid}
      @page{size:A4; margin:12mm}
    """.trimIndent()

    fun esc(s: String) = s
        .replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
        .replace("\"","&quot;")

    // badge automático para cores
    fun badgeIfColor(text: String): String {
        val t = text.trim().uppercase()
        val cls = when {
            "VERDE" in t -> "green"
            "AMARELO" in t -> "yellow"
            "VERMELHO" in t -> "red"
            "ÓBITO" in t || "OBITO" in t -> "black"
            else -> null
        }
        return if (cls != null) """<span class="tag $cls">${esc(text)}</span>""" else esc(text)
    }

    // converte sexo para M/F
    fun mapSexToInitial(value: String): String {
        val v = value.trim().lowercase()
        return when {
            v.startsWith("f") || v == "feminino" -> "F"
            v.startsWith("m") || v == "masculino" -> "M"
            else -> value
        }
    }

    val hasRows = rows.isNotEmpty()
    val headerCells = if (hasRows) rows.first() else emptyList()
    val dataRows = if (hasRows) rows.drop(1) else emptyList()

    val thead = if (headerCells.isNotEmpty())
        "<tr>${headerCells.joinToString("") { "<th>${esc(it)}</th>" }}</tr>"
    else ""

    val sxIndex = headerCells.indexOfFirst { it.trim().equals("Sx", ignoreCase = true) }

    val tbody = dataRows.joinToString("") { r ->
        val cols = r.mapIndexed { idx, cell ->
            val norm = if (sxIndex >= 0 && idx == sxIndex) mapSexToInitial(cell) else cell
            "<td>${badgeIfColor(norm)}</td>"
        }
        "<tr>${cols.joinToString("")}</tr>"
    }

    return """
      <!doctype html>
      <html lang="pt-br">
      <head>
        <meta charset="utf-8"/>
        <meta name="viewport" content="width=device-width,initial-scale=1"/>
        <title>Relatório</title>
        <style>$css</style>
      </head>
      <body>
        <div class="wrap">
          <h1>${esc(title)}</h1>
          <table>
            <thead>$thead</thead>
            <tbody>$tbody</tbody>
          </table>
        </div>
      </body>
      </html>
    """.trimIndent()
}

private fun htmlToPdfA4(
    context: Context,
    html: String,
    outUri: Uri,
    onFinish: (Boolean) -> Unit
) {
    val web = WebView(context)
    web.settings.javaScriptEnabled = false

    val a4w = 1240
    val a4h = 1754

    web.layoutParams = android.view.ViewGroup.LayoutParams(a4w, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
    web.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String?) {
            val wSpec = View.MeasureSpec.makeMeasureSpec(a4w, View.MeasureSpec.EXACTLY)
            val hSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            web.measure(wSpec, hSpec)
            web.layout(0, 0, a4w, web.measuredHeight)

            val totalH = web.measuredHeight
            val pages = ceil(totalH.toDouble() / a4h).toInt().coerceAtLeast(1)

            val pdf = PdfDocument()
            try {
                repeat(pages) { idx ->
                    val info = PdfDocument.PageInfo.Builder(a4w, a4h, idx + 1).create()
                    val page = pdf.startPage(info)
                    val c = page.canvas
                    c.save()
                    c.translate(0f, (-idx * a4h).toFloat())
                    web.draw(c)
                    c.restore()
                    pdf.finishPage(page)
                }
                context.contentResolver.openOutputStream(outUri)?.use { os -> pdf.writeTo(os) }
                onFinish(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onFinish(false)
            } finally {
                pdf.close()
                web.destroy()
            }
        }
    }
    web.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
}
