package top.e404.mywol.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ConfirmDialog(
    state: Boolean,
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit = {}
) {
    if (!state) return
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { Text("确定", Modifier.clickable(onClick = onConfirm)) },
        dismissButton = { Text("取消", Modifier.clickable(onClick = onCancel)) }
    )
}