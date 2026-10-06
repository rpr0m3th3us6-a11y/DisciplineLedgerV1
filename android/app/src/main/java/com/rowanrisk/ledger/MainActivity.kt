package com.rowanrisk.ledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {

    // Bumped on every resume so the screen reloads after a widget tap changed the data.
    private var resumeTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LedgerTheme { LedgerScreen(resumeTick) }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeTick++
    }
}

@Composable
private fun LedgerScreen(resumeTick: Int) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var data by remember(resumeTick) { mutableStateOf(Store.load(ctx)) }
    var newHabit by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var statusIsError by remember { mutableStateOf(false) }
    var pushIndex by remember { mutableIntStateOf(Content.indexFor()) }

    fun refresh(newData: LedgerData) {
        data = newData
        scope.launch { LedgerWidget().updateAll(ctx) }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            runCatching {
                val out = ctx.contentResolver.openOutputStream(uri, "wt") ?: error("cannot open the file")
                out.use { it.write(Store.exportJson(Store.load(ctx)).toByteArray(Charsets.UTF_8)) }
            }.onSuccess {
                status = "Exported."
                statusIsError = false
            }.onFailure {
                status = "Export failed: ${it.message}"
                statusIsError = true
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                val input = ctx.contentResolver.openInputStream(uri) ?: error("cannot open the file")
                val text = input.use { it.readBytes().toString(Charsets.UTF_8) }
                Store.importJson(ctx, text)
            }.onSuccess {
                refresh(it)
                status = "Imported ${it.habits.size} habits and ${it.log.size} logged days."
                statusIsError = false
            }.onFailure {
                status = "Import failed: ${it.message}. Nothing was changed."
                statusIsError = true
            }
        }
    }

    val today = LocalDate.now()
    val doneCount = Logic.doneCount(data, today)
    val total = data.habits.size
    val streak = Logic.overallStreak(data, today)
    val best = maxOf(data.bestStreak, streak)
    val mono = FontFamily.Monospace

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        LazyColumn(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 640.dp)
                .fillMaxHeight()
                .systemBarsPadding()
                .imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "DISCIPLINE LEDGER",
                        fontFamily = mono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "Rowan Risk Solutions // daily ops log",
                        fontFamily = mono,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = today.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                            fontFamily = mono,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = if (streak > 0) "$streak-day streak" else "no streak yet",
                            fontFamily = mono,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("TODAY", "$doneCount/$total", Modifier.weight(1f))
                    StatCard("STREAK", "$streak", Modifier.weight(1f))
                    StatCard("BEST", "$best", Modifier.weight(1f))
                }
            }

            item {
                SectionCard(title = "DAILY DISCIPLINE", hint = "${if (total > 0) doneCount * 100 / total else 0}%") {
                    if (data.habits.isEmpty()) {
                        Text(
                            text = "No habits yet. Add one below.",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                        )
                    }
                    data.habits.forEach { habit ->
                        val done = Logic.isDone(data, habit.id, today)
                        val hs = Logic.habitStreak(data, habit.id, today)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = done,
                                onCheckedChange = { refresh(Store.toggle(ctx, habit.id)) },
                            )
                            Text(
                                text = habit.name,
                                modifier = Modifier.weight(1f),
                                fontSize = 15.sp,
                                color = if (done) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                            if (hs > 0) {
                                Text(
                                    text = "${hs}d",
                                    fontFamily = mono,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                            TextButton(onClick = { refresh(Store.removeHabit(ctx, habit.id)) }) {
                                Text("×", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = newHabit,
                            onValueChange = { newHabit = it.take(60) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = { Text("Add a habit") },
                        )
                        Button(
                            onClick = {
                                if (newHabit.isNotBlank()) {
                                    refresh(Store.addHabit(ctx, newHabit))
                                    newHabit = ""
                                }
                            },
                        ) { Text("Add") }
                    }
                }
            }

            item {
                val push = Content.items[pushIndex % Content.items.size]
                SectionCard(title = "PUSH", hint = "CBT-based") {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "“${push.quote}”",
                            fontSize = 18.sp,
                            lineHeight = 25.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "— ${push.author}",
                            fontFamily = mono,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Try: ${push.tool}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(onClick = { pushIndex = (pushIndex + 1) % Content.items.size }) {
                            Text("Next one")
                        }
                    }
                }
            }

            item {
                SectionCard(title = "LAST 7 DAYS", hint = "") {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        for (i in 0..6) {
                            val day = today.minusDays(i.toLong())
                            val done = Logic.doneCount(data, day)
                            val pct = if (total > 0) done.toFloat() / total else 0f
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (i == 0) "Today" else day.format(DateTimeFormatter.ofPattern("EEE")),
                                    modifier = Modifier.padding(end = 10.dp).widthIn(min = 44.dp),
                                    fontFamily = mono,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                LinearProgressIndicator(
                                    progress = { pct },
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = "${(pct * 100).toInt()}%",
                                    modifier = Modifier.padding(start = 10.dp).widthIn(min = 36.dp),
                                    fontFamily = mono,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            item {
                SectionCard(title = "BACKUP", hint = "") {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "Data lives on this phone only. Export a copy now and then. " +
                                "Import also accepts the export from the web app.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { exportLauncher.launch("discipline-ledger-$today.json") }) {
                                Text("Export JSON")
                            }
                            OutlinedButton(
                                onClick = {
                                    importLauncher.launch(
                                        arrayOf("application/json", "text/plain", "application/octet-stream"),
                                    )
                                },
                            ) { Text("Import JSON") }
                        }
                        if (status.isNotEmpty()) {
                            Text(
                                text = status,
                                fontFamily = mono,
                                fontSize = 12.sp,
                                color = if (statusIsError) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                fontFamily = FontFamily.Monospace,
                fontSize = 26.sp,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SectionCard(title: String, hint: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (hint.isNotEmpty()) {
                    Text(
                        text = hint,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            content()
        }
    }
}
