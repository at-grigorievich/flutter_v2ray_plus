package com.wisecodex.flutter_v2ray.xray.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.wisecodex.flutter_v2ray.xray.service.XrayVPNService
import com.wisecodex.flutter_v2ray.xray.utils.AppConfigs

class AutoDisconnectReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.wisecodex.flutter_v2ray.AUTO_DISCONNECT") return

        //android.util.Log.d("AutoDisconnectReceiver", "onReceive triggered at ${System.currentTimeMillis()}")

        XrayCoreManager.saveAutoDisconnectTimestamp(context)
        showExpiryNotification(context)

        try {
            val stopIntent = Intent(context, XrayVPNService::class.java).apply {
                putExtra("COMMAND", AppConfigs.V2RAY_SERVICE_COMMANDS.STOP_SERVICE)
            }
            context.startService(stopIntent)
        } catch (e: Exception) {
            //android.util.Log.e("AutoDisconnectReceiver", "Failed to stop VPN: ${e.message}")
        }
    }

    private fun showExpiryNotification(context: Context) {
        val channelId = "vpn_session_expired"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "VPN Session",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    enableVibration(true)
                    setShowBadge(true)
                }
            )
        }

        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, flags)

        val message = context
            .getSharedPreferences("flutter_v2ray_prefs", Context.MODE_PRIVATE)
            .getString("expiry_notification_message", "VPN session expired")

        val appName = context.applicationInfo
            .loadLabel(context.packageManager).toString()

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(context, channelId)
        } else {
            @Suppress("DEPRECATION")
            android.app.Notification.Builder(context)
        }.apply {
            setContentTitle(appName)
            setContentText(message)
            val iconResId = context.resources.getIdentifier(
                "ic_notification", "mipmap", context.packageName
            )
            setSmallIcon(if (iconResId != 0) iconResId else context.applicationInfo.icon)
            setContentIntent(pendingIntent)
            setAutoCancel(true)
        }.build()

        notificationManager.notify(0x6B06, notification)
    }
}