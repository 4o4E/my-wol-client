package top.e404.mywol.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowDropUp
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.e404.mywol.dao.Machine
import top.e404.mywol.model.MachineState
import top.e404.mywol.model.SshHistory
import top.e404.mywol.model.WolClient
import top.e404.mywol.model.WolMachine
import top.e404.mywol.vm.LocalVm
import top.e404.mywol.vm.RemoteVm
import top.e404.mywol.vm.Result
import top.e404.mywol.vm.SshVm
import top.e404.mywol.vm.UiVm
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MachineItem(machine: MachineWrapper) {
    var showSsh by remember { mutableStateOf(false) }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    if (showDeleteConfirm) Dialog(
        onDismissRequest = { showDeleteConfirm = false },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = true
        )
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("删除后无法恢复, 确认删除?")
                Spacer(modifier = Modifier.height(20.dp))
                Row {
                    Button(onClick = {
                        UiVm.ioScope.launch {
                            LocalVm.remove(machine.id)
                            showDeleteConfirm = false
                        }
                    }) { Text("确认") }
                    Spacer(modifier = Modifier.width(20.dp))
                    FilledTonalButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
                }
            }
        }
    }
    var showShutdownConfirm by remember { mutableStateOf(false) }
    if (showShutdownConfirm) Dialog(
        onDismissRequest = { showShutdownConfirm = false },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = true
        )
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("确认关机?")
                Spacer(modifier = Modifier.height(20.dp))
                Row {
                    Button(onClick = {
                        UiVm.ioScope.launch {
                            val result = machine.shutdown()
                            UiVm.showSnackbar("已发送关机指令: $result")
                        }
                    }) { Text("确认") }
                    Spacer(modifier = Modifier.width(20.dp))
                    FilledTonalButton(onClick = { showShutdownConfirm = false }) { Text("取消") }
                }
            }
        }
    }
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
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 15.dp, top = 5.dp, end = 15.dp, bottom = 5.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StateIcon(machine.state)
                Spacer(Modifier.width(10.dp))
                Text(machine.name, fontSize = 24.sp)
                Spacer(Modifier.weight(1F))

                IconButton({
                    scope.launch(Dispatchers.IO) {
                        machine.wol()
                        UiVm.globalSnackbarHostState.showSnackbar("已广播wol")
                    }
                }) {
                    Icon(Icons.Filled.PowerSettingsNew, null)
                }
                Box {
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton({ showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, null)
                    }
                    DropdownMenu(
                        showMenu,
                        { showMenu = false },
                        Modifier.align(Alignment.TopEnd)
                    ) {
                        if (machine is LocalMachineWrapper) {
                            DropdownMenuItem(
                                text = { Text("编辑") },
                                onClick = {
                                    showMenu = false
                                    editMachine = machine.machine
                                },
                                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                                modifier = Modifier.align(Alignment.End)
                            )
                            DropdownMenuItem(
                                text = { Text("删除") },
                                onClick = {
                                    showMenu = false
                                    showDeleteConfirm = true
                                },
                                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                        if (machine.canShutdown) DropdownMenuItem(
                            text = { Text("关机") },
                            onClick = {
                                showMenu = false
                                showShutdownConfirm = true
                            },
                            leadingIcon = { Icon(Icons.Outlined.Power, contentDescription = null) },
                            modifier = Modifier.align(Alignment.End)
                        )
                        if (machine.isSshConfigured) DropdownMenuItem(
                            text = { Text("SSH") },
                            onClick = {
                                showMenu = false
                                showSsh = true
                            },
                            leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null) },
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("添加于$time")
                if (machine.isSshConfigured) {
                    Spacer(Modifier.weight(1F))
                    if (machine is LocalMachineWrapper) {
                        val handler = SshVm.handlers[machine.id]
                        if (handler != null && handler.isStart.value) {
                            val isStart by handler.isStart
                            if (isStart) IconButton({
                                handler.history.value = emptyList()
                                SshVm.close(machine.id)
                            }) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                    IconButton({ showSsh = !showSsh }) {
                        Icon(
                            if (showSsh) Icons.Outlined.ArrowDropUp
                            else Icons.Outlined.ArrowDropDown,
                            contentDescription = null
                        )
                    }
                }
            }
            if (UiVm.debug) Text(machine.id)
            if (showSsh) {
                val scrollState = rememberScrollState()
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(5.dp))
                ) {
                    LazyColumn(
                        Modifier
                            .height(300.dp)
                            .horizontalScroll(scrollState)
                            .padding(10.dp)
                    ) {
                        val histories by machine.sshHistories
                        items(histories.size) { index ->
                            val history = histories[index]
                            Text(
                                history.command,
                                color = Color(0xFFA8CD89),
                                fontSize = 12.sp,
                                lineHeight = TextUnit(12f, TextUnitType.Sp),
                                overflow = TextOverflow.Clip
                            )
                            Text(
                                history.result.removeSuffix("\n"),
                                fontSize = 12.sp,
                                lineHeight = TextUnit(12f, TextUnitType.Sp),
                                overflow = TextOverflow.Clip
                            )
                        }
                    }
                }
                var command by remember { mutableStateOf("") }
                Spacer(Modifier.height(10.dp))
                var sending by remember { mutableStateOf(false) }
                val onSend = onSend@{
                    if (command.isBlank()) return@onSend
                    sending = true
                    UiVm.ioScope.launch {
                        val result = machine.ssh(command)
                        if (result.success) {
                            machine.sshHistories.value += SshHistory(command, result.result)
                            command = ""
                        } else UiVm.showSnackbar("SSH执行失败: ${result.message}")
                        sending = false
                    }
                }
                OutlinedTextField(
                    command,
                    { command = it },
                    Modifier.fillMaxWidth(),
                    maxLines = 1,
                    enabled = !sending,
                    placeholder = { Text("输入并发送") },
                    trailingIcon = {
                        if (sending) CircularProgressIndicator(Modifier.size(24.dp))
                        else IconButton({ onSend() }) {
                            Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null)
                        }
                    },
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send)
                )
                Spacer(Modifier.height(10.dp))
            } else {
                Spacer(Modifier.height(5.dp))
            }
        }

        LaunchedEffect(Unit) {
            while (true) {
                val (formatted, delay) = formatTime(machine.time)
                time = formatted
                delay(delay)
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
            MachineState.ON -> Color(0xFFA8CD89)
            MachineState.OFF -> Color(0xFFF9C0AB)
            MachineState.UNKNOWN -> Color.LightGray
        }
    )
}

interface MachineWrapper {
    val id: String
    val name: String
    val time: Long
    val state: MachineState
    val isSshConfigured: Boolean
    val canShutdown: Boolean
    val sshHistories: MutableState<List<SshHistory>>

    suspend fun wol(): Result<Unit>
    suspend fun ssh(command: String): Result<String>
    suspend fun shutdown(): Result<String>
}

data class LocalMachineWrapper(val machine: Machine) : MachineWrapper {
    override val id get() = machine.id
    override val name get() = machine.name
    override val time get() = machine.time
    override val state get() = LocalVm.machineState[machine.id] ?: MachineState.UNKNOWN
    override val isSshConfigured get() = machine.sshUsername.isNotBlank()
    override val canShutdown get() = machine.sshShutdownCommand.isNotBlank()
    override val sshHistories
        get() = SshVm.getOrCreate(machine).history

    override suspend fun wol(): Result<Unit> {
        machine.sendMagicPacket()
        return Result.success(Unit)
    }

    override suspend fun ssh(command: String) = SshVm.getOrCreate(machine).apply {
        start()
    }.exec(command)

    override suspend fun shutdown() = SshVm.getOrCreate(machine).apply { start() }.use {
        it.exec(machine.sshShutdownCommand)
    }
}

data class RemoteMachineWrapper(val client: WolClient, val machine: WolMachine) : MachineWrapper {
    override val id get() = machine.id
    override val name get() = machine.name
    override val time get() = machine.time
    override val state get() = machine.state
    override val isSshConfigured get() = machine.isSshConfigured
    override val canShutdown get() = machine.canShutdown
    override val sshHistories: MutableState<List<SshHistory>>
        get() = RemoteVm.sshHistories.getOrPut(machine.id) { mutableStateOf(emptyList()) }

    override suspend fun wol() = RemoteVm.sendWolReq(client.id, machine.id)
    override suspend fun ssh(command: String) = RemoteVm.sendSshReq(client.id, machine.id, command)
    override suspend fun shutdown() = RemoteVm.sendSshShutdown(client.id, machine.id)
}