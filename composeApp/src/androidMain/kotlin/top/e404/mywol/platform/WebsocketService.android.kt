package top.e404.mywol.platform

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import top.e404.mywol.AndroidMain
import top.e404.mywol.R
import top.e404.mywol.util.afterVer
import top.e404.mywol.util.debug
import top.e404.mywol.util.logger
import top.e404.mywol.util.ver

actual class WebsocketService : Service() {
    actual companion object {
        private val log = logger(WebsocketService::class)

        @Volatile
        actual var instance: WebsocketService? = null

        private val ctx get() = AndroidMain.appContext

        actual fun start(address: String, id: String, name: String, secret: String?): Boolean {
            val intent = Intent(ctx, WebsocketService::class.java).apply {
                putExtra("address", address)
                putExtra("id", id)
                putExtra("name", name)
                putExtra("secret", secret)
            }
            val exists = ver(Build.VERSION_CODES.O,
                { ctx.startForegroundService(intent) },
                { ctx.startService(intent) })
            return exists == null
        }

        actual fun stop() {
            ctx.stopService(Intent(ctx, WebsocketService::class.java))
        }

        private val channelId by lazy {
            val id = "SERVICE_CHANNEL"
            afterVer(Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    id,
                    "服务消息通知",
                    NotificationManager.IMPORTANCE_LOW
                )
                ctx.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
            }
            id
        }
    }

    @Volatile
    actual var handler: WebsocketHandler? = null
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    // 创建一个通知
    lateinit var notification: Notification private set

    override fun onBind(intent: Intent) = null
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val id = intent.getStringExtra("id")!!
        val name = intent.getStringExtra("name")!!
        val address = intent.getStringExtra("address")!!
        val secret = intent.getStringExtra("secret")!!
        log.debug { "启动服务: id: $id, name: $name, address: $address, secret: $secret" }
        handler = WebsocketHandler(id, name, address)
        // 启动前台服务
        notification = NotificationCompat.Builder(ctx, channelId)
            .setContentTitle("wol远程连接服务")
            .setContentText("服务正在运行...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSmallIcon(R.drawable.icon)
            .build()
        startForeground(1, notification)
        handler!!.start()

        log.debug { "已启动服务" }
        // 确保服务在资源不足时不会被系统终止
        return START_STICKY
    }

    override fun onDestroy() {
        log.debug("销毁服务")
        super.onDestroy()
        handler?.closeWebsocket?.invoke()
        instance = null
        handler = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        log.debug("已销毁服务")
    }
}