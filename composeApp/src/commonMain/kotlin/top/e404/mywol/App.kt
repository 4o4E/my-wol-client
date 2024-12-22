package top.e404.mywol

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kotlinx.coroutines.delay
import top.e404.mywol.ui.view.Local
import top.e404.mywol.ui.view.Mine
import top.e404.mywol.ui.view.Remote
import top.e404.mywol.util.logger
import top.e404.mywol.vm.RemoteVm
import top.e404.mywol.vm.SettingsVm
import top.e404.mywol.vm.UiVm

@Composable
fun App() {
    UiVm.initController()
    val colorScheme =
        if (isSystemInDarkTheme()) darkColorScheme()
        else lightColorScheme()
    LaunchedEffect(Unit) {
        UiVm.isDebug.value = SettingsVm.local.getBoolean("isDebug", false)
    }
    LaunchedEffect(Unit) {
        delay(1000)
        if (SettingsVm.remote.getString("serverAddress", "").isNotEmpty()) {
            RemoteVm.startWebsocket()
        }
    }
    MaterialTheme(colorScheme = colorScheme) {
        val bottomBarHeight = 70.dp
        Scaffold(
            bottomBar = {
                NavigationBar {
                    for (router in Router.entries) {
                        val selected = UiVm.currentRouter == router
                        NavigationBarItem(
                            selected = selected,
                            icon = { Icon(router.getIcon(selected), null) },
                            label = { Text(router.displayName) },
                            onClick = { UiVm.navigate(router) },
                        )
                    }
                }
            },
            content = {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = bottomBarHeight),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(
                        navController = UiVm.controller,
                        startDestination = UiVm.currentRouter.routerName
                    ) {
                        for (router in Router.entries) {
                            composable(router.routerName) { router.route() }
                        }
                    }
                }
            },
            snackbarHost = {
                SnackbarHost(UiVm.globalSnackbarHostState)
            }
        )
    }
}

enum class Router(
    val routerName: String,
    val displayName: String,
    val unselectedIcon: ImageVector,
    val selectedIcon: ImageVector,
    val route: @Composable () -> Unit
) {
    /**
     * 本地机器界面
     */
    LOCAL(
        "local",
        "本地",
        Icons.AutoMirrored.Outlined.FormatListBulleted,
        Icons.AutoMirrored.Filled.FormatListBulleted,
        { Local() }
    ),

    /**
     * 链接服务器后展示其他client连接的设备
     */
    REMOTE(
        "remote",
        "远程",
        Icons.Outlined.WifiTethering,
        Icons.Filled.WifiTethering,
        { Remote() }
    ),

    /**
     * 我的界面
     */
    MINE(
        "mine",
        "我的",
        Icons.Outlined.Person,
        Icons.Filled.Person,
        { Mine() }
    ),
    ;

    companion object {
        fun fromRoute(route: String) = entries.first { it.routerName == route }
    }

    fun getIcon(selected: Boolean): ImageVector = if (selected) selectedIcon else unselectedIcon
}

@Suppress("UNUSED")
val AppLog = logger("App")