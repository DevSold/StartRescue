@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.devstart.startrescue

import com.devstart.startrescue.util.formatTec
import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.devstart.startrescue.model.TriageColor
import com.devstart.startrescue.ui.theme.StartRescueTheme
import com.devstart.startrescue.util.answersToCsv
import com.devstart.startrescue.util.sharePdf
import com.devstart.startrescue.util.timestamp
import com.devstart.startrescue.util.writeResultsPdfTable
import com.devstart.startrescue.util.writeTextToUri

/* Cores fixas de triagem */
private val TriageGreen  = Color(0xFF2E7D32)
private val TriageYellow = Color(0xFFF9A825)
private val TriageRed    = Color(0xFFC62828)
private val TriageBlack  = Color(0xFF000000)

/* Navegação simples */
private enum class Screen { Home, Exam, Result, About }

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var dark by remember { mutableStateOf(true) }
            StartRescueTheme(dark = dark) {
                RootApp(
                    dark = dark,
                    onToggleDark = { dark = !dark }
                )
            }
        }
    }
}

/* --------- ROOT --------- */
@Composable
private fun RootApp(
    dark: Boolean,
    onToggleDark: () -> Unit,
    vm: StartRescueViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var screen by remember { mutableStateOf(Screen.Home) }
    var languageMenu by remember { mutableStateOf(false) }
    var showFluxograma by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.app_title)) },
                actions = {
                    Text(
                        text = if (dark) stringResource(R.string.action_theme_light) else stringResource(R.string.action_theme_dark),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clickable { onToggleDark() }
                    )
                    Box {
                        Text(
                            text = stringResource(R.string.action_choose_language),
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clickable { languageMenu = true }
                        )
                        DropdownMenu(expanded = languageMenu, onDismissRequest = { languageMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.language_portuguese)) },
                                onClick = {
                                    languageMenu = false
                                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("pt"))
                                    activity?.recreate()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.language_english)) },
                                onClick = {
                                    languageMenu = false
                                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
                                    activity?.recreate()
                                }
                            )
                        }
                    }
                    Text(text = stringResource(R.string.action_flowchart), modifier = Modifier
                        .padding(end = 12.dp)
                        .clickable { showFluxograma = true })

                }
            )
        }
    ) { inner ->
        Box(Modifier.padding(inner)) {
            when (screen) {
                Screen.Home -> HomeScreen(
                    vm = vm,
                    onStart = { vm.startExam(vm.nome, vm.setor, vm.ra); screen = Screen.Exam },
                    onStartAbout = { screen = Screen.About }
                )
                Screen.Exam -> ExamScreen(
                    vm = vm,
                    onExit = { screen = Screen.Home },
                    onFinish = { screen = Screen.Result }
                )
                Screen.Result -> ResultScreen(
                    vm = vm,
                    onRestart = { vm.restart(); screen = Screen.Exam },
                    onBackToHome = { screen = Screen.Home }
                )
                Screen.About -> AboutScreen(onBack = { screen = Screen.Home })
            }

        }
    }

    if (showFluxograma) {
        FluxogramaStartDialog(onDismiss = { showFluxograma = false })
    }
}

