@file:Suppress("NOTHING_TO_INLINE", "KotlinRedundantDiagnosticSuppress")

package top.e404.mywol

import androidx.compose.ui.graphics.painter.Painter
import androidx.room.RoomDatabase
import com.russhwolf.settings.Settings
import top.e404.mywol.dao.WolDatabase
import top.e404.mywol.vm.Result
import java.io.File

expect object Platform {
    /**
     * 平台名字
     */
    val name: String

    /**
     * 选择文件夹保存文件
     *
     * @return 保存的文件路径
     */
    suspend fun exportChooseDir(content: String): Result<String>

    /**
     * 选择文件导入数据
     *
     * @return 选择的文件路径
     */
    suspend fun importChooseFile(): Result<String>
}

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