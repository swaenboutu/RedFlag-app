package app.redflag.service

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import app.redflag.R
import app.redflag.container
import app.redflag.ui.home.HomeActivity
import app.redflag.ui.interruption.InterstitialActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Watches which app comes to the foreground by reading Android's usage events ("Usage access") every [POLL_MILLIS], and opens the
 * interruption screen over a flagged app ("Display over other apps" allows it from the background). Only the names of the apps and
 * the times they come to the front are read; nothing is stored or sent. A foreground service (persistent notification), so that
 * Android keeps it running. Polling stops while the screen is off.
 */
class FrictionMonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pollJob: Job? = null

    @Volatile
    private var monitored: Map<String, Long> = emptyMap()

    private lateinit var watcher: ForegroundWatcher
    private lateinit var usage: UsageStatsManager

    private val screen = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    stopPolling()
                    watcher.screenOff()
                }
                Intent.ACTION_SCREEN_ON -> startPolling()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        usage = getSystemService(UsageStatsManager::class.java)
        val settings = container.settings
        watcher = ForegroundWatcher(
            ownPackage = packageName,
            gate = container.frictionGate,
            isIgnored = ::isIgnored,
            monitored = { monitored },
            isActive = { settings.isInterceptionActive() },
        )
        registerReceiver(screen, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        })
        scope.launch {
            container.repository.monitoredApps.collect { list ->
                monitored = list.associate { it.app.packageName to it.app.snoozedUntil }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!showNotification()) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!hasUsageAccess(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (getSystemService(PowerManager::class.java)?.isInteractive != false) startPolling()
        return START_STICKY
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(screen) }
        scope.cancel()
        super.onDestroy()
    }

    private fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = scope.launch {
            val start = System.currentTimeMillis()
            if (watcher.lastTimeStamp == 0L) watcher.prime(read(start - PRIME_MILLIS, start), start)
            while (isActive) {
                delay(POLL_MILLIS)
                // Usage access withdrawn in Android's settings: nothing more can be detected.
                if (!hasUsageAccess(this@FrictionMonitorService)) {
                    stopSelf()
                    return@launch
                }
                val target = watcher.onEvents(read(watcher.lastTimeStamp, System.currentTimeMillis())) ?: continue
                if (!hasOverlayPermission(this@FrictionMonitorService)) continue
                Log.d(TAG, "interrupt $target")
                startActivity(InterstitialActivity.intent(this@FrictionMonitorService, target))
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    /** The "activity resumed" events between [from] and [to] (Android keeps them for a few days; we only read the last moments). */
    private fun read(from: Long, to: Long): List<UsageEventLite> {
        val events = usage.queryEvents(from, to) ?: return emptyList()
        val out = ArrayList<UsageEventLite>()
        val event = UsageEvents.Event()
        while (events.getNextEvent(event)) {
            if (event.eventType == ForegroundWatcher.ACTIVITY_RESUMED) {
                out += UsageEventLite(event.packageName, event.eventType, event.timeStamp)
            }
        }
        return out
    }

    private fun isIgnored(pkg: String): Boolean {
        if (pkg == SYSTEM_UI) return true
        val imm = getSystemService(InputMethodManager::class.java) ?: return false
        return imm.enabledInputMethodList.any { it.packageName == pkg }
    }

    /** The persistent notification Android requires for a foreground service; false if Android refuses the start. */
    private fun showNotification(): Boolean {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, getString(R.string.monitor_channel_name), NotificationManager.IMPORTANCE_MIN).apply {
                    setShowBadge(false)
                },
            )
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, HomeActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.monitor_notification_title, getString(R.string.app_name)))
            .setContentText(getString(R.string.monitor_notification_text))
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
        return try {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
            true
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && e is ForegroundServiceStartNotAllowedException) {
                Log.w(TAG, "foreground start refused", e)
                false
            } else {
                throw e
            }
        }
    }

    companion object {
        private const val TAG = "RedFlagMonitor"
        private const val SYSTEM_UI = "com.android.systemui"
        private const val CHANNEL = "monitor"
        private const val NOTIFICATION_ID = 1

        /** How often the foreground app is read. */
        const val POLL_MILLIS = 500L

        /** At start, how far back the events are read to know the app already in front. */
        private const val PRIME_MILLIS = 60_000L

        /** Starts the watch if both permissions are granted (from a screen of the app, at boot, after an update). */
        fun startIfPermitted(context: Context) {
            if (!hasFrictionPermissions(context)) return
            try {
                context.startForegroundService(Intent(context, FrictionMonitorService::class.java))
            } catch (e: IllegalStateException) {
                // Android 12+ refuses a start from the background outside the allowed cases: the next screen of the app retries.
                Log.w(TAG, "start refused", e)
            }
        }
    }
}

/** Starts the watch at boot and after an update of the app (the service does not survive either). */
class FrictionBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED ->
                FrictionMonitorService.startIfPermitted(context)
        }
    }
}
