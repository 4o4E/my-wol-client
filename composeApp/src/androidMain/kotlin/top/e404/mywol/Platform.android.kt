package top.e404.mywol

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.room.Room
import androidx.room.RoomDatabase
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import top.e404.mywol.dao.WolDatabase

class AndroidPlatform : Platform {
    override val name: String = "Android"

    override fun save(content: String) {
        TODO("Not yet implemented")
    }

    override fun load(path: String): String {
        TODO("Not yet implemented")
    }
}

actual fun getPlatform(): Platform = AndroidPlatform()

private lateinit var factory: Settings.Factory

actual fun getSettings(name: String): Settings {
    if (!::factory.isInitialized) {
        factory = SharedPreferencesSettings.Factory(AndroidMain.appContext)
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

actual val appIcon: Painter
    @Composable
    get() = painterResource(id = R.drawable.icon)