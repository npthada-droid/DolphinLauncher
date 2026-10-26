package th.dolphin.launcher

import android.app.Application
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import th.dolphin.launcher.apps.AppRepository
import th.dolphin.launcher.apps.ShortcutStore
import kotlinx.coroutines.launch
import th.dolphin.launcher.laps.LapStore
import th.dolphin.launcher.trip.TripRecorder
import th.dolphin.launcher.location.LocationRepository
import th.dolphin.launcher.location.WeatherRepository
import th.dolphin.launcher.laps.Stopwatch
import th.dolphin.launcher.media.BluetoothRepository
import th.dolphin.launcher.media.MediaRepository
import th.dolphin.launcher.settings.SettingsStore
import th.dolphin.launcher.vehicle.HiddenApi
import th.dolphin.launcher.vehicle.VehicleRepository

class App : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    lateinit var settings: SettingsStore
        private set
    lateinit var vehicle: VehicleRepository
        private set
    lateinit var location: LocationRepository
        private set
    lateinit var weather: WeatherRepository
        private set
    lateinit var apps: AppRepository
        private set
    lateinit var shortcuts: ShortcutStore
        private set
    lateinit var laps: LapStore
        private set
    lateinit var trip: TripRecorder
        private set
    lateinit var widgets: th.dolphin.launcher.ui.widgets.WidgetStore
        private set
    lateinit var media: MediaRepository
        private set
    lateinit var bluetooth: BluetoothRepository
        private set

    /** นาฬิกาจับเวลารอบ — อยู่ระดับแอปเพื่อไม่หยุดเมื่อสลับหน้า */
    val stopwatch = Stopwatch()

    /** true เมื่อหน้า Launcher อยู่บนจอ (ใช้ตัดสินว่าจะแสดงแจ้งเตือนแบบลอยหรือไม่) */
    val foreground = MutableStateFlow(false)

    override fun onCreate() {
        super.onCreate()
        SafetyGuard.init(this)
        CrashGuard.install(this)
        HiddenApi.exempt()
        settings = SettingsStore(this)
        location = LocationRepository(this)
        vehicle = VehicleRepository(this, settings, scope, location)
        weather = WeatherRepository(location, scope) { settings.value.weatherEnabled }
        apps = AppRepository(this, scope)
        shortcuts = ShortcutStore(this)
        laps = LapStore(this, scope)
        // ค่าระบบป้องกันจากตั้งค่า
        scope.launch {
            settings.flow.collect {
                HangWatchdog.timeoutSec = it.watchdogSec
                th.dolphin.launcher.i18n.I18n.lang = th.dolphin.launcher.i18n.I18n.resolve(it.language)
                SafetyGuard.strict = it.guardStrict
            }
        }
        HangWatchdog.start(this)
        trip = TripRecorder(this, settings, scope)
        scope.launch { vehicle.state.collect { trip.onState(it) } }
        widgets = th.dolphin.launcher.ui.widgets.WidgetStore(this)
        media = MediaRepository(this)
        bluetooth = BluetoothRepository(this, scope)
    }
}

val Context.app: App get() = applicationContext as App
