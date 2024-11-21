package top.e404.mywol

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.WindowState
import androidx.room.Room
import androidx.room.RoomDatabase
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import top.e404.mywol.dao.WolDatabase
import java.io.File

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"

    override fun save(content: String) {
        TODO("Not yet implemented")
    }

    override fun load(path: String): String {
        TODO("Not yet implemented")
    }
}

actual fun getPlatform(): Platform = JVMPlatform()

lateinit var factory: Settings.Factory

actual fun getSettings(name: String): Settings {
    if (!::factory.isInitialized) {
        factory = PreferencesSettings.Factory()
    }
    return factory.create(name)
}

actual abstract class Context

@Stable
class DesktopContext(
    val windowState: WindowState,
    val dataDir: File,
    val cacheDir: File,
) : Context()


internal actual val Context.filesImpl: ContextFiles
    get() = object : ContextFiles {
        override val cacheDir = (this@filesImpl as DesktopContext).cacheDir
        override val dataDir = (this@filesImpl as DesktopContext).dataDir
    }

actual fun Context.createDatabaseBuilder(): RoomDatabase.Builder<WolDatabase> {
    this as DesktopContext
    return Room.databaseBuilder<WolDatabase>(
        name = dataDir.resolve("wol.db").absolutePath,
    )
}

actual val appIcon: Painter
    @Composable
    @Suppress("DEPRECATION")
    get() = painterResource("drawable/icon.png")