/* --------- FORM --------- */
@Composable
private fun HomeScreen(vm: StartRescueViewModel, onStart: () -> Unit, onStartAbout: () -> Unit) {
    val scroll = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scroll),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.app_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = vm.nome, onValueChange = { vm.nome = it },
            label = { Text(stringResource(R.string.label_name)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = vm.setor, onValueChange = { vm.setor = it },
            label = { Text(stringResource(R.string.label_sector)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = vm.ra, onValueChange = { vm.ra = it },
            label = { Text(stringResource(R.string.label_ra)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onStart() } ),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(stringResource(R.string.form_start_eval))
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onStartAbout,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(stringResource(R.string.btn_about))
        }


    }
}

/* --------- PROVA --------- */
@Composable
private fun ExamScreen(
    vm: StartRescueViewModel,
    onExit: () -> Unit,
    onFinish: () -> Unit
) {
    val p = vm.currentPatient

    // Limite de "gestante" visível em até 2/20
    val gestantesMeta = 2
    var gestantesMostradas by rememberSaveable { mutableStateOf(0) }
    LaunchedEffect(vm.index) {
        val elegivel = p != null && p.sexo == "Feminino" && p.gestante34
        if (elegivel && gestantesMostradas < gestantesMeta) gestantesMostradas += 1
    }

    // Rodapé fixo com botão Próxima
    Scaffold(
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            if (vm.index >= StartRescueViewModel.TOTAL_QUESTOES) onFinish()
                            else vm.nextQuestion()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = vm.feedbackMode,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            if (vm.index >= StartRescueViewModel.TOTAL_QUESTOES)
                                stringResource(R.string.btn_finish)
                            else
                                stringResource(R.string.btn_next)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Questão ${vm.index}/${StartRescueViewModel.TOTAL_QUESTOES}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                val secs = vm.secondsLeft
                val timeColor = if (secs <= 3) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurfaceVariant
                Text(
                    text = "${secs}s",
                    color = timeColor,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = if (secs <= 3) FontWeight.Bold else FontWeight.Normal
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onExit, shape = RoundedCornerShape(12.dp)) {
                    Text(stringResource(R.string.btn_exit))
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val canShowVA = p != null && !p.respIni && !vm.vaApplied
                if (canShowVA) {
                    Button(
                        onClick = { vm.applyAirway() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = (!p!!.hemExs || vm.tqApplied)
                    ) { Text(stringResource(R.string.btn_apply_airway))
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }

                Button(
                    onClick = { vm.applyTorniquet() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(stringResource(R.string.btn_apply_tq)) }
            }

            if (vm.tqApplied) {
                AssistChip(onClick = {}, label = { Text(stringResource(R.string.pill_tq_applied)) })
            }
            if (vm.vaApplied) {
                AssistChip(onClick = {}, label = {
                    Text(
                        if (p?.respPos == true)
                            stringResource(R.string.pill_va_applied_yes)
                        else
                            stringResource(R.string.pill_va_applied_no)
                    )
                })
            }


            Spacer(Modifier.height(12.dp))

            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.section_case), fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))

                    if (p == null) {
                        Text(stringResource(R.string.loading_patient))
                    } else {
                        val yes = stringResource(R.string.yes)
                        val no  = stringResource(R.string.no)
                        val lang = LocalContext.current.resources.configuration.locales[0].language

                        val sexStr = when (p.sexo.trim().lowercase()) {
                            "male", "masculino", "m" -> stringResource(R.string.sex_male)
                            else -> stringResource(R.string.sex_female)
                        }

                        val lines = buildList {
                            add("${stringResource(R.string.label_age)}: ${p.idade}  |  ${stringResource(R.string.label_sex)}: $sexStr")
                            add("${stringResource(R.string.label_exsang)}: ${if (p.hemExs) yes else no}")
                            add("${stringResource(R.string.label_deambula)}: ${if (p.deambula) yes else no}")
                            add("${stringResource(R.string.label_resp_init)}: ${if (p.respIni) yes else no}")
                            if (!p.respIni && vm.vaApplied) {
                                add("${stringResource(R.string.label_resp_after_va)}: ${if (p.respPos) yes else no}")
                            }
                            add("${stringResource(R.string.label_fr)}: ${maxOf(0, p.fr)}")
                            add("${stringResource(R.string.label_tec)}: ${com.devstart.startrescue.util.formatTec(p.tec, lang)}")
                            add("${stringResource(R.string.label_obeys)}: ${if (p.obedece) yes else no}")
                            if (p.sexo.equals("Feminino", true) && p.gestante34 && gestantesMostradas <= gestantesMeta) {
                                add("${stringResource(R.string.label_preg34)}: $yes")
                            }
                            val lang = LocalContext.current.resources.configuration.locales[0].language
                            add("${stringResource(R.string.label_lesions)}: ${com.devstart.startrescue.util.localizeLesions(p.distr, lang)}")

                        }

                        lines.forEach { Text(it) }
                    }
                }
            }


            Spacer(Modifier.height(12.dp))

            val podeClassificar = p?.let { it.respIni || vm.vaApplied } ?: false

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { vm.pickColor(TriageColor.GREEN) },
                    enabled = podeClassificar,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TriageGreen, contentColor = Color.White)
                ) { Text(stringResource(R.string.btn_green)) }

                Button(
                    onClick = { vm.pickColor(TriageColor.YELLOW) },
                    enabled = podeClassificar,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TriageYellow, contentColor = Color.Black)
                ) { Text(stringResource(R.string.btn_yellow)) }
            }

            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { vm.pickColor(TriageColor.RED) },
                    enabled = podeClassificar,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TriageRed, contentColor = Color.White)
                ) { Text(stringResource(R.string.btn_red)) }

                Button(
                    onClick = { vm.pickColor(TriageColor.BLACK) },
                    enabled = podeClassificar,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TriageBlack, contentColor = Color.White)
                ) { Text(stringResource(R.string.btn_black)) }
            }

            Spacer(Modifier.height(10.dp))
            if (vm.feedbackMode) {
                Text(vm.feedbackText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
            }

            // Espaço extra para não bater no rodapé fixo
            Spacer(Modifier.height(80.dp))
        }
    }
}

