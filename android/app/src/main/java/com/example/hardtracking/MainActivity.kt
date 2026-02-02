package com.example.hardtracking

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.hardtracking.ui.theme.HardTrackingTheme
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
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
        },
        onClearEvents = {
            TimeEventRepository.clearAll(context)
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
            onLabelChange = { _, _ -> },
            onClearEvents = {}
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
    onClearEvents: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var canDrawOverlay by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var overlayEnabled by remember { mutableStateOf(OverlaySettings.isEnabled(context)) }
    val sortedEvents = events.sortedBy { it.start }
    val zoneId = ZoneId.systemDefault()
    val today = now.atZone(zoneId).toLocalDate()
    var selectedDate by remember { mutableStateOf(today) }
    var showCalendarDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showStartDialog by remember { mutableStateOf(false) }
    var startLabel by remember { mutableStateOf("") }
    var editingEvent by remember { mutableStateOf<TimeEvent?>(null) }
    var editLabel by remember { mutableStateOf("") }
    var showOverlaySettings by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var overlaySize by remember { mutableStateOf(OverlaySettings.loadSizeDp(context)) }
    var overlayAlpha by remember { mutableStateOf(OverlaySettings.loadAlpha(context)) }

    val endMap = sortedEvents.mapIndexed { index, event ->
        val end = sortedEvents.getOrNull(index + 1)?.start ?: now
        event.id to end
    }.toMap()
    val filteredEvents = sortedEvents.filter { event ->
        event.start.atZone(zoneId).toLocalDate() == selectedDate
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canDrawOverlay = Settings.canDrawOverlays(context)
                overlayEnabled = OverlaySettings.isEnabled(context)
                overlaySize = OverlaySettings.loadSizeDp(context)
                overlayAlpha = OverlaySettings.loadAlpha(context)
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { showCalendarDialog = true }) {
                    Text(
                        text = formatDateTime(now, zoneId),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                IconButton(
                    onClick = { showOverlaySettings = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "잠금화면 플로팅 버튼 설정",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            Button(
                onClick = { showStartDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "기록하기")
            }

            Text(
                text = if (selectedDate == today) {
                    "오늘의 기록"
                } else {
                    "${formatDate(selectedDate)} 기록"
                },
                style = MaterialTheme.typography.titleMedium
            )

            if (filteredEvents.isEmpty()) {
                Text(
                    text = "아직 기록이 없습니다.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(filteredEvents, key = { _, item -> item.id }) { _, event ->
                        val end = endMap[event.id] ?: now
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

    if (showOverlaySettings) {
        AlertDialog(
            onDismissRequest = { showOverlaySettings = false },
            title = { Text(text = "잠금화면 플로팅 버튼 설정") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!canDrawOverlay) {
                        Text(text = "권한이 필요합니다.")
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
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "잠금화면 버튼 사용")
                            Switch(
                                checked = overlayEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        context.startService(OverlayService.intent(context))
                                    } else {
                                        context.stopService(OverlayService.intent(context))
                                    }
                                    OverlaySettings.setEnabled(context, enabled)
                                    overlayEnabled = enabled
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(text = "미리보기")
                            Box(
                                modifier = Modifier
                                    .size(overlaySize.dp)
                                    .clip(CircleShape)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(
                                            alpha = overlayAlpha
                                        )
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "크기: ${overlaySize.toInt()}dp")
                        Slider(
                            value = overlaySize,
                            onValueChange = { overlaySize = it },
                            valueRange = 36f..72f,
                            steps = 6
                        )
                        Text(text = "투명도: ${(overlayAlpha * 100).toInt()}%")
                        Slider(
                            value = overlayAlpha,
                            onValueChange = { overlayAlpha = it },
                            valueRange = 0.4f..1.0f
                        )
                        TextButton(
                            onClick = {
                                OverlaySettings.resetPosition(context)
                                if (overlayEnabled) {
                                    context.stopService(OverlayService.intent(context))
                                    context.startService(OverlayService.intent(context))
                                }
                            }
                        ) {
                            Text(text = "위치 초기화")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { showClearConfirm = true }) {
                            Text(text = "기록 초기화")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        OverlaySettings.saveSizeDp(context, overlaySize)
                        OverlaySettings.saveAlpha(context, overlayAlpha)
                        if (overlayEnabled) {
                            context.stopService(OverlayService.intent(context))
                            context.startService(OverlayService.intent(context))
                        }
                        showOverlaySettings = false
                    }
                ) {
                    Text(text = "저장")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverlaySettings = false }) {
                    Text(text = "닫기")
                }
            }
        )
    }

    if (showCalendarDialog) {
        val initialMillis = selectedDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis
        )
        DatePickerDialog(
            onDismissRequest = { showCalendarDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()
                        }
                        showCalendarDialog = false
                    }
                ) {
                    Text(text = "확인")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCalendarDialog = false }) {
                    Text(text = "취소")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(text = "기록 초기화") },
            text = { Text(text = "모든 기록을 삭제합니다. 계속할까요?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearEvents()
                        showClearConfirm = false
                    }
                ) {
                    Text(text = "삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
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
                text = "${formatTimestamp(event.start)}-${formatTimestamp(end)} (${formatDurationMinutes(event.start, end)})",
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

private fun formatDate(date: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("MM월 dd일")
    return formatter.format(date)
}

private fun formatDateTime(timestamp: Instant, zoneId: ZoneId): String {
    val formatter = DateTimeFormatter.ofPattern("MM월 dd일 HH:mm")
    return formatter.format(timestamp.atZone(zoneId))
}

private fun formatDurationMinutes(start: Instant, end: Instant): String {
    val rawDuration = Duration.between(start, end)
    val duration = if (rawDuration.isNegative) Duration.ZERO else rawDuration
    val minutes = duration.toMinutes()
    return "${minutes}분"
}
