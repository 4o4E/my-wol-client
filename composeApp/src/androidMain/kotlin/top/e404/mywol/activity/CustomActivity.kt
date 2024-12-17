package top.e404.mywol.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.e404.mywol.AndroidMain

private var importLauncher: (CustomActivity.() -> Unit)? = null

suspend fun startActivity(block: CustomActivity.() -> Unit) {
    importLauncher = block
    withContext(Dispatchers.Main) {
        AndroidMain.mainActivity.startActivity(Intent(AndroidMain.appContext, CustomActivity::class.java))
    }
}

class CustomActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        importLauncher?.invoke(this) ?: run {
            startActivity(Intent(this, AndroidMain::class.java))
        }
    }
}