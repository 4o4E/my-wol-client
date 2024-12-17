package top.e404.mywol.vm

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.e404.mywol.Router
import top.e404.mywol.dao.Machine
import top.e404.mywol.util.debug
import top.e404.mywol.util.logger

object UiVm {
    private val log = logger()
    var currentRouter by mutableStateOf(Router.LOCAL)
    lateinit var controller: NavHostController private set
    val ioScope = CoroutineScope(Dispatchers.IO)

    @Composable
    fun initController() {
        controller = rememberNavController()
        controller.addOnDestinationChangedListener { _, target, _ ->
            target.route?.let { currentRouter = Router.fromRoute(it) }
        }
    }

    fun navigate(router: Router) {
        if (currentRouter == router) {
            log.debug { "跳过当前路由: $router" }
            return
        }
        currentRouter = router
        ioScope.launch {
            withContext(Dispatchers.Main) {
                controller.navigate(router.routerName)
            }
        }
    }

    /**
     * 展示一个 Snackbar 并且不阻塞
     */
    fun showSnackbar(
        message: String,
        actionLabel: String? = null,
        duration: SnackbarDuration = SnackbarDuration.Short
    ) {
        ioScope.launch(Dispatchers.IO) {
            globalSnackbarHostState.currentSnackbarData?.dismiss()
            globalSnackbarHostState.showSnackbar(
                message,
                actionLabel,
                true,
                duration
            )
        }
    }

    val globalSnackbarHostState = SnackbarHostState()

    val showAdd = mutableStateOf(false)
    val editMachine = mutableStateOf<Machine?>(null)
    val isDebug = mutableStateOf(false)
    val debug get() = isDebug.value
}