package top.e404.mywol

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.documentfile.provider.DocumentFile
import androidx.room.Room
import androidx.room.RoomDatabase
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
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

    @Composable
    actual fun ExportChooseDir(deferred: CompletableDeferred<Result<String>>) {
        val filePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) launcher@{ result ->
            if (result.resultCode != RESULT_OK) {
                deferred.complete(Result.fail("取消导出"))
                return@launcher
            }
            val uri = result.data?.data ?: run {
                deferred.complete(Result.fail("取消导出"))
                return@launcher
            }
            AppLog.debug { "exportChooseDir: $uri" }
            AndroidMain.mainActivity.grantUriPermission(
                AndroidMain.mainActivity.packageName,
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            AndroidMain.mainActivity.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            UiVm.ioScope.launch {
                @Suppress("SimpleDateFormat")
                val date = SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(Date())
                val docFile = DocumentFile.fromTreeUri(AndroidMain.mainActivity, uri)!!
                val newFile = docFile.createFile("application/json", "my-wol-export-${date}")
                val fileUri = newFile!!.uri
                val stream = AndroidMain.mainActivity.contentResolver.openOutputStream(fileUri, "w") ?: run {
                    deferred.complete(Result.fail("无效文件夹, 请重新选择"))
                    return@launch
                }
                val json = LocalVm.exportAll()
                stream.bufferedWriter().use { it.write(json) }
                deferred.complete(Result.success("导出完成, 文件位于: ${fileUri.path}"))
            }
        }
        LaunchedEffect(Unit) {
            filePickerLauncher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            })
        }
    }

    @Composable
    actual fun ImportChooseFile(deferred: CompletableDeferred<Result<String>>) {
        val filePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) launcher@{ uri: Uri? ->
            if (uri == null) {
                deferred.complete(Result.fail("取消导入"))
                return@launcher
            }
            AndroidMain.mainActivity.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            UiVm.ioScope.launch {
                val stream = AndroidMain.mainActivity.contentResolver.openInputStream(uri) ?: run {
                    deferred.complete(Result.fail("文件不存在"))
                    return@launch
                }
                val json = stream.bufferedReader().use(BufferedReader::readText)
                deferred.complete(Result.success(json))
            }
        }
        LaunchedEffect(System.currentTimeMillis()) {
            filePickerLauncher.launch("application/json")
        }
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