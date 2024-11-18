package top.e404.mywol.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.e404.mywol.LocalSnackbarHostState
import top.e404.mywol.dao.Machine
import top.e404.mywol.model.MachineState
import top.e404.mywol.ui.view.EditMachine
import top.e404.mywol.vm.LocalVm
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MachineItem(machine: Machine) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(5.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
//                .clickable(onClick = onClick)
    ) {
        val scope = rememberCoroutineScope()
        var showEdit by remember { mutableStateOf(false) }
        val modalBottomSheetState = rememberModalBottomSheetState(true) { true }
        var time by remember { mutableStateOf("") }
        val snackbar = LocalSnackbarHostState.current
        var state by remember { mutableStateOf(MachineState.UNKNOWN) }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(15.dp)
        ) {
            Row {
                Icon(
                    modifier = Modifier
                        .align(Alignment.CenterVertically),
                    imageVector = Icons.Filled.Circle,
                    contentDescription = null,
                    tint = when (state) {
                        top.e404.mywol.model.MachineState.ON -> Color.Green
                        top.e404.mywol.model.MachineState.OFF -> Color.Red
                        top.e404.mywol.model.MachineState.UNKNOWN -> Color.LightGray
                    }
                )
                Spacer(Modifier.width(10.dp))
                Text(machine.name, Modifier, fontSize = 30.sp)
            }
            Text("添加于$time")
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button({
                    showEdit = true
                }) {
                    Text("编辑")
                }
                Spacer(modifier = Modifier.width(10.dp))
                Button({
                    scope.launch(Dispatchers.IO) {
                        machine.sendMagicPacket()
                        snackbar.showSnackbar("已发送")
                    }
                }) {
                    Text("启动")
                }
            }
        }

        if (showEdit) {
            ModalBottomSheet(
                sheetState = modalBottomSheetState,
                modifier = Modifier.fillMaxWidth(),
                onDismissRequest = {
                    scope.launch {
                        showEdit = false
                    }
                }
            ) {
                EditMachine(machine) { showEdit = false }
            }
        }
        LaunchedEffect(Unit) {
            while (true) {
                val (formatted, delay) = formatTime(machine.time)
                time = formatted
                delay(delay)
            }
        }
        LaunchedEffect(Unit) {
            while (true) {
                state = LocalVm.machineState[machine.id] ?: MachineState.UNKNOWN
                delay(1000)
            }
        }
    }
}

internal val displayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M月d日")

/**
 * 格式化时间
 *
 * @return 格式化后的时间和下次刷新的时间间隔, 单位ms
 */
internal fun formatTime(time: Long): Pair<String, Long> {
    val now = System.currentTimeMillis()
    val diff = now - time
    return when {
        diff < 60 * 1000 -> "${(diff / 1000)}秒前" to 1000
        diff < 60 * 60 * 1000 -> "${(diff / (60 * 1000))}分钟前" to 60 * 1000
        diff < 24 * 60 * 60 * 1000 -> "${(diff / (60 * 60 * 1000))}小时前" to 60 * 60 * 1000
        diff < 7 * 24 * 60 * 60 * 1000 -> "${(diff / (24 * 60 * 60 * 1000))}天前" to 24 * 60 * 60 * 1000
        else -> Instant.ofEpochSecond(time)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .let(displayFormatter::format) to 24 * 60 * 60 * 1000
    }
}