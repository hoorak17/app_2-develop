package com.example.hardtracking

import android.content.Context
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

object TimeEventRepository {
    private const val PREFS_NAME = "time_event_repository"
    private const val KEY_EVENTS = "events"
    private val eventsFlow = MutableStateFlow<List<TimeEvent>>(emptyList())
    private var initialized = false

    val events: StateFlow<List<TimeEvent>> = eventsFlow

    fun init(context: Context) {
        if (initialized) return
        eventsFlow.value = load(context)
        initialized = true
    }

    fun addEvent(context: Context, label: String = "") {
        val updated = eventsFlow.value + TimeEvent(start = Instant.now(), label = label)
        eventsFlow.value = updated
        save(context, updated)
    }

    fun updateLabel(context: Context, id: String, label: String) {
        val updated = eventsFlow.value.map { event ->
            if (event.id == id) {
                event.copy(label = label)
            } else {
                event
            }
        }
        eventsFlow.value = updated
        save(context, updated)
    }

    private fun load(context: Context): List<TimeEvent> {
        val raw = prefs(context).getString(KEY_EVENTS, "[]") ?: "[]"
        val array = JSONArray(raw)
        val result = mutableListOf<TimeEvent>()
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            val id = item.optString("id")
            val start = Instant.ofEpochMilli(item.optLong("start"))
            val label = item.optString("label")
            if (id.isNotBlank()) {
                result.add(TimeEvent(id = id, start = start, label = label))
            }
        }
        return result
    }

    private fun save(context: Context, events: List<TimeEvent>) {
        val array = JSONArray()
        events.forEach { event ->
            val obj = JSONObject()
            obj.put("id", event.id)
            obj.put("start", event.start.toEpochMilli())
            obj.put("label", event.label)
            array.put(obj)
        }
        prefs(context).edit().putString(KEY_EVENTS, array.toString()).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
