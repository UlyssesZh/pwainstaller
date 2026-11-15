package io.github.ulysseszh.pwainstaller

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object ProgressNotifications {
    const val BUILDS = "apk-builds"
    const val FILTERS = "filter-downloads"
    fun create(context: Context, channel: String, title: String, text: String, percent: Int?, settings: Boolean = false, confirmation: Boolean = false): Notification {
        if (Build.VERSION.SDK_INT >= 26) (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(NotificationChannel(channel,
                context.getString(if (channel == FILTERS) R.string.filter_notifications else R.string.build_notifications), NotificationManager.IMPORTANCE_LOW))
        val pending = PendingIntent.getActivity(context, if (settings) 1 else if (confirmation) 2 else 0,
            Intent(context, if (settings) SettingsActivity::class.java else MainActivity::class.java)
                .putExtra("resumeInstallConfirmation", confirmation)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(context, channel).setSmallIcon(R.drawable.ic_app_monochrome)
            .setContentTitle(title).setContentText(text).setContentIntent(pending)
            .setOngoing(true).setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setProgress(100, percent ?: 0, percent == null).build()
    }
    fun requestPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT < 33 || activity.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) return
        val prefs = activity.getSharedPreferences("build-notifications", 0)
        if (!prefs.getBoolean("asked", false)) {
            prefs.edit().putBoolean("asked", true).apply()
            activity.requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 222)
        }
    }
}
