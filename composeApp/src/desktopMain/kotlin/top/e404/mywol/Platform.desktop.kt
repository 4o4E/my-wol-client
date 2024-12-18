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
import kotlinx.coroutines.CompletableDeferred
import top.e404.mywol.dao.WolDatabase
import top.e404.mywol.vm.Result
import java.io.File

actual object Platform {
    actual val name = "Desktop"

    @Composable
    actual fun ExportChooseDir(deferred: CompletableDeferred<Result<String>>) {
        TODO("Not yet implemented")
    }

    @Composable
    actual fun ImportChooseFile(deferred: CompletableDeferred<Result<String>>) {
        TODO("Not yet implemented")
    }
}

private lateinit var factory: Settings.Factory

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