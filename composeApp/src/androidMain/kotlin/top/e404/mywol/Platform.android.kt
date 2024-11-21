package top.e404.mywol

import android.app.Service
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.core.app.NotificationCompat
import androidx.room.Room
import androidx.room.RoomDatabase
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.e404.mywol.dao.WolDatabase
import top.e404.mywol.util.logger

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"

    override fun save(content: String) {
        TODO("Not yet implemented")
    }

    override fun load(path: String): String {
        TODO("Not yet implemented")
    }
}

actual fun getPlatform(): Platform = AndroidPlatform()

lateinit var factory: Settings.Factory

actual fun getSettings(name: String): Settings {
    if (!::factory.isInitialized) {
        factory = SharedPreferencesSettings.Factory(MainActivity.appContext)
    }
    return factory.create(name)
}

actual typealias Context = android.content.Context

internal actual val Context.filesImpl: ContextFiles
    get() = object : ContextFiles {
        override val cacheDir get() = (this@filesImpl.cacheDir)
        override val dataDir get() = (this@filesImpl.filesDir)
    }

actual fun Context.createDatabaseBuilder(): RoomDatabase.Builder<WolDatabase> {
    return Room.databaseBuilder<WolDatabase>(
        context = applicationContext,
        name = applicationContext.getDatabasePath("wol.db").absolutePath,
    )
}

/**
 * 和远程服务器进行数据交互的服务
 */
class WebsocketService : Service() {
    private val log = logger(WebsocketService::class)
    private val scope = CoroutineScope(CoroutineExceptionHandler { ctx, t ->
        log.error("Unhandled exception in websocket service scope, coroutineContext: $ctx", t)
    } + SupervisorJob())

    override fun onBind(intent: Intent) = null
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val clientName = intent.getStringExtra("clientName")!!
        val serverAddress = intent.getStringExtra("serverAddress")!!
        // 创建一个通知
        val notification = NotificationCompat.Builder(this, "service_channel")
            .setContentTitle("wol远程连接服务")
            .setContentText("服务正在运行...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
        // 启动前台服务
        startForeground(1, notification)
        // 处理后台任务
        scope.launch(Dispatchers.IO) {
            // 模拟长时间运行的任务
            while (true) {
                log.debug("服务运行中...")
                delay(5000) // 每隔 5 秒执行一次任务
            }
        }

        // 确保服务在资源不足时不会被系统终止
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        log.debug("服务已销毁")
    }
}

actual val appIcon: Painter
    @Composable
    get() = painterResource(id = R.drawable.icon)