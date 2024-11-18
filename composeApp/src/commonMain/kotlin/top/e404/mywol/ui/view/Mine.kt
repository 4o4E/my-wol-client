package top.e404.mywol.ui.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import org.jetbrains.compose.ui.tooling.preview.Preview
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Preview
@Composable
fun Mine() {
    var showAbout by remember { mutableStateOf(false) }
    if (showAbout) AlertDialog(onDismissRequest = {
        showAbout = false
    }, text = {
        Text(text = "made by 404E and ❤")
    }, confirmButton = {
        ClickableText(
            text = AnnotatedString("确定"),
            onClick = { showAbout = false },
            style = TextStyle(color = MaterialTheme.colorScheme.onPrimaryContainer)
        )
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
        ClickableText(
            text = AnnotatedString("确定"),
            onClick = {
                importConfirm = false
                // todo 跳转主页
            },
            style = TextStyle(color = MaterialTheme.colorScheme.onPrimaryContainer)
        )
    }, dismissButton = {
        ClickableText(
            text = AnnotatedString("取消"),
            onClick = { importConfirm = false },
            style = TextStyle(color = MaterialTheme.colorScheme.onPrimaryContainer)
        )
    })


    // var showExport by remember { mutableStateOf(false) }
    // if (showExport) FileSelector

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(50.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Companion.CenterHorizontally
    ) {
//        Image(
//            painter = painterResource(id = R.drawable.ic_launcher_foreground),
//            contentDescription = "app图标",
//            modifier = Modifier.size(250.dp)
//        )
        Column {
            val spanStyle = SpanStyle(
                fontSize = 1.3.em,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            ClickableText(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                text = AnnotatedString("关于", spanStyle),
                onClick = { showAbout = true },
            )
            HorizontalDivider()
            ClickableText(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                text = AnnotatedString("导出", spanStyle),
                onClick = {
                    val now = LocalDateTime.now().format(formatter)
//                    val fileName = "keepaccounts_export_$now.json"
//                    top.e404.keepaccounts.App.Companion.launch(Dispatchers.IO) {
//                        val export = top.e404.keepaccounts.data.entity.Export(
//                            top.e404.keepaccounts.data.dao.BalanceRecordDao.list(),
//                            top.e404.keepaccounts.data.dao.TagDao.list(),
//                            top.e404.keepaccounts.data.dao.RecordTagDao.list(),
//                        )
//                        android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
//                            .resolve(fileName)
//                            .writeText(
//                                Json.encodeToString(
//                                    top.e404.keepaccounts.data.entity.Export(),
//                                    export
//                                )
//                            )
//                        withContext(Dispatchers.Main) {
//                            exportFile = fileName
//                        }
//                    }
                },
            )
            HorizontalDivider()
            ClickableText(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                text = AnnotatedString("导入", spanStyle),
                onClick = { importConfirm = true },
            )
            // HorizontalDivider()
            // ClickableText(
            //     modifier = Modifier.padding(10.dp),
            //     text = AnnotatedString("设置", spanStyle),
            //     onClick = { controller.navigate(Router.Config) },
            // )
            // HorizontalDivider()
        }
    }
}

private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")