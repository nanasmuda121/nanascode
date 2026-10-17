/**
 * Adapted from Acode (https://github.com/acode-foundation/acode)
 * Copyright (c) 2020 Foxdebug (Ajit Kumar)
 * Licensed under the MIT License. See LICENSE-ACODE.txt for details.
 */
package com.nanascode.app.core.terminal

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.nanascode.app.core.common.SecureLogger

class TerminalService : Service() {

    private val binder = LocalBinder()
    private var wakeLock: PowerManager.WakeLock? = null

    inner class LocalBinder : Binder() {
        fun getService(): TerminalService = this@TerminalService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startInForeground()
        acquireWakeLock()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun startInForeground() {
        val notification = createNotification("Sesi terminal sedang berjalan")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotification(message: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NasCode Terminal")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "NasCode Terminal Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Menjaga proses kompilasi dan terminal tetap aktif"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NasCode:TerminalWakeLock")
            wakeLock?.setReferenceCounted(false)
        }
        wakeLock?.acquire(2 * 60 * 60 * 1000L) // 2 hours max
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        TerminalManager.terminateAll()
    }

    companion object {
        const val CHANNEL_ID = "nascode_terminal_channel"
        const val NOTIFICATION_ID = 2026
        const val ACTION_STOP_SERVICE = "com.nanascode.app.terminal.STOP"

        fun start(context: Context) {
            val intent = Intent(context, TerminalService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, TerminalService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}
