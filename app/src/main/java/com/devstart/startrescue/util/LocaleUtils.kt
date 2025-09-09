package com.devstart.startrescue.util

/**
 * Traduz descrições de lesões geradas em PT para EN (e mantém PT quando lang="pt").
 * Se não houver mapeamento, devolve o texto original.
 */
fun localizeLesions(distr: String, lang: String = "pt"): String {
    if (lang.lowercase().startsWith("pt")) return distr

    val mapExact = mapOf(
        "Sem lesões aparentes" to "No visible injuries",
        "Ferimento corto-contuso em mão" to "Laceration/contusion on hand",
        "Ferimento corto-contuso em perna" to "Laceration/contusion on leg",
        "Ferimento corto-contuso em braço" to "Laceration/contusion on arm",
        "Ferimento corto-contuso em antebraço" to "Laceration/contusion on forearm",
        "Fratura exposta em perna" to "Open fracture of the leg",
        "Fratura fechada em perna" to "Closed fracture of the leg",
        "Fratura exposta em braço" to "Open fracture of the arm",
        "Fratura fechada em braço" to "Closed fracture of the arm",
        "Queimadura de 2º grau em antebraço" to "Second-degree burn on forearm",
        "Queimadura de 2º grau em braço" to "Second-degree burn on arm",
        "Ferimento perfurante em tórax" to "Penetrating chest wound",
        "Ferimento perfurante em abdome" to "Penetrating abdominal wound",
        "Trauma cranioencefálico" to "Head injury (TBI)"
    )

    mapExact[distr]?.let { return it }

    // fallback por padrões comuns
    val t = distr.lowercase()
    return when {
        t.contains("sem lesões") -> "No visible injuries"
        t.contains("mão") -> "Injury to hand"
        t.contains("antebraço") -> "Injury to forearm"
        t.contains("braço") -> "Injury to arm"
        t.contains("perna") -> "Injury to leg"
        t.contains("tórax") -> "Chest injury"
        t.contains("abdome") -> "Abdominal injury"
        t.contains("queimadura") -> "Burn injury"
        t.contains("fratura exposta") -> "Open fracture"
        t.contains("fratura fechada") -> "Closed fracture"
        else -> distr // sem mapeamento, mantém original
    }
}
