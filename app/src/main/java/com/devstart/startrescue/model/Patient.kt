package com.devstart.startrescue.model

data class Patient(
    val id: Int,
    val idade: Int,            // anos
    val sexo: String,          // "Masculino" | "Feminino"
    val deambula: Boolean,     // deambula?
    val respIni: Boolean,      // respiração inicial (antes de VA)
    val hemExs: Boolean,       // hemorragia exsanguinante
    val fr: Int,               // frequência respiratória
    val tec: Double,           // tempo de enchimento capilar (s)
    val obedece: Boolean,      // obedece comandos?
    val gestante34: Boolean,   // gestante ≥34 semanas
    val distr: String,         // lesões / distribuição
    val respPos: Boolean       // respiração após liberação de vias aéreas (VA)
)
