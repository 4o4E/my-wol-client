package top.e404.mywol

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import top.e404.mywol.util.AndroidLoggingConfigurator
import top.e404.mywol.util.afterVer
import top.e404.mywol.util.error
import top.e404.mywol.util.getCommonKoinModule
import top.e404.mywol.util.logger
import top.e404.mywol.vm.LocalVm
import top.e404.mywol.vm.RemoteVm
import top.e404.mywol.vm.ScheduleVm
import kotlin.system.exitProcess

class AndroidMain : ComponentActivity() {
    companion object {
        lateinit var appContext: Context
        lateinit var mainActivity: AndroidMain
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val logsDir = applicationContext
            .externalCacheDir!!
            .resolve("logs")
            .also { it.mkdirs() }
            .absolutePath
        AndroidLoggingConfigurator.configure(logsDir)

        val defaultUEH = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            logger<AndroidMain>().error(e) { "!!!MYWOL FINAL EXCEPTION!!!" }
            Thread.sleep(500)
            defaultUEH?.uncaughtException(t, e)
        }
        appContext = applicationContext
        mainActivity = this

        afterVer(Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "service_channel",
                "服务通知",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
        afterVer(Build.VERSION_CODES.TIRAMISU) {
            // 已有权限
            val current = appContext.checkSelfPermission(POST_NOTIFICATIONS)
            if (current == PackageManager.PERMISSION_GRANTED) return@afterVer
            // 请求权限
            ActivityCompat.requestPermissions(this, arrayOf(POST_NOTIFICATIONS), 1)
        }

        if (GlobalContext.getOrNull() == null) startKoin {
            androidContext(this@AndroidMain)
            modules(getCommonKoinModule({ this@AndroidMain }))
        }

        LocalVm.startSync()
        ScheduleVm.start()

        System.setProperty("user.home", applicationContext.applicationInfo.dataDir)

        setContent { App() }
    }

    override fun onDestroy() {
        super.onDestroy()
        RemoteVm.stop()
        LocalVm.stop()
        stopKoin()
        exitProcess(0)
    }
}