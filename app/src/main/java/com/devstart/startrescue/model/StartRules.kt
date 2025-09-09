package com.devstart.startrescue.model

// Cores de triagem

fun labelColor(c: TriageColor): String = when (c) {
    TriageColor.GREEN -> "VERDE"
    TriageColor.YELLOW -> "AMARELO"
    TriageColor.RED -> "VERMELHO"
    TriageColor.BLACK -> "ÓBITO"
}

/**
 * Fluxograma START SEM pulso.
 * Usado quando NÃO há hemorragia exsanguinante ativa (ou quando já aplicou TQ).
 */
fun classifyStartNoPulse(p: Patient): Pair<TriageColor, String> {
    if (p.deambula) {
        return TriageColor.GREEN to "Deambula: SIM → VERDE."
    }
    if (!p.respIni) {
        return if (p.respPos) {
            // não respirava → após VA respira == VERMELHO
            Pair(TriageColor.RED, "Respirou após liberação de vias aéreas → VERMELHO.")
        } else {
            // não respirava → após VA continua sem respirar == PRETO
            Pair(TriageColor.BLACK, "Não respira mesmo após liberação de vias aéreas → PRETO (ÓBITO).")
        }
    }

    if (!p.respIni) {
        return if (!p.respPos) {
            TriageColor.BLACK to "Não respira após abrir vias aéreas → ÓBITO."
        } else {
            TriageColor.RED to "Após abrir vias aéreas passou a respirar → VERMELHO."
        }
    }
    if (p.fr > 30) {
        return TriageColor.RED to "FR=${p.fr} (>30) → VERMELHO."
    }
    if (p.tec > 2.0) {
        return TriageColor.RED to "TEC>2s → VERMELHO."
    }
    if (!p.obedece) {
        return TriageColor.RED to "Não obedece comandos → VERMELHO."
    }
    return TriageColor.YELLOW to "Parâmetros adequados (não deambula) → AMARELO."
}

/**
 * Regra de hemorragia exsanguinante + TQ:
 * - Se HEM_EXS = true e NÃO aplicou TQ → correta = VERMELHO.
 * - Se aplicou TQ, segue o fluxograma normal (classifyStartNoPulse).
 */
fun correctClassification(p: Patient, tqApplied: Boolean): Pair<TriageColor, String> {
    if (p.hemExs && !tqApplied) {
        return TriageColor.RED to "Hemorragia exsanguinante: primeiro aplicar torniquete → VERMELHO."
    }
    return classifyStartNoPulse(p)
}
