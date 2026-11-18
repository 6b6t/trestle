package net.blockhost.trestle

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import net.blockhost.trestle.text.load
import net.blockhost.trestle.ui.OperationStatus
import java.util.Locale

class LauncherOperationService : Service() {
    private var channelLanguage: String? = null

    // Checked on every progress update, not only when the service starts, so the channel name follows a language
    // change made while a download runs. Creating an existing channel again only updates its name and description.
    private fun updateNotificationChannel() {
        val language = Locale.getDefault().toLanguageTag()
        if (language == channelLanguage) return
        channelLanguage = language
        val labels = launcherLanguageResources()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                labels.getString(R.string.notification_channel_operations),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = labels.getString(R.string.notification_channel_operations_description)
            },
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateNotificationChannel()
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: launcherLanguageResources().getString(R.string.notification_trestle_is_working)
        val detail = intent?.getStringExtra(EXTRA_DETAIL)
        val completed = intent?.getLongExtra(EXTRA_COMPLETED, -1L) ?: -1L
        val total = intent?.getLongExtra(EXTRA_TOTAL, -1L) ?: -1L
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                this.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_trestle_monochrome)
            .setContentTitle(title)
            .setContentText(detail)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .apply {
                if (completed >= 0 && total > 0) {
                    setProgress(100, ((completed.toDouble() / total) * 100).toInt().coerceIn(0, 100), false)
                } else {
                    setProgress(0, 0, true)
                }
            }
            .build()
        startForeground(NOTIFICATION_ID, notification)
        running = true
        return START_NOT_STICKY
    }

    // Android 15 limits dataSync services to six hours a day. Stop cleanly instead of being killed.
    override fun onTimeout(startId: Int, fgsType: Int) {
        stopSelf()
    }

    override fun onDestroy() {
        running = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "launcher-operations"
        private const val NOTIFICATION_ID = 4102
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_DETAIL = "detail"
        private const val EXTRA_COMPLETED = "completed"
        private const val EXTRA_TOTAL = "total"

        @Volatile
        private var running = false

        suspend fun update(context: Context, operation: OperationStatus) {
            val title = operation.title.load()
            val detail = operation.detail?.load()
            val intent = Intent(context, LauncherOperationService::class.java).apply {
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_DETAIL, detail)
                putExtra(EXTRA_COMPLETED, operation.completed ?: -1L)
                putExtra(EXTRA_TOTAL, operation.total ?: -1L)
            }
            // A running foreground service keeps the app in the foreground, so updates can use startService.
            // Starting a new foreground service from the background is not allowed. The operation then runs
            // without a notification instead of crashing the app.
            try {
                if (running) context.startService(intent) else context.startForegroundService(intent)
            } catch (_: IllegalStateException) {
                // ForegroundServiceStartNotAllowedException and BackgroundServiceStartNotAllowedException.
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LauncherOperationService::class.java))
        }
    }
}
