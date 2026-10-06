package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.ui.navigation.AppDestination

/**
 * Lightweight Android foreground service that displays a silent, ongoing system-level
 * chronometer timer indicator ONLY while a Career Focus session is actively RUNNING.
 *
 * Uses Android's native chronometer (`setUsesChronometer(true)` + `setWhen(baseWallClockMillis)`)
 * so the system UI ticks the elapsed time without any background polling loop in the app.
 */
class FocusTimerService : Service() {

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_START -> {
        val elapsedMillis = intent.getLongExtra(EXTRA_ELAPSED_MILLIS, 0L).coerceAtLeast(0L)
        val baseWallClockMillis = System.currentTimeMillis() - elapsedMillis
        ensureNotificationChannel()
        val notification = buildOngoingTimerNotification(baseWallClockMillis)
        try {
          val fgsType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
              ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
              0
            }
          ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, fgsType)
        } catch (_: Exception) {
          // Safeguard if foreground start is restricted by OS policy in background state
        }
      }
      ACTION_STOP -> {
        stopIndicatorAndSelf()
      }
      else -> {
        stopIndicatorAndSelf()
      }
    }
    return START_NOT_STICKY
  }

  private fun stopIndicatorAndSelf() {
    try {
      ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    } catch (_: Exception) {}
    val notificationManager =
      getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    notificationManager?.cancel(NOTIFICATION_ID)
    stopSelf()
  }

  override fun onDestroy() {
    val notificationManager =
      getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    notificationManager?.cancel(NOTIFICATION_ID)
    super.onDestroy()
  }

  private fun ensureNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val notificationManager =
        getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
      val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
      if (existing == null) {
        val channel =
          NotificationChannel(
              CHANNEL_ID,
              getString(R.string.focus_notification_channel_name),
              NotificationManager.IMPORTANCE_LOW,
            )
            .apply {
              description = getString(R.string.focus_notification_subtitle)
              setSound(null, null)
              enableVibration(false)
              setShowBadge(false)
            }
        notificationManager.createNotificationChannel(channel)
      }
    }
  }

  internal fun buildOngoingTimerNotification(baseWallClockMillis: Long): Notification {
    val openFocusIntent =
      Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(EXTRA_OPEN_DESTINATION, AppDestination.FOCUS.route)
      }
    val pendingIntentFlags =
      PendingIntent.FLAG_UPDATE_CURRENT or
        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
    val contentPendingIntent =
      PendingIntent.getActivity(this, 0, openFocusIntent, pendingIntentFlags)

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_focus_timer_notification)
      .setContentTitle(getString(R.string.focus_notification_title))
      .setContentText(getString(R.string.focus_notification_subtitle))
      .setWhen(baseWallClockMillis)
      .setShowWhen(true)
      .setUsesChronometer(true)
      .setOngoing(true)
      .setSilent(true)
      .setOnlyAlertOnce(true)
      .setAutoCancel(false)
      .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setContentIntent(contentPendingIntent)
      .build()
  }

  companion object {
    const val CHANNEL_ID = "focus_ongoing_timer_channel"
    const val NOTIFICATION_ID = 3001
    const val ACTION_START = "com.example.service.action.START_FOCUS_INDICATOR"
    const val ACTION_STOP = "com.example.service.action.STOP_FOCUS_INDICATOR"
    const val EXTRA_ELAPSED_MILLIS = "extra_elapsed_millis"
    const val EXTRA_OPEN_DESTINATION = "extra_open_destination"

    fun startIndicator(context: Context, currentElapsedMillis: Long) {
      val appContext = context.applicationContext
      val intent =
        Intent(appContext, FocusTimerService::class.java).apply {
          action = ACTION_START
          putExtra(EXTRA_ELAPSED_MILLIS, currentElapsedMillis.coerceAtLeast(0L))
        }
      try {
        ContextCompat.startForegroundService(appContext, intent)
      } catch (_: Exception) {}
    }

    fun stopIndicator(context: Context) {
      val appContext = context.applicationContext
      val intent =
        Intent(appContext, FocusTimerService::class.java).apply {
          action = ACTION_STOP
        }
      try {
        appContext.stopService(intent)
      } catch (_: Exception) {}
      val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      notificationManager?.cancel(NOTIFICATION_ID)
    }
  }
}
