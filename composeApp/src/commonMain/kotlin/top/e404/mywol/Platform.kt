@file:Suppress("NOTHING_TO_INLINE", "KotlinRedundantDiagnosticSuppress")

package top.e404.mywol

import androidx.room.RoomDatabase
import com.russhwolf.settings.Settings
import top.e404.mywol.dao.WolDatabase
import java.io.File

interface Platform {
    val name: String

    fun save(content: String)
    fun load(path: String): String
}

expect fun getPlatform(): Platform

expect fun getSettings(name: String): Settings

expect abstract class Context

val Context.files: ContextFiles get() = filesImpl

internal expect val Context.filesImpl: ContextFiles

interface ContextFiles {
    val cacheDir: File
    val dataDir: File
}

expect fun Context.createDatabaseBuilder(): RoomDatabase.Builder<WolDatabase>

expect object WsService {
    /**
     * 初始化服务
     */
    fun init(clientName: String, serverUrl: String, packetHandler: () -> Unit)

    /**
     * 启动服务
     */
    fun start()

    /**
     * 结束服务
     */
    fun stop()
}