package top.e404.mywol.vm

import androidx.compose.material3.SnackbarHostState

object UiVm {
    suspend fun showSnackbar(message: String) {
        globalSnackbarHostState.showSnackbar(message)
    }

    val globalSnackbarHostState = SnackbarHostState()
}