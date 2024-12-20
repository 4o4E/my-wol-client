package top.e404.mywol.ui.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.e404.mywol.dao.Machine
import top.e404.mywol.dao.SshSecretType
import top.e404.mywol.dao.validate
import top.e404.mywol.dao.validateMachine
import top.e404.mywol.util.valueWithError
import top.e404.mywol.vm.LocalVm
import top.e404.mywol.vm.RemoteVm
import top.e404.mywol.vm.UiVm
import java.util.UUID

@Composable
fun EditMachine() {
    var machine by remember { UiVm.editMachine }

    // wol info
    var name by remember { mutableStateOf(TextFieldValue(machine?.name ?: "")) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var deviceHost by remember { mutableStateOf(TextFieldValue(machine?.deviceHost ?: "")) }
    var deviceHostError by remember { mutableStateOf<String?>(null) }
    var wolHost by remember { mutableStateOf(TextFieldValue(machine?.wolHost ?: "")) }
    var wolHostError by remember { mutableStateOf<String?>(null) }
    var wolPort by remember { mutableStateOf(TextFieldValue(machine?.wolPort?.toString() ?: "")) }
    var wolPortError by remember { mutableStateOf<String?>(null) }
    var mac by remember { mutableStateOf(TextFieldValue(machine?.mac ?: "")) }
    var macLast by remember { mutableStateOf(machine?.mac ?: "") }
    var macError by remember { mutableStateOf<String?>(null) }
    // ssh info
    var sshPort by remember { mutableStateOf(TextFieldValue(machine?.sshPort?.toString() ?: "")) }
    var sshPortError by remember { mutableStateOf<String?>(null) }
    var sshUsername by remember { mutableStateOf(TextFieldValue(machine?.sshUsername ?: "")) }
    var sshUsernameError by remember { mutableStateOf<String?>(null) }
    var sshSecretType by remember { mutableStateOf(machine?.sshSecretType ?: SshSecretType.PASSWORD) }
    var sshSecretValue by remember { mutableStateOf(TextFieldValue(machine?.sshSecretValue ?: "")) }
    var sshSecretValueError by remember { mutableStateOf<String?>(null) }
    var sshCharset by remember { mutableStateOf(TextFieldValue(machine?.sshCharset ?: "")) }
    var sshCharsetError by remember { mutableStateOf<String?>(null) }
    var sshShutdownCommand by remember { mutableStateOf(TextFieldValue(machine?.sshShutdownCommand ?: "")) }

    fun isSshEnabled() = sshPort.text.isNotBlank()
            || sshUsername.text.isNotBlank()
            || sshSecretValue.text.isNotBlank()
            || sshCharset.text.isNotBlank()

    val isAdd = machine == null

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val coroutineScope = rememberCoroutineScope()
        val keyboard = LocalSoftwareKeyboardController.current
        val focusRequester = remember { FocusRequester() }
        val scope = rememberCoroutineScope()
        val onDone: () -> Unit = onClick@{
            var hasError = false
            fun check(error: String?, block: (String) -> Unit) {
                if (error != null) {
                    block(error)
                    hasError = true
                }
            }

            validateMachine {
                check(name.text.validateNotBlank()) { nameError = it }
                check(deviceHost.text.validateHost()) { deviceHostError = it }
                check(wolHost.text.validateHost(true)) { wolHostError = it }
                check(wolPort.text.validatePort()) { wolPortError = it }
                check(mac.text.validateMac()) { macError = it }
                if (hasError) return@onClick
                if (isSshEnabled()) {
                    check(sshPort.text.validatePort()) { sshPortError = it }
                    check(sshUsername.text.validateNotBlank()) { sshUsernameError = it }
                    check(sshSecretValue.text.validateNotBlank()) { sshSecretValueError = it }
                    check(sshCharset.text.validateCharset()) { sshCharsetError = it }
                }
                if (hasError) return@onClick
            }

            coroutineScope.launch(Dispatchers.IO) {
                val new = Machine(
                    machine?.id ?: UUID.randomUUID().toString(),
                    name.text,
                    deviceHost.text,
                    wolHost.text,
                    wolPort.text.toInt(),
                    mac.text,

                    sshPort.text.toIntOrNull() ?: 0,
                    sshUsername.text,
                    sshSecretType,
                    sshSecretValue.text,
                    sshCharset.text,
                    sshShutdownCommand.text,

                    System.currentTimeMillis()
                )
                if (isAdd) LocalVm.save(new)
                else LocalVm.update(new)
                withContext(Dispatchers.Main) {
                    keyboard?.hide()
                    if (isAdd) UiVm.showAdd.value = false
                    else machine = null
                    UiVm.showSnackbar("已${if (isAdd) "添加" else "保存"}")
                }
            }
        }

        // title
        Text(
            text = if (isAdd) "添加机器" else "修改机器",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            fontSize = 25.sp
        )
        Spacer(Modifier.height(20.dp))

        // wol info
        OutlinedTextField(
            label = { Text(text = valueWithError("名字", nameError)) },
            modifier = Modifier.focusRequester(focusRequester),
            value = name,
            maxLines = 1,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            isError = nameError != null,
            onValueChange = {
                name = it
                nameError = validate { it.text.validateNotBlank() }
            }
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            label = { Text(text = valueWithError("设备ip地址", deviceHostError)) },
            placeholder = { Text("xxx.xxx.xxx.xxx") },
            value = deviceHost,
            maxLines = 1,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Next
            ),
            isError = deviceHostError != null,
            onValueChange = {
                deviceHost = it
                deviceHostError = validate { it.text.validateHost() }
            }
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            label = { Text(text = valueWithError("广播地址(可选)", wolHostError)) },
            placeholder = { Text("xxx.xxx.xxx.xxx") },
            value = wolHost,
            maxLines = 1,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Next
            ),
            isError = wolHostError != null,
            onValueChange = {
                wolHost = it
                wolHostError = validate { it.text.validateHost(true) }
            }
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            label = { Text(text = valueWithError("广播端口", wolPortError)) },
            placeholder = { Text("9") },
            value = wolPort,
            maxLines = 1,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            isError = wolPortError != null,
            onValueChange = {
                wolPort = it
                wolPortError = validate { it.text.validatePort() }
            }
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            label = { Text(text = valueWithError("设备mac地址", macError)) },
            modifier = Modifier,
            value = mac,
            maxLines = 1,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            isError = macError != null,
            onValueChange = {
                mac = autoCompleteMac(it, macLast)
                macError = validate { it.text.validateMac() }
                macLast = mac.text
            }
        )

        Spacer(Modifier.height(20.dp))
        Text(
            text = "SSH",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            fontSize = 22.sp
        )
        Spacer(Modifier.height(20.dp))

        // ssh info
        SingleChoiceSegmentedButtonRow {
            val types = SshSecretType.entries
            for ((index, type) in types.withIndex()) {
                SegmentedButton(
                    sshSecretType == type,
                    { sshSecretType = type },
                    SegmentedButtonDefaults.itemShape(index = index, count = types.size)
                ) { Text(type.display) }
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            label = { Text(text = valueWithError("ssh端口", sshPortError)) },
            modifier = Modifier,
            value = sshPort,
            maxLines = 1,
            placeholder = { Text("22") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            isError = sshPortError != null,
            onValueChange = {
                sshPort = it
                sshPortError = validate { it.text.validatePort() }
            }
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            label = { Text(text = valueWithError("ssh用户名", sshUsernameError)) },
            modifier = Modifier,
            value = sshUsername,
            maxLines = 1,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Next
            ),
            isError = sshUsernameError != null,
            onValueChange = {
                sshUsername = it
                sshUsernameError = validate { it.text.validateNotBlank() }
            }
        )
        Spacer(Modifier.height(10.dp))
        val transformation = remember { PasswordVisualTransformation() }
        var showSecret by remember { mutableStateOf(false) }
        OutlinedTextField(
            label = { Text(text = valueWithError("ssh${sshSecretType.display}", sshSecretValueError)) },
            modifier = Modifier,
            value = sshSecretValue,
            visualTransformation = if (showSecret) VisualTransformation.None else transformation,
            maxLines = if (sshSecretType == SshSecretType.PASSWORD) 1 else Int.MAX_VALUE,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Next
            ),
            trailingIcon = {
                IconButton({ showSecret = !showSecret }) {
                    Icon(
                        if (showSecret) Icons.Outlined.VisibilityOff
                        else Icons.Outlined.Visibility,
                        contentDescription = null
                    )
                }
            },
            isError = sshSecretValueError != null,
            onValueChange = {
                sshSecretValue = it
                sshSecretValueError = validate { it.text.validateNotBlank() }
            }
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            label = { Text(text = valueWithError("ssh字符集", sshCharsetError)) },
            modifier = Modifier,
            value = sshCharset,
            maxLines = 1,
            placeholder = { Text("UTF8 / GBK") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            isError = sshCharsetError != null,
            onValueChange = {
                sshCharset = it
                sshCharsetError = validate { it.text.validateCharset() }
            }
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            label = { Text(text = "ssh关机指令") },
            modifier = Modifier,
            value = sshShutdownCommand,
            maxLines = 1,
            placeholder = { Text("poweroff / shutdown /p") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            onValueChange = { sshShutdownCommand = it }
        )
        Spacer(Modifier.height(10.dp))

        // 操作按钮
        Row(
            Modifier.fillMaxWidth(),
            Arrangement.Center,
            Alignment.CenterVertically
        ) {
            Button(onClick = onDone) { Text(text = if (isAdd) "新增" else "保存") }
            if (!isAdd) {
                Spacer(modifier = Modifier.width(20.dp))
                Button({
                    scope.launch(Dispatchers.IO) {
                        LocalVm.remove(machine!!.id)
                        RemoteVm.syncMachines()
                        machine = null
                    }
                }) { Text("删除") }
            }
            Spacer(modifier = Modifier.width(20.dp))
            Button({
                scope.launch(Dispatchers.IO) {
                    if (!isAdd) machine = null
                    else UiVm.showAdd.value = false
                }
            }) { Text("取消") }
        }

        LaunchedEffect(true) {
            keyboard?.show()
            focusRequester.requestFocus()
        }
    }
}

private val AUTO_COMPLETE_REGEX = Regex("(?i)([\\da-z]{2}:){0,4}([\\da-z]{2})")

private fun autoCompleteMac(input: TextFieldValue, macLast: String): TextFieldValue {
    return if (input.text.matches(AUTO_COMPLETE_REGEX)
        && input.selection.run { start == end && end == max }
        && macLast.last() != ':'
    ) {
        val text = "${input.text}:"
        TextFieldValue(text, selection = TextRange(text.length))
    } else input
}