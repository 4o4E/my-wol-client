package top.e404.mywol.vm

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import top.e404.mywol.dao.Machine

object UiVm {
    private val ioScope = CoroutineScope(Dispatchers.IO)

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