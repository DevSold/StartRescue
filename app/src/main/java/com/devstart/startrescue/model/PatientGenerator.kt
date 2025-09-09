package com.devstart.startrescue.model

import kotlin.math.round
import kotlin.random.Random

fun generatePatient(
    gestantesJa: Int,
    gestantesMeta: Int,
    hemFaltam: Int,
    totalRestante: Int
): Patient {

    val id    = Random.nextInt(100_000, 999_999)
    val idade = Random.nextInt(1, 91)                           // 1..90
    val sexo  = if (Random.nextBoolean()) "Masculino" else "Feminino"

    // ----- Sorteio base -----
    var respIni  = Random.nextDouble() > 0.15                   // ~85% respiram inicialmente
    var fr       = if (respIni) Random.nextInt(8, 41) else 0
    var obedece  = Random.nextDouble() < 0.70
    var deambula = Random.nextDouble() < 0.60
    var hemExs   = Random.nextDouble() < 0.12
    var tec = run {
        val bruto = Random.nextDouble(0.5, 4.0)                 // 0.5..4.0 s
        round(bruto * 10.0) / 10.0
    }

    // ----- Regras de coerência START -----

    // 1) Não respira => FR=0, NÃO obedece, NÃO deambula, TEC alto (≥3s)
    if (!respIni) {
        fr = 0
        obedece = false
        deambula = false
        tec = run {
            val bruto = Random.nextDouble(3.0, 4.0)
            round(bruto * 10.0) / 10.0
        }
    }

    // 2) Deambula => respira, obedece, FR “ok”, TEC <= 2, sem hemorragia exsanguinante
    if (deambula) {
        respIni = true
        obedece = true
        fr = Random.nextInt(12, 31)
        tec = run {
            val bruto = Random.nextDouble(0.6, 1.9)
            round(bruto * 10.0) / 10.0
        }
        hemExs = false
    }

    // 3) Hemorragia exsanguinante geralmente impede deambulação
    if (hemExs) deambula = false

    // ----- Gestante (>=34 semanas) -----
    val dentroDaFaixa = (sexo == "Feminino") && idade in 12..55
    val dentroDaMeta  = (gestantesMeta == 0) || (gestantesJa < gestantesMeta)
    val gestante34 = if (dentroDaFaixa && dentroDaMeta) Random.nextDouble() < 0.15 else false

    // Lesões
    val poolLesoes = listOf(
        "Escoriações múltiplas",
        "Laceração superficial em antebraço",
        "Fratura fechada de rádio",
        "Contusão torácica leve",
        "Edema em tornozelo",
        "Ferimento corto-contuso em mão"
    )
    val distr = if (Random.nextDouble() < 0.40) "Sem lesões aparentes"
    else poolLesoes.shuffled().take(Random.nextInt(1, 3)).joinToString(", ")

    val respPos = false // será definido após “Liberar VA”

    return Patient(
        id = id,
        idade = idade,
        sexo = sexo,
        deambula = deambula,
        respIni = respIni,
        hemExs = hemExs,
        fr = fr,
        tec = tec,
        obedece = obedece,
        gestante34 = gestante34,
        distr = distr,
        respPos = respPos
    )
}
