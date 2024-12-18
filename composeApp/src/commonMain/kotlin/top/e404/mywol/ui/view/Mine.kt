package top.e404.mywol.ui.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.unit.dp
import com.russhwolf.settings.set
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import top.e404.mywol.Platform
import top.e404.mywol.Router
import top.e404.mywol.appIcon
import top.e404.mywol.ui.components.ConfirmDialog
import top.e404.mywol.vm.LocalVm
import top.e404.mywol.vm.RemoteVm
import top.e404.mywol.vm.Result
import top.e404.mywol.vm.SettingsVm
import top.e404.mywol.vm.UiVm
import top.e404.mywol.vm.WsState

@Preview
@Composable
fun Mine() {
    var showAbout by remember { mutableStateOf(false) }
    fun getModifier() = Modifier.padding(10.dp).fillMaxWidth()
    if (showAbout) AlertDialog(onDismissRequest = {
        showAbout = false
    }, text = {
        Text(text = "made by 404E and ❤")
    }, confirmButton = {
        Text("确定", getModifier().clickable { showAbout = false })
    })

    var exportFile by remember { mutableStateOf<String?>(null) }
    if (exportFile != null) {
        AlertDialog(
            onDismissRequest = { exportFile = null },
            title = { Text(text = "导出完成") },
            text = { Text(text = "文件位于: 下载/${exportFile}") },
            confirmButton = {
                LinkAnnotation.Clickable(
                    tag = "确定",
                    styles = TextLinkStyles(SpanStyle(MaterialTheme.colorScheme.onPrimaryContainer)),
                    linkInteractionListener = { exportFile = null },
                )
            }
        )
    }

    var importConfirm by remember { mutableStateOf(false) }
    if (importConfirm) AlertDialog(onDismissRequest = {
        importConfirm = true
    }, title = {
        Text(text = "导入确认")
    }, text = {
        Text(text = "导入数据后会覆盖本地现有数据, 确定继续?")
    }, confirmButton = {
        Text(
            "确定",
            getModifier().clickable {
                importConfirm = false
                UiVm.navigate(Router.LOCAL)
            },
        )
    }, dismissButton = {
        Text(
            "取消",
            getModifier().clickable {
                importConfirm = false
            },
        )
    })


    var showImport by remember { mutableStateOf(false) }
    var confirmImport by remember { mutableStateOf(false) }
    var importDeferred by remember { mutableStateOf(CompletableDeferred<Result<String>>()) }
    ConfirmDialog(
        showImport,
        "导入",
        "导入数据后会覆盖本地现有数据, 确定继续?",
        {
            showImport = false
            confirmImport = true
            importDeferred = CompletableDeferred()
            UiVm.ioScope.launch {
                importDeferred.await().onSuccess {
                    LocalVm.importAll(it)
                    UiVm.showSnackbar("导入成功")
                    UiVm.navigate(Router.LOCAL)
                    confirmImport = false
                }.onFail {
                    UiVm.showSnackbar(it)
                }
            }
        },
        { showImport = false }
    )
    if (confirmImport) {
        Platform.ImportChooseFile(importDeferred)
    }

    var showExport by remember { mutableStateOf(false) }
    var exportDeferred by remember { mutableStateOf(CompletableDeferred<Result<String>>()) }
    var exportResult by remember { mutableStateOf("") }
    if (showExport) {
        LaunchedEffect(System.currentTimeMillis()) {
            exportDeferred = CompletableDeferred()
            UiVm.ioScope.launch {
                exportDeferred.await().onSuccess {
                    exportResult = it
                }.onFail {
                    UiVm.showSnackbar(it)
                }
                showExport = false
            }
        }
        Platform.ExportChooseDir(exportDeferred)
    }
    ConfirmDialog(
        exportResult != "",
        "导出",
        exportResult,
        { exportResult = "" },
        { exportResult = "" }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(50.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Companion.CenterHorizontally
    ) {
        Image(
            painter = appIcon,
            contentDescription = "app图标",
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(50.dp))
        )
        Column(Modifier.padding(top = 30.dp)) {
            Text(
                "关于",
                getModifier().clickable {
                    showAbout = true
                },
            )
            HorizontalDivider()
            Text(
                "导出",
                getModifier().clickable {
                    showExport = true
                },
            )
            HorizontalDivider()
            Text(
                "导入",
                getModifier().clickable {
                    showImport = true
                },
            )
            HorizontalDivider()
            Text(
                "重置服务器地址",
                getModifier().clickable {
                    SettingsVm.remote.remove("serverAddress")
                    RemoteVm.stop()
                    RemoteVm.state = WsState.INITIALIZING
                    RemoteVm.initializing = true
                    UiVm.showSnackbar("重置完成")
                },
            )
            HorizontalDivider()
            Text(
                "切换debug",
                getModifier().clickable {
                    val current = !UiVm.isDebug.value
                    UiVm.isDebug.value = current
                    SettingsVm.local["isDebug"] = current
                    UiVm.showSnackbar("已${if (current) "开启" else "关闭"}debug模式")
                },
            )
        }
    }
}