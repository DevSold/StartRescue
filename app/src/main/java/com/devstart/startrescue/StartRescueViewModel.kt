package com.devstart.startrescue

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devstart.startrescue.model.Patient
import com.devstart.startrescue.model.TriageColor
import com.devstart.startrescue.model.generatePatient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.random.Random

data class AnswerRecord(
    val i: Int,
    val patient: Patient,
    val tqApplied: Boolean,
    val sua: String,
    val correta: String,
    val acertou: Boolean,
    val explic: String
)

class StartRescueViewModel : ViewModel() {

    companion object {
        const val TOTAL_QUESTOES = 20
        const val QUESTION_TIME = 10
        const val TEC_AUSENTE_DESC = "TEC ausente"
        val TEC_AUSENTE_VALUE: Double = Double.POSITIVE_INFINITY
    }

    var nome by mutableStateOf("")
    var setor by mutableStateOf("")
    var ra by mutableStateOf("")
    var email by mutableStateOf("")

    var vaApplied by mutableStateOf(false)         // VA aplicada nesta questão
        private set

    var index by mutableStateOf(1)
        private set
    var secondsLeft by mutableStateOf(QUESTION_TIME)
        private set
    var finished by mutableStateOf(false)
        private set

    var tqApplied by mutableStateOf(false)
        private set
    var feedbackMode by mutableStateOf(false)
        private set
    var feedbackText by mutableStateOf("")
        private set

    var currentPatient by mutableStateOf<Patient?>(null)
        private set

    val respostas = mutableListOf<AnswerRecord>()

    // quotas
    private var gestantesJa = 0
    private var gestantesMeta = Random.nextInt(2, 4) // 2..3
    private var hemCasos = 0
    private var hemAlvo = 3

    private var timerJob: Job? = null

    fun startExam(nome: String, setor: String, ra: String) {
        this.nome = nome.trim(); this.setor = setor.trim(); this.ra = ra.trim(); this.email = ""
        respostas.clear(); index = 1; finished = false
        tqApplied = false
        vaApplied = false
        feedbackMode = false; feedbackText = ""
        gestantesJa = 0; gestantesMeta = Random.nextInt(2, 4)
        hemCasos = 0; hemAlvo = 3
        generateAndSetPatient()
        startTimer()
    }

    fun restart() { startExam(nome, setor, ra) }

    private fun generateAndSetPatient() {
        val totalRestante = TOTAL_QUESTOES - (index - 1)
        val hemFaltam = max(0, hemAlvo - hemCasos)
        val gen = generatePatient(gestantesJa, gestantesMeta, hemFaltam, totalRestante)

        currentPatient = if (!gen.respIni) {
            gen.copy(tec = TEC_AUSENTE_VALUE) // 👈 TEC ausente quando não respira inicialmente
        } else {
            gen
        }
    }


    private fun startTimer() {
        timerJob?.cancel()
        secondsLeft = QUESTION_TIME
        timerJob = viewModelScope.launch {
            while (!finished && !feedbackMode && secondsLeft > 0) {
                delay(1000); secondsLeft--
                if (secondsLeft == 0) timeout()
            }
        }
    }

    fun applyTorniquet() { tqApplied = true }

    fun applyAirway() {
        val p = currentPatient ?: return
        if (vaApplied || p.respIni) return

        val passaARespirar = kotlin.random.Random.nextDouble() < 0.5
        val novaFr = if (passaARespirar) kotlin.random.Random.nextInt(10, 31) else 0

        // Se respirar, 60% normal (1.0–2.0s), 40% lento (2.1–4.0s)
        val novoTec = if (passaARespirar) {
            if (kotlin.random.Random.nextDouble() < 0.6)
                kotlin.random.Random.nextDouble(1.0, 2.0)
            else
                kotlin.random.Random.nextDouble(2.1, 4.0)
        } else {
            TEC_AUSENTE_VALUE // continua sem respiração → ausente
        }

        currentPatient = p.copy(
            respPos = passaARespirar,
            fr = novaFr,
            tec = novoTec
        )

        vaApplied = true
    }




    fun pickColor(color: TriageColor) {
        if (finished || feedbackMode) return
        val p = currentPatient ?: return
        val (correct, exp) = correctClassification(p, tqApplied)
        val ok = (color == correct)
        respostas.add(
            AnswerRecord(index, p, tqApplied, labelColor(color), labelColor(correct), ok,
                if (ok) "Correto!" else exp)
        )
        if (p.hemExs) hemCasos++
        if (p.gestante34) gestantesJa++
        feedbackMode = true
        feedbackText = if (ok) "Correto!" else exp
        timerJob?.cancel()
    }

    private fun timeout() {
        if (finished || feedbackMode) return
        val p = currentPatient ?: return
        val (correct, exp) = correctClassification(p, tqApplied)
        respostas.add(
            AnswerRecord(index, p, tqApplied, "—", labelColor(correct), false,
                "Tempo esgotado. Resposta correta: ${labelColor(correct)}. $exp")
        )
        if (p.hemExs) hemCasos++
        if (p.gestante34) gestantesJa++
        feedbackMode = true
        feedbackText = "Tempo esgotado. Resposta correta: ${labelColor(correct)}. $exp"
        timerJob?.cancel()
    }

    fun nextQuestion() {
        if (finished) return
        if (index >= TOTAL_QUESTOES) { finished = true; timerJob?.cancel(); return }
        index++
        tqApplied = false
        vaApplied = false
        feedbackMode = false
        feedbackText = ""
        generateAndSetPatient()
        startTimer()
    }

    private fun classifyStartNoPulse(p: Patient): Pair<TriageColor, String> {
        if (p.deambula) return TriageColor.GREEN to "Deambula: SIM → VERDE."
        if (!p.respIni) return if (!p.respPos)
            TriageColor.BLACK to "Não respira após abrir vias aéreas → ÓBITO (${TEC_AUSENTE_DESC})."
        else TriageColor.RED to "Após abrir vias aéreas passou a respirar → VERMELHO."

        if (p.fr > 30) return TriageColor.RED to "FR=${p.fr} (>30) → VERMELHO."
        if (p.tec > 2.0) return TriageColor.RED to "TEC>2s → VERMELHO."
        if (!p.obedece) return TriageColor.RED to "Não obedece comandos → VERMELHO."
        return TriageColor.YELLOW to "Parâmetros adequados (não deambula) → AMARELO."
    }


    fun correctClassification(p: Patient, tqApplied: Boolean): Pair<TriageColor, String> {
        if (p.hemExs && !tqApplied) return TriageColor.RED to "Hemorragia exsanguinante: primeiro aplicar torniquete → VERMELHO."
        return classifyStartNoPulse(p)
    }

    fun score(): Int = respostas.count { it.acertou }
    fun accuracy(): Int = if (respostas.isEmpty()) 0 else score() * 100 / respostas.size
    fun setFeedback(s: String) {

    }
}

fun labelColor(c: TriageColor): String = when (c) {
    TriageColor.GREEN -> "VERDE"
    TriageColor.YELLOW -> "AMARELO"
    TriageColor.RED -> "VERMELHO"
    TriageColor.BLACK -> "ÓBITO"
}
