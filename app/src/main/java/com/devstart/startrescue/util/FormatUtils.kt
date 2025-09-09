package com.devstart.startrescue.util

import com.devstart.startrescue.StartRescueViewModel

fun formatTec(tec: Double, lang: String = "pt"): String {
    return if (tec == StartRescueViewModel.TEC_AUSENTE_VALUE) {
        when (lang) {
            "en" -> "Absent"
            "es" -> "Ausente"
            else -> "Ausente"
        }
    } else {
        "%.1fs".format(tec)
    }
}