/* --------- RESULTADO --------- */
@Composable
fun ResultScreen(
    vm: StartRescueViewModel,
    onRestart: () -> Unit,
    onBackToHome: () -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf(vm.email) }

    // CSV
    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val csv = answersToCsv(vm.respostas, vm.nome, vm.setor, vm.ra, email)
        writeTextToUri(context, uri, csv)
    }

    // PDF (EXPORTAR)
    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult

        val header = "${context.getString(R.string.result_title)} – ${vm.nome} | ${vm.setor} | ${vm.ra} | " +
                context.getString(
                    R.string.result_accuracy,
                    vm.score(),
                    StartRescueViewModel.TOTAL_QUESTOES,
                    vm.accuracy()
                )

        fun sxShort(sexo: String) = when (sexo.trim().lowercase()) {
            "feminino" -> "F"
            "masculino" -> "M"
            else -> if (sexo.startsWith("F", ignoreCase = true)) "F" else "M"
        }
        fun sn(b: Boolean) = if (b) "S" else "N"

        val headerRow = listOf(
            "Q","Id","Sx","Hem","TQ","Deamb","RIni","RPos",
            "FR","TEC","Obed","Gest","Lesão","Sua","Correta","Explicação"
        )
        val colCount = headerRow.size

        val dataRowsBase = vm.respostas.map { r ->
            val p = r.patient
            val rPosStr = if (p.respIni) "—" else if (p.respPos) "S" else "N"
            listOf(
                r.i.toString(),
                p.idade.toString(),
                sxShort(p.sexo),
                sn(p.hemExs),
                sn(r.tqApplied),
                sn(p.deambula),
                sn(p.respIni),
                rPosStr,
                p.fr.toString(),
                formatTec(p.tec, "pt"),
                sn(p.obedece),
                sn(p.gestante34),
                com.devstart.startrescue.util.localizeLesions(p.distr, "pt"),
                r.sua,
                r.correta,
                r.explic
            )
        }

        val dataRows = dataRowsBase.map { row ->
            when {
                row.size == colCount -> row
                row.size >  colCount -> row.take(colCount)
                else -> row + List(colCount - row.size) { "" }
            }
        }

        val linhas = listOf(headerRow) + dataRows
        writeResultsPdfTable(context, uri, header, linhas)
    }

    val acc = vm.accuracy()

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.result_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("${vm.nome}  |  ${vm.setor}  |  ${vm.ra}", textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.result_accuracy, vm.score(), StartRescueViewModel.TOTAL_QUESTOES, acc))

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.result_email)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // ENVIAR POR E-MAIL
        Button(
            onClick = {
                val header = "Resultado START – ${vm.nome} | ${vm.setor} | ${vm.ra}"
                sharePdf(
                    context = context,
                    toEmail = email.ifBlank { null },
                    subject = "Resultado START - ${vm.nome}",
                    body = "Segue em anexo o relatório em PDF.",
                    header = header,
                    respostas = vm.respostas
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) { Text(stringResource(R.string.result_send_pdf_email)) }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { csvLauncher.launch("triagem_${timestamp()}.csv") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) { Text(stringResource(R.string.result_export_csv)) }
            Button(
                onClick = { pdfLauncher.launch("triagem_${timestamp()}.pdf") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) { Text(stringResource(R.string.result_export_pdf)) }
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = onRestart, shape = RoundedCornerShape(16.dp)) { Text(stringResource(R.string.result_restart)) }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBackToHome, shape = RoundedCornerShape(16.dp)) { Text(stringResource(R.string.result_home)) }
    }
}

/* --------- FLUXOGRAMA START (Dialog + Zoom) --------- */
@Composable
private fun FluxogramaStartDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        },
        title = { Text("Fluxograma START") },
        text = {
            FluxogramaZoomableBox()
        }
    )
}

@Composable
private fun FluxogramaZoomableBox() {
    val context = LocalContext.current
    // Busca o id por nome. Se não existir, volta 0.
    val resId = remember {
        context.resources.getIdentifier("fluxograma_start", "drawable", context.packageName)
    }

    // Se não achou, mostra fallback sem quebrar o app
    if (resId == 0) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Imagem do fluxograma não encontrada.\n" +
                        "Adicione res/drawable/fluxograma_start.png (ou .webp) e reconstrua o projeto.",
                textAlign = TextAlign.Center
            )
        }
        return
    }

    // Carrega com o id encontrado
    val painterOrNull = runCatching { painterResource(id = resId) }.getOrNull()
    if (painterOrNull == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) { Text("Falha ao decodificar imagem do fluxograma.") }
        return
    }

    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    val minScale = 1f
    val maxScale = 5f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(minScale, maxScale)
                    val factor = if (scale == 0f) 1f else (newScale / scale)
                    offsetX = (offsetX + pan.x) * factor
                    offsetY = (offsetY + pan.y) * factor
                    scale = newScale
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offsetX
                translationY = offsetY
            }
    ) {
        Image(
            painter = painterOrNull,
            contentDescription = "Fluxograma START",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

