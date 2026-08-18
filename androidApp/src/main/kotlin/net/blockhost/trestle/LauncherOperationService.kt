package net.blockhost.trestle

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import net.blockhost.trestle.ui.OperationStatus

class LauncherOperationService : Service() {
    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Launcher operations",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Install and download progress"
            },
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Trestle is working"
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
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "launcher-operations"
        private const val NOTIFICATION_ID = 4102
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_DETAIL = "detail"
        private const val EXTRA_COMPLETED = "completed"
        private const val EXTRA_TOTAL = "total"

        fun update(context: Context, operation: OperationStatus) {
            context.startForegroundService(
                Intent(context, LauncherOperationService::class.java).apply {
                    putExtra(EXTRA_TITLE, operation.title)
                    putExtra(EXTRA_DETAIL, operation.detail)
                    putExtra(EXTRA_COMPLETED, operation.completed ?: -1L)
                    putExtra(EXTRA_TOTAL, operation.total ?: -1L)
                },
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LauncherOperationService::class.java))
        }
    }
}
