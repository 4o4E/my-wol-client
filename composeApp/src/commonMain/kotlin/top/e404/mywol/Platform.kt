@file:Suppress("NOTHING_TO_INLINE", "KotlinRedundantDiagnosticSuppress")

package top.e404.mywol

import androidx.compose.ui.graphics.painter.Painter
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

expect val appIcon: Painter