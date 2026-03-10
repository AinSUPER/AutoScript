package com.autoscript.advanced.system

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * 通知操作管理
 * 提供通知发送、取消、渠道管理等功能
 */
class NotificationManager(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

    /**
     * 通知配置
     */
    data class NotificationConfig(
        val channelId: String = "default",
        val channelName: String = "默认通知",
        val channelDescription: String = "",
        val importance: Int = android.app.NotificationManager.IMPORTANCE_DEFAULT,
        val enableSound: Boolean = true,
        val enableVibrate: Boolean = true,
        val enableLights: Boolean = true,
        val lightColor: Int = android.graphics.Color.BLUE
    )

    /**
     * 通知构建器
     */
    data class NotificationData(
        val id: Int,
        val title: String,
        val content: String,
        val ticker: String? = null,
        val smallIcon: Int = android.R.drawable.ic_dialog_info,
        val largeIcon: Bitmap? = null,
        val autoCancel: Boolean = true,
        val ongoing: Boolean = false,
        val priority: Int = NotificationCompat.PRIORITY_DEFAULT,
        val category: String? = null,
        val intent: PendingIntent? = null,
        val actions: List<NotificationAction> = emptyList(),
        val style: NotificationStyle? = null,
        val progress: ProgressInfo? = null
    )

    /**
     * 通知动作
     */
    data class NotificationAction(
        val icon: Int,
        val title: String,
        val intent: PendingIntent
    )

    /**
     * 通知样式
     */
    sealed class NotificationStyle {
        data class BigTextStyle(
            val bigText: String,
            val summaryText: String? = null
        ) : NotificationStyle()

        data class BigPictureStyle(
            val bigPicture: Bitmap,
            val summaryText: String? = null
        ) : NotificationStyle()

        data class InboxStyle(
            val lines: List<String>,
            val summaryText: String? = null
        ) : NotificationStyle()

        data class ProgressStyle(
            val max: Int,
            val progress: Int,
            val indeterminate: Boolean = false
        ) : NotificationStyle()
    }

    /**
     * 进度信息
     */
    data class ProgressInfo(
        val max: Int,
        val progress: Int,
        val indeterminate: Boolean = false
    )

    /**
     * 创建通知渠道
     * @param config 通知配置
     */
    fun createChannel(config: NotificationConfig = NotificationConfig()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                config.channelId,
                config.channelName,
                config.importance
            ).apply {
                description = config.channelDescription

                if (config.enableSound) {
                    val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val audioAttributes = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                    setSound(soundUri, audioAttributes)
                } else {
                    setSound(null, null)
                }

                enableVibration(config.enableVibrate)
                enableLights(config.enableLights)
                lightColor = config.lightColor
            }

            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 删除通知渠道
     * @param channelId 渠道ID
     */
    fun deleteChannel(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.deleteNotificationChannel(channelId)
        }
    }

    /**
     * 发送通知
     * @param data 通知数据
     * @param config 通知配置
     * @return 通知ID
     */
    fun notify(data: NotificationData, config: NotificationConfig = NotificationConfig()): Int {
        createChannel(config)

        val builder = NotificationCompat.Builder(context, config.channelId)
            .setSmallIcon(data.smallIcon)
            .setContentTitle(data.title)
            .setContentText(data.content)
            .setAutoCancel(data.autoCancel)
            .setOngoing(data.ongoing)
            .setPriority(data.priority)
            .setWhen(System.currentTimeMillis())

        data.ticker?.let { builder.setTicker(it) }
        data.largeIcon?.let { builder.setLargeIcon(it) }
        data.category?.let { builder.setCategory(it) }
        data.intent?.let { builder.setContentIntent(it) }

        for (action in data.actions) {
            builder.addAction(action.icon, action.title, action.intent)
        }

        when (val style = data.style) {
            is NotificationStyle.BigTextStyle -> {
                builder.setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(style.bigText)
                        .setSummaryText(style.summaryText)
                )
            }
            is NotificationStyle.BigPictureStyle -> {
                builder.setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(style.bigPicture)
                        .setSummaryText(style.summaryText)
                )
            }
            is NotificationStyle.InboxStyle -> {
                val inboxStyle = NotificationCompat.InboxStyle()
                style.lines.forEach { inboxStyle.addLine(it) }
                style.summaryText?.let { inboxStyle.setSummaryText(it) }
                builder.setStyle(inboxStyle)
            }
            is NotificationStyle.ProgressStyle -> {
                builder.setProgress(style.max, style.progress, style.indeterminate)
            }
            null -> {}
        }

        data.progress?.let { progress ->
            builder.setProgress(progress.max, progress.progress, progress.indeterminate)
        }

        notificationManager.notify(data.id, builder.build())

        return data.id
    }

    /**
     * 发送简单通知
     * @param id 通知ID
     * @param title 标题
     * @param content 内容
     * @param channelId 渠道ID
     * @return 通知ID
     */
    fun notifySimple(
        id: Int,
        title: String,
        content: String,
        channelId: String = "default"
    ): Int {
        return notify(
            NotificationData(
                id = id,
                title = title,
                content = content
            ),
            NotificationConfig(channelId = channelId)
        )
    }

    /**
     * 发送进度通知
     * @param id 通知ID
     * @param title 标题
     * @param content 内容
     * @param max 最大进度
     * @param progress 当前进度
     * @param indeterminate 是否不确定进度
     * @param channelId 渠道ID
     * @return 通知ID
     */
    fun notifyProgress(
        id: Int,
        title: String,
        content: String,
        max: Int,
        progress: Int,
        indeterminate: Boolean = false,
        channelId: String = "progress"
    ): Int {
        return notify(
            NotificationData(
                id = id,
                title = title,
                content = content,
                ongoing = progress < max,
                progress = ProgressInfo(max, progress, indeterminate)
            ),
            NotificationConfig(channelId = channelId)
        )
    }

    /**
     * 发送大文本通知
     * @param id 通知ID
     * @param title 标题
     * @param content 简短内容
     * @param bigText 大文本内容
     * @param channelId 渠道ID
     * @return 通知ID
     */
    fun notifyBigText(
        id: Int,
        title: String,
        content: String,
        bigText: String,
        channelId: String = "default"
    ): Int {
        return notify(
            NotificationData(
                id = id,
                title = title,
                content = content,
                style = NotificationStyle.BigTextStyle(bigText)
            ),
            NotificationConfig(channelId = channelId)
        )
    }

    /**
     * 发送图片通知
     * @param id 通知ID
     * @param title 标题
     * @param content 内容
     * @param image 图片
     * @param channelId 渠道ID
     * @return 通知ID
     */
    fun notifyImage(
        id: Int,
        title: String,
        content: String,
        image: Bitmap,
        channelId: String = "image"
    ): Int {
        return notify(
            NotificationData(
                id = id,
                title = title,
                content = content,
                style = NotificationStyle.BigPictureStyle(image)
            ),
            NotificationConfig(channelId = channelId)
        )
    }

    /**
     * 发送列表通知
     * @param id 通知ID
     * @param title 标题
     * @param lines 行列表
     * @param summaryText 摘要文本
     * @param channelId 渠道ID
     * @return 通知ID
     */
    fun notifyInbox(
        id: Int,
        title: String,
        lines: List<String>,
        summaryText: String? = null,
        channelId: String = "inbox"
    ): Int {
        return notify(
            NotificationData(
                id = id,
                title = title,
                content = lines.firstOrNull() ?: "",
                style = NotificationStyle.InboxStyle(lines, summaryText)
            ),
            NotificationConfig(channelId = channelId)
        )
    }

    /**
     * 取消通知
     * @param id 通知ID
     */
    fun cancel(id: Int) {
        notificationManager.cancel(id)
    }

    /**
     * 取消所有通知
     */
    fun cancelAll() {
        notificationManager.cancelAll()
    }

    /**
     * 取消指定标签的通知
     * @param tag 标签
     * @param id 通知ID
     */
    fun cancel(tag: String, id: Int) {
        notificationManager.cancel(tag, id)
    }

    /**
     * 获取活动通知数量
     * @return 通知数量
     */
    fun getActiveNotificationsCount(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.activeNotifications.size
        } else {
            0
        }
    }

    /**
     * 获取活动通知列表
     * @return 通知列表
     */
    fun getActiveNotifications(): Array<out android.service.notification.StatusBarNotification>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.activeNotifications
        } else {
            null
        }
    }

    /**
     * 检查通知渠道是否启用
     * @param channelId 渠道ID
     * @return 是否启用
     */
    fun isChannelEnabled(channelId: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel(channelId)
            channel?.importance != android.app.NotificationManager.IMPORTANCE_NONE
        } else {
            true
        }
    }

    /**
     * 获取通知渠道重要性
     * @param channelId 渠道ID
     * @return 重要性级别
     */
    fun getChannelImportance(channelId: String): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.getNotificationChannel(channelId)?.importance
                ?: android.app.NotificationManager.IMPORTANCE_DEFAULT
        } else {
            android.app.NotificationManager.IMPORTANCE_DEFAULT
        }
    }

    /**
     * 打开通知设置
     * @param channelId 渠道ID (可选)
     */
    fun openNotificationSettings(channelId: String? = null) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (channelId != null) {
                android.content.Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                    putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                    putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, channelId)
                }
            } else {
                android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            }
        } else {
            android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
            }
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * 创建PendingIntent
     * @param intent Intent
     * @param flags 标志
     * @return PendingIntent
     */
    fun createPendingIntent(intent: Intent, flags: Int = PendingIntent.FLAG_UPDATE_CURRENT): PendingIntent {
        return PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags or PendingIntent.FLAG_IMMUTABLE
            } else {
                flags
            }
        )
    }
}
