package th.dolphin.launcher.laps

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class LapSession(
    val id: Long,
    val type: String,          // lap | accel
    val createdAt: Long,
    val laps: List<Long>,
    val maxSpeed: Float,
    val title: String,
) {
    val best: Long? get() = laps.minOrNull()

    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("type", type).put("createdAt", createdAt)
        .put("laps", JSONArray(laps)).put("maxSpeed", maxSpeed.toDouble()).put("title", title)

    companion object {
        fun fromJson(o: JSONObject) = LapSession(
            id = o.getLong("id"),
            type = o.optString("type", "lap"),
            createdAt = o.optLong("createdAt"),
            laps = o.optJSONArray("laps")?.let { a -> List(a.length()) { a.getLong(it) } } ?: emptyList(),
            maxSpeed = o.optDouble("maxSpeed", 0.0).toFloat(),
            title = o.optString("title"),
        )
    }
}

class LapStore(context: Context, private val scope: CoroutineScope) {
    private val file = File(context.filesDir, "laps.json")
    private val _sessions = MutableStateFlow(load())
    val sessions: StateFlow<List<LapSession>> = _sessions.asStateFlow()

    private fun load(): List<LapSession> = runCatching {
        val a = JSONArray(file.readText())
        List(a.length()) { LapSession.fromJson(a.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    fun add(s: LapSession) {
        _sessions.value = listOf(s) + _sessions.value
        persist()
    }

    fun delete(id: Long) {
        _sessions.value = _sessions.value.filterNot { it.id == id }
        persist()
    }

    private fun persist() {
        val snapshot = _sessions.value
        scope.launch(Dispatchers.IO) {
            runCatching { file.writeText(JSONArray(snapshot.map { it.toJson() }).toString()) }
        }
    }
}

/** นาฬิกาจับเวลารอบ (อิง SystemClock.elapsedRealtime) */
class Stopwatch {
    var running by mutableStateOf(false)
        private set
    private var startAt by mutableLongStateOf(0L)
    var accumulated by mutableLongStateOf(0L)
        private set
    private var lapStartTotal by mutableLongStateOf(0L)
    val laps = mutableStateListOf<Long>()
    var maxSpeed by mutableFloatStateOf(0f)

    fun total(now: Long): Long = accumulated + if (running) now - startAt else 0L
    fun currentLap(now: Long): Long = total(now) - lapStartTotal

    fun start(now: Long) {
        if (!running) { startAt = now; running = true }
    }

    fun pause(now: Long) {
        if (running) { accumulated += now - startAt; running = false }
    }

    fun lap(now: Long) {
        val t = total(now)
        laps.add(t - lapStartTotal)
        lapStartTotal = t
    }

    fun reset() {
        running = false; accumulated = 0L; lapStartTotal = 0L; laps.clear(); maxSpeed = 0f
    }
}
