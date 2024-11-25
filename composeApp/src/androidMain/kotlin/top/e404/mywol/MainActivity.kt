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
import org.koin.core.context.startKoin
import top.e404.mywol.util.afterVer
import top.e404.mywol.util.getCommonKoinModule
import top.e404.mywol.vm.LocalVm

class MainActivity : ComponentActivity() {
    companion object {
        lateinit var appContext: Context
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContext = applicationContext

        afterVer(Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "service_channel",
                "服务通知",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }

        startKoin {
            androidContext(this@MainActivity)
            modules(getCommonKoinModule({ this@MainActivity }))
        }

        LocalVm.startSync()

        setContent {
            afterVer(Build.VERSION_CODES.TIRAMISU) {
                // 已有权限
                val current = appContext.checkSelfPermission(POST_NOTIFICATIONS)
                if (current == PackageManager.PERMISSION_GRANTED) return@afterVer
                // 请求权限
                ActivityCompat.requestPermissions(this, arrayOf(POST_NOTIFICATIONS), 1)
            }
            App()
        }
    }
}