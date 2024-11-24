package top.e404.mywol.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material3.Text
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
import top.e404.mywol.dao.Machine
import top.e404.mywol.model.MachineState
import top.e404.mywol.model.WolClient
import top.e404.mywol.model.WolMachine
import top.e404.mywol.vm.LocalVm
import top.e404.mywol.vm.RemoteVm
import top.e404.mywol.vm.UiVm
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun LocalMachineItem(machine: Machine) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(5.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
    ) {
        val scope = rememberCoroutineScope()
        var editMachine by remember { UiVm.editMachine }
        var time by remember { mutableStateOf("") }
        var state by remember { mutableStateOf(MachineState.UNKNOWN) }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(15.dp)
        ) {
            Row {
                StateIcon(state)
                Spacer(Modifier.width(10.dp))
                Text(machine.name, Modifier, fontSize = 20.sp)
            }
            Text("添加于$time")
            if (UiVm.debug) Text(machine.id)
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button({
                    editMachine = machine
                }) {
                    Text("编辑")
                }
                Spacer(modifier = Modifier.width(10.dp))
                Button({
                    scope.launch(Dispatchers.IO) {
                        machine.sendMagicPacket()
                        UiVm.globalSnackbarHostState.showSnackbar("已广播wol")
                    }
                }) {
                    Text("启动")
                }
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

@Composable
fun RemoteMachineItem(client: WolClient, machine: WolMachine) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(5.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(Modifier.fillMaxWidth().padding(15.dp)) {
            Row {
                StateIcon(machine.state)
                Spacer(Modifier.width(10.dp))
                Text(machine.name, fontSize = 20.sp)
            }
            if (UiVm.debug) Text(machine.id)
            Spacer(modifier = Modifier.height(10.dp))
            Button({
                RemoteVm.connectScope.launch {
                    UiVm.showSnackbar("广播中...")
                    val message = RemoteVm.sendWolReq(client.id, machine.id)
                    if (message == null) {
                        UiVm.showSnackbar("已广播wol")
                    } else {
                        UiVm.showSnackbar("发送失败: $message")
                    }
                }
            }) {
                Text("启动")
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

@Composable
internal fun RowScope.StateIcon(state: MachineState) {
    Icon(
        modifier = Modifier
            .align(Alignment.CenterVertically),
        imageVector = Icons.Filled.Circle,
        contentDescription = null,
        tint = when (state) {
            MachineState.ON -> Color.Green
            MachineState.OFF -> Color.Red
            MachineState.UNKNOWN -> Color.LightGray
        }
    )
}