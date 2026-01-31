package com.example.hardtracking

import android.os.Bundle
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.hardtracking.ui.theme.HardTrackingTheme
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.delay

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
    val events = remember { mutableStateListOf<TimeEvent>() }
    var now by remember { mutableStateOf(Instant.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = Instant.now()
            delay(1_000)
        }
    }

    TimeLogScreen(
        events = events,
        now = now,
        onStartEvent = {
            events.add(TimeEvent(start = Instant.now()))
        },
        onLabelChange = { id, label ->
            val index = events.indexOfFirst { it.id == id }
            if (index != -1) {
                events[index] = events[index].copy(label = label)
            }
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
            onStartEvent = {},
            onLabelChange = { _, _ -> }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeLogScreen(
    events: List<TimeEvent>,
    now: Instant,
    onStartEvent: () -> Unit,
    onLabelChange: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sortedEvents = events.sortedBy { it.start }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(text = "시간 기록") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "지금 하는 행동을 기록하세요",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "입력 시점이 시작 시각이 되며, 이전 기록은 자동 종료됩니다.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onStartEvent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "지금 시작 기록")
                    }
                }
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
                            onLabelChange = onLabelChange
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeEventCard(
    event: TimeEvent,
    end: Instant,
    onLabelChange: (String, String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
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
            OutlinedTextField(
                value = event.label,
                onValueChange = { onLabelChange(event.id, it) },
                label = { Text(text = "행동 이름(사후 입력)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
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

private data class TimeEvent(
    val id: String = UUID.randomUUID().toString(),
    val start: Instant,
    val label: String = ""
)
