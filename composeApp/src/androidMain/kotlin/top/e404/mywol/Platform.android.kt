package top.e404.mywol

import android.app.Activity.RESULT_OK
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.documentfile.provider.DocumentFile
import androidx.room.Room
import androidx.room.RoomDatabase
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import top.e404.mywol.activity.startActivity
import top.e404.mywol.dao.WolDatabase
import top.e404.mywol.util.debug
import top.e404.mywol.vm.LocalVm
import top.e404.mywol.vm.Result
import top.e404.mywol.vm.UiVm
import java.io.BufferedReader
import java.text.SimpleDateFormat
import java.util.Date

actual object Platform {
    actual val name = "Android"

    actual suspend fun exportChooseDir(content: String): Result<String> {
        val deferred = CompletableDeferred<Result<String>>()
        startActivity start@{
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) register@{ result ->
                if (result.resultCode != RESULT_OK) {
                    startActivity(Intent(this, AndroidMain::class.java))
                    deferred.complete(Result.fail("取消选择"))
                    return@register
                }
                val uri = result.data?.data ?: run {
                    deferred.complete(Result.fail("取消导出"))
                    return@register
                }
                AppLog.debug { "exportChooseDir: $uri" }
                grantUriPermission(
                    packageName,
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                UiVm.ioScope.launch {
                    @Suppress("SimpleDateFormat")
                    val date = SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(Date())
                    val docFile = DocumentFile.fromTreeUri(this@start, uri)!!
                    val newFile = docFile.createFile("text/plain", "my-wol-export-${date}.json")
                    val fileUri = newFile!!.uri
                    val stream = contentResolver.openOutputStream(fileUri, "w") ?: run {
                        deferred.complete(Result.fail("无效文件夹, 请重新选择"))
                        return@launch
                    }
                    val json = LocalVm.exportAll()
                    stream.bufferedWriter().use { it.write(json) }
                    deferred.complete(Result.fail("导出完成, 文件位于: ${fileUri.path}"))
                }
                startActivity(Intent(this, AndroidMain::class.java))
                deferred.complete(Result.fail("取消选择"))
            }.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            })
        }
        return deferred.await()
    }

    actual suspend fun importChooseFile(): Result<String> {
        val deferred = CompletableDeferred<Result<String>>()
        startActivity start@{
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) register@{ result ->
                if (result.resultCode != RESULT_OK) {
                    startActivity(Intent(this, AndroidMain::class.java))
                    deferred.complete(Result.fail("取消选择"))
                    return@register
                }
                val uri = result.data?.data ?: run {
                    deferred.complete(Result.fail("文件不存在"))
                    return@register
                }
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                UiVm.ioScope.launch {
                    val stream = contentResolver.openInputStream(uri) ?: run {
                        deferred.complete(Result.fail("文件不存在"))
                        return@launch
                    }
                    val json = stream.bufferedReader().use(BufferedReader::readText)
                    deferred.complete(Result.success(json))
                }
                startActivity(Intent(this, AndroidMain::class.java))
                deferred.complete(Result.fail("取消选择"))
            }.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            })
        }
        return deferred.await()
    }
}

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