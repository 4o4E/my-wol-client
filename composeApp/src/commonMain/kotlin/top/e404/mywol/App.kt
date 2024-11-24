package top.e404.mywol

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import top.e404.mywol.ui.view.EditMachine
import top.e404.mywol.ui.view.Local
import top.e404.mywol.ui.view.Mine
import top.e404.mywol.ui.view.Remote
import top.e404.mywol.util.logger
import top.e404.mywol.vm.SettingsVm
import top.e404.mywol.vm.UiVm

@Composable
fun App() {
    val controller = rememberNavController()
    val colorScheme =
        if (isSystemInDarkTheme()) darkColorScheme()
        else lightColorScheme()
    var showAdd by remember { UiVm.showAdd }
    var isDebug by remember { UiVm.isDebug }
    LaunchedEffect(Unit) {
        isDebug = SettingsVm.local.getBoolean("isDebug", false)
    }
    MaterialTheme(colorScheme = colorScheme) {
        val bottomBarHeight = 70.dp
        var currentRouter by remember { mutableStateOf(Router.DEFAULT) }
        Scaffold(
            bottomBar = {
                NavigationBar {
                    for (router in Router.entries) {
                        val selected = currentRouter == router
                        NavigationBarItem(
                            selected = selected,
                            icon = { Icon(router.getIcon(selected), null) },
                            label = { Text(router.displayName) },
                            onClick = {
                                if (!selected) {
                                    controller.navigate(router.routerName)
                                    currentRouter = router
                                }
                            },
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
                    CompositionLocalProvider(NavController provides controller) {
                        NavHost(
                            navController = controller,
                            startDestination = Router.LOCAL.routerName
                        ) {
                            for (router in Router.entries) {
                                composable(router.routerName) { router.route() }
                            }
                        }
                    }
                }
            },
            snackbarHost = {
                SnackbarHost(UiVm.globalSnackbarHostState)
            },
            floatingActionButton = {
                if (showAdd) {
                    val modalBottomSheetState = rememberModalBottomSheetState(true) { true }
                    ModalBottomSheet(
                        sheetState = modalBottomSheetState,
                        modifier = Modifier.fillMaxWidth(),
                        onDismissRequest = {
                            showAdd = false
                        }
                    ) {
                        EditMachine()
                    }
                }
                FloatingActionButton(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    onClick = {
                        showAdd = true
                    }
                ) {
                    Text(text = "+", color = MaterialTheme.colorScheme.onPrimary, fontSize = 26.sp)
                }
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
    )
    ;

    companion object {
        val DEFAULT = LOCAL
    }

    fun getIcon(selected: Boolean): ImageVector = if (selected) selectedIcon else unselectedIcon
}

@Stable
val NavController: ProvidableCompositionLocal<NavHostController> = staticCompositionLocalOf {
    error("no resource provided")
}

@Suppress("UNUSED")
val AppLog = logger("App")