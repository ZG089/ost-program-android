package com.ost.application.share

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ost.application.R
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

object NotificationHelper {

    private const val CHANNEL_ID = "ost_file_transfer_channel_wear"
    private const val CHANNEL_NAME = "File Transfers (Wear)"
    const val NOTIFICATION_ID_TRANSFER = 11224

    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description =
                context.getString(R.string.notifications_for_file_transfer_progress_and_status)
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
        }
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    private fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showTransferNotification(
        context: Context,
        fileName: String,
        progress: Int,
        totalSize: Long,
        isSending: Boolean,
        isIndeterminate: Boolean = false
    ) {
        if (!canPostNotifications(context)) return

        val title = if (isSending) context.getString(R.string.sending_notification, fileName) else context.getString(
            R.string.receiving_notification, fileName
        )
        val iconRes = if (isSending) R.drawable.ic_upload_24dp else R.drawable.ic_download_24dp

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress.coerceIn(0, 100), isIndeterminate)

        if (!isIndeterminate && totalSize > 0) {
            val currentBytes = (progress / 100.0 * totalSize).toLong()
            val readableProgress = formatFileSize(context, currentBytes)
            val readableTotal = formatFileSize(context, totalSize)
            builder.setContentText("$readableProgress / $readableTotal (${progress}%)")
        } else if (!isIndeterminate) {
            builder.setContentText("${progress}%")
        } else {
            builder.setContentText(if (isSending) context.getString(R.string.connecting_notification) else context.getString(
                R.string.waiting_for_sender
            ))
        }

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_TRANSFER, builder.build())
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showCompletionNotification(
        context: Context,
        fileName: String,
        isSuccess: Boolean,
        errorMessage: String? = null
    ) {
        if (!canPostNotifications(context)) return

        val title = when {
            isSuccess -> context.getString(R.string.received_notification, fileName)
            else -> context.getString(R.string.receive_failed_notification, fileName)
        }
        val message = if (isSuccess) {
            context.getString(R.string.transfer_complete)
        } else {
            errorMessage ?: context.getString(R.string.an_unknown_error_occurred)
        }

        val iconRes = if (isSuccess) R.drawable.ic_check_circle_24dp else R.drawable.ic_error_24dp

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(if (isSuccess) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setProgress(0, 0, false)
            .setOngoing(false)
            .setTimeoutAfter(if (isSuccess) 30000 else 60000)


        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_TRANSFER, builder.build())
    }

    fun cancelNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_TRANSFER)
    }

    // Твоя функция форматирования размера файла
    private fun formatFileSize(context: Context, size: Long): String {
        if (size <= 0) return "0 ${context.getString(R.string.b)}"
        val units = arrayOf(
            context.getString(R.string.b),
            context.getString(R.string.kb),
            context.getString(R.string.mb),
            context.getString(R.string.gb),
            context.getString(R.string.tb))
        val digitGroups = if (size > 0) (log10(size.toDouble()) / log10(1024.0)).toInt() else 0
        val safeDigitGroups = digitGroups.coerceIn(0, units.size - 1)
        val sizeInUnit = size / 1024.0.pow(safeDigitGroups.toDouble())
        val formattedSize = if (safeDigitGroups == 0) {
            String.format(Locale.US, "%d", size.toInt())
        } else {
            String.format(Locale.US, "%.1f", sizeInUnit)
        }
        return "$formattedSize ${units[safeDigitGroups]}"
    }
}