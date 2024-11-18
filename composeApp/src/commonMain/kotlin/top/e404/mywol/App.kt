package top.e404.mywol

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import top.e404.mywol.ui.view.EditMachine
import top.e404.mywol.ui.view.Local
import top.e404.mywol.ui.view.Mine
import top.e404.mywol.ui.view.Remote

@Composable
fun App() {
    val controller = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val colorScheme =
        if (isSystemInDarkTheme()) darkColorScheme()
        else lightColorScheme()
    var showAdd by remember { mutableStateOf(false) }
    MaterialTheme(colorScheme = colorScheme) {
        val bottomBarHeight = 70.dp
        Scaffold(
            bottomBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BottomAppBarDefaults.bottomAppBarFabColor)
                        .height(bottomBarHeight),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    BottomRouterButton(
                        icon = Icons.AutoMirrored.Outlined.FormatListBulleted,
                        text = "本地"
                    ) {
                        controller.navigate(Router.LOCAL)
                    }
                    BottomRouterButton(
                        icon = Icons.AutoMirrored.Outlined.Label,
                        text = "远程"
                    ) {
                        controller.navigate(Router.REMOTE)
                    }
                    BottomRouterButton(
                        icon = Icons.Default.Person,
                        text = "我的"
                    ) {
                        controller.navigate(Router.MINE)
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
                        CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
                            NavHost(navController = controller, startDestination = Router.LOCAL) {
                                composable(Router.LOCAL) { Local() }
                                composable(Router.REMOTE) { Remote() }
                                composable(Router.MINE) { Mine() }
                            }
                        }
                    }
                }
            },
            snackbarHost = {
                SnackbarHost(snackbarHostState)
            },
            floatingActionButton = {
                @OptIn(ExperimentalMaterial3Api::class)
                if (showAdd) {
                    val modalBottomSheetState = rememberModalBottomSheetState(true) { true }
                    ModalBottomSheet(
                        sheetState = modalBottomSheetState,
                        modifier = Modifier.fillMaxWidth(),
                        onDismissRequest = {
                            scope.launch {
                                showAdd = false
                            }
                        }
                    ) {
                        EditMachine { showAdd = false }
                    }
                }
                FloatingActionButton(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    onClick = {
                        scope.launch {
                            showAdd = true
                        }
                    }
                ) {
                    Text(text = "+", color = MaterialTheme.colorScheme.onPrimary, fontSize = 26.sp)
                }
            }
        )


    }
}

object Router {
    /**
     * 本地机器界面
     */
    const val LOCAL = "local"

    /**
     * 链接服务器后展示其他client连接的设备
     */
    const val REMOTE = "remote"

    /**
     * 我的界面
     */
    const val MINE = "mine"
}

val LocalSnackbarHostState: ProvidableCompositionLocal<SnackbarHostState> =
    staticCompositionLocalOf {
        error("no snackbarHostState provided")
    }

@Stable
val NavController: ProvidableCompositionLocal<NavHostController> = staticCompositionLocalOf {
    error("no resource provided")
}

@Composable
fun BottomRouterButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Column(
        Modifier
            .clickable(onClick = onClick)
            .padding(40.dp, 5.dp)
    ) {
        Icon(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            imageVector = icon,
            contentDescription = null
        )
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = text
        )
    }
}