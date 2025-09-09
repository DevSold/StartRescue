package com.devstart.startrescue.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devstart.startrescue.model.Patient
import com.devstart.startrescue.util.formatTec

@Composable
fun CasoCard(p: Patient, lang: String = "pt", modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Caso", style = MaterialTheme.typography.titleMedium)
            Text("Idade: ${p.idade} | Sexo: ${p.sexo}")
            Text("Hemorragia exsanguinante: ${if (p.hemExs) "SIM" else "NÃO"}")
            Text("Deambula: ${if (p.deambula) "SIM" else "NÃO"}")
            Text("Respira (inicial): ${if (p.respIni) "SIM" else "NÃO"}")
            Text("Respira após abrir VA: ${if (p.respPos) "SIM" else "NÃO"}")
            Text("FR: ${p.fr}")
            Text("TEC: ${formatTec(p.tec, lang)}") // 👈 Infinity vira "Ausente"
            Text("Obedece comandos: ${if (p.obedece) "SIM" else "NÃO"}")
            Text("Lesões observadas: ${p.distr}")
        }
    }
}
