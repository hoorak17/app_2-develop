package com.example.hardtracking

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.hardtracking.ui.theme.HardTrackingTheme
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HardTrackingTheme {
                TimeLogApp()
            }
        }
    }
}

@Composable
fun TimeLogApp() {
    val context = LocalContext.current
    var now by remember { mutableStateOf(Instant.now()) }
    val events by TimeEventRepository.events.collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        TimeEventRepository.init(context)
    }

    LaunchedEffect(Unit) {
        while (true) {
            now = Instant.now()
            delay(1_000)
        }
    }

    TimeLogScreen(
        events = events,
        now = now,
        onStartEvent = { label ->
            TimeEventRepository.addEvent(context, label)
        },
        onLabelChange = { id, label ->
            TimeEventRepository.updateLabel(context, id, label)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun TimeLogPreview() {
    HardTrackingTheme {
        val previewEvents = listOf(
            TimeEvent(id = "1", start = Instant.now().minusSeconds(3_600), label = "집중 작업"),
            TimeEvent(id = "2", start = Instant.now().minusSeconds(1_800), label = ""),
            TimeEvent(id = "3", start = Instant.now().minusSeconds(600), label = "휴식")
        )
        TimeLogScreen(
            events = previewEvents,
            now = Instant.now(),
            onStartEvent = { _ -> },
            onLabelChange = { _, _ -> }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeLogScreen(
    events: List<TimeEvent>,
    now: Instant,
    onStartEvent: (String) -> Unit,
    onLabelChange: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var canDrawOverlay by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var overlayEnabled by remember { mutableStateOf(OverlaySettings.isEnabled(context)) }
    val sortedEvents = events.sortedBy { it.start }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showStartDialog by remember { mutableStateOf(false) }
    var startLabel by remember { mutableStateOf("") }
    var editingEvent by remember { mutableStateOf<TimeEvent?>(null) }
    var editLabel by remember { mutableStateOf("") }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canDrawOverlay = Settings.canDrawOverlays(context)
                overlayEnabled = OverlaySettings.isEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(text = "시간 기록") })
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "지금 하는 행동을 기록하세요",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "입력 시점이 시작 시각이 되며, 이전 기록은 자동 종료됩니다.",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = { showStartDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "기록 시작")
            }

            Text(
                text = "오늘의 기록",
                style = MaterialTheme.typography.titleMedium
            )

            if (sortedEvents.isEmpty()) {
                Text(
                    text = "아직 기록이 없습니다.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(sortedEvents, key = { _, item -> item.id }) { index, event ->
                        val end = sortedEvents.getOrNull(index + 1)?.start ?: now
                        TimeEventCard(
                            event = event,
                            end = end,
                            onEdit = {
                                editingEvent = event
                                editLabel = event.label
                            }
                        )
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "잠금화면 플로팅 버튼",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    when {
                        !canDrawOverlay -> {
                            Button(
                                onClick = {
                                    context.startActivity(
                                        OverlaySettings.intentForPermission(context)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = "권한 허용")
                            }
                        }
                        overlayEnabled -> {
                            Button(
                                onClick = {
                                    context.stopService(
                                        OverlayService.intent(context)
                                    )
                                    OverlaySettings.setEnabled(context, false)
                                    overlayEnabled = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = "잠금화면 버튼 끄기")
                            }
                        }
                        else -> {
                            Button(
                                onClick = {
                                    context.startService(OverlayService.intent(context))
                                    OverlaySettings.setEnabled(context, true)
                                    overlayEnabled = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = "잠금화면 버튼 켜기")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showStartDialog) {
        AlertDialog(
            onDismissRequest = { showStartDialog = false },
            title = { Text(text = "기록 시작") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "지금 시작할 행동 이름을 입력하세요.")
                    OutlinedTextField(
                        value = startLabel,
                        onValueChange = { startLabel = it },
                        label = { Text(text = "행동 이름") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = startLabel.trim()
                        if (trimmed.isNotEmpty()) {
                            onStartEvent(trimmed)
                            showStartDialog = false
                            startLabel = ""
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("기록을 시작했습니다")
                            }
                        }
                    },
                    enabled = startLabel.isNotBlank()
                ) {
                    Text(text = "시작")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDialog = false }) {
                    Text(text = "취소")
                }
            }
        )
    }

    if (editingEvent != null) {
        AlertDialog(
            onDismissRequest = { editingEvent = null },
            title = { Text(text = "행동 이름 수정") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "잠금화면에서 시작한 기록이라면 여기서 이름을 입력하세요.")
                    OutlinedTextField(
                        value = editLabel,
                        onValueChange = { editLabel = it },
                        label = { Text(text = "행동 이름") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val event = editingEvent
                        if (event != null) {
                            onLabelChange(event.id, editLabel.trim())
                        }
                        editingEvent = null
                    }
                ) {
                    Text(text = "저장")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingEvent = null }) {
                    Text(text = "취소")
                }
            }
        )
    }
}

@Composable
private fun TimeEventCard(
    event: TimeEvent,
    end: Instant,
    onEdit: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onEdit
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTimestamp(event.start),
                    style = MaterialTheme.typography.titleSmall
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "→", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatTimestamp(end),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "지속 시간: ${formatDuration(event.start, end)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (event.label.isBlank()) "행동 이름 없음" else event.label,
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (event.label.isBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "잠금화면에서 시작한 기록입니다. 이름을 입력하세요.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                TextButton(onClick = onEdit) {
                    Text(text = "편집")
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Instant): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    return formatter.format(timestamp.atZone(ZoneId.systemDefault()))
}

private fun formatDuration(start: Instant, end: Instant): String {
    val rawDuration = Duration.between(start, end)
    val duration = if (rawDuration.isNegative) Duration.ZERO else rawDuration
    val hours = duration.toHours()
    val minutes = duration.minusHours(hours).toMinutes()
    return if (hours > 0) {
        "${hours}시간 ${minutes}분"
    } else {
        "${minutes}분"
    }
}
