package com.devstart.startrescue.util

import java.text.SimpleDateFormat
import java.util.*

/** Gera um timestamp seguro para nomes de arquivos */
fun timestamp(): String {
    val sdf = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault())
    return sdf.format(Date())
}
