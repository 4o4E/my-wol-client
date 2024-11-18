package top.e404.mywol.ui.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.e404.mywol.dao.Machine
import top.e404.mywol.util.valueWithError
import top.e404.mywol.vm.LocalVm
import java.util.UUID

private val IP_REGEX = Regex(
    "(?i)((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)(\\.(?!$)|$)){4}|" + // IPv4
            "([\\da-f]{1,4}:){7}[0-9a-f]{1,4}|" +                 // IPv6 (full form)
            "(([\\da-f]{1,4}:){1,7}:)|" +                         // IPv6 (:: shortcut)
            "(([\\da-f]{1,4}:){1,6}:[\\da-f]{1,4})|" +            // IPv6 (1 missing group)
            "(([\\da-f]{1,4}:){1,5}(:[\\da-f]{1,4}){1,2})|" +     // IPv6 (2 missing groups)
            "(([\\da-f]{1,4}:){1,4}(:[\\da-f]{1,4}){1,3})|" +     // IPv6 (3 missing groups)
            "(([\\da-f]{1,4}:){1,3}(:[\\da-f]{1,4}){1,4})|" +     // IPv6 (4 missing groups)
            "(([\\da-f]{1,4}:){1,2}(:[\\da-f]{1,4}){1,5})|" +     // IPv6 (5 missing groups)
            "([\\da-f]{1,4}:((:[\\da-f]{1,4}){1,6}))|" +          // IPv6 (6 missing groups)
            "(::((:[\\da-f]{1,4}){1,7}|:))"                       // IPv6 (:: alone))
)
private val MAC_REGEX = Regex("(?i)([\\da-z]{2}:){5}([\\da-z]{2})")

@Composable
fun EditMachine(
    machine: Machine? = null,
    done: CoroutineScope.() -> Unit
) {
    var name by remember {
        mutableStateOf(
            TextFieldValue(
                machine?.name ?: UUID.randomUUID().toString().substring(0, 4)
            )
        )
    }
    var nameError by remember { mutableStateOf<String?>(null) }
    var deviceIp by remember { mutableStateOf(TextFieldValue(machine?.deviceIp ?: "1.1.1.1")) }
    var deviceIpError by remember { mutableStateOf<String?>(null) }
    var broadcastIp by remember { mutableStateOf(TextFieldValue(machine?.deviceIp ?: "1.1.1.1")) }
    var broadcastIpError by remember { mutableStateOf<String?>(null) }
    var mac by remember { mutableStateOf(TextFieldValue(machine?.mac ?: "aa:aa:aa:aa:aa:aa")) }
    var macLast by remember { mutableStateOf(machine?.mac ?: "aa:aa:aa:aa:aa:aa") }
    var macError by remember { mutableStateOf<String?>(null) }

    val isAdd = machine == null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(30.dp, 30.dp)
            .imePadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val coroutineScope = rememberCoroutineScope()
        val keyboard = LocalSoftwareKeyboardController.current
        val focusRequester = remember { FocusRequester() }
        val scope = rememberCoroutineScope()
        val onDone: () -> Unit = onClick@{
            if (name.text.isEmpty()) {
                nameError = "请输入名字"
                return@onClick
            }
            if (deviceIp.text.isEmpty()) {
                deviceIpError = "请输入ip"
                return@onClick
            }
            if (!deviceIp.text.matches(IP_REGEX)) {
                return@onClick
            }
            if (broadcastIp.text.isEmpty()) {
                broadcastIpError = "请输入ip"
                return@onClick
            }
            if (!broadcastIp.text.matches(IP_REGEX)) {
                return@onClick
            }
            if (mac.text.isEmpty()) {
                macError = "请输入mac"
                return@onClick
            }
            if (!mac.text.matches(MAC_REGEX)) {
                return@onClick
            }
            coroutineScope.launch(Dispatchers.IO) {
                val new = Machine(
                    machine?.id ?: UUID.randomUUID().toString(),
                    name.text,
                    deviceIp.text,
                    broadcastIp.text,
                    mac.text,
                    System.currentTimeMillis()
                )
                if (isAdd) LocalVm.save(new)
                else LocalVm.update(new)
                withContext(Dispatchers.Main) {
                    keyboard?.hide()
                    done()
                }
            }
        }

        Text(
            text = if (isAdd) "添加机器" else "修改机器",
            modifier = Modifier.fillMaxWidth(),
            fontSize = 30.sp
        )
        Spacer(modifier = Modifier.height(30.dp))
        OutlinedTextField(
            label = {
                Text(text = valueWithError("名字", nameError))
            },
            modifier = Modifier.focusRequester(focusRequester),
            value = name,
            maxLines = 1,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            isError = nameError != null,
            onValueChange = {
                name = it
            }
        )
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedTextField(
            label = { Text(text = valueWithError("设备ip地址", deviceIpError)) },
            placeholder = { Text("xxx.xxx.xxx.xxx") },
            value = deviceIp,
            maxLines = 1,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Next
            ),
            isError = deviceIpError != null,
            onValueChange = {
                deviceIp = it
                deviceIpError = if (it.text.matches(IP_REGEX)) null else "无效ip"
            }
        )
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedTextField(
            label = { Text(text = valueWithError("广播ip地址", broadcastIpError)) },
            placeholder = { Text("xxx.xxx.xxx.xxx") },
            value = broadcastIp,
            maxLines = 1,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Next
            ),
            isError = deviceIpError != null,
            onValueChange = {
                broadcastIp = it
                broadcastIpError =
                    if (it.text.isBlank() || it.text.matches(IP_REGEX)) null
                    else "无效ip"
            }
        )
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedTextField(
            label = { Text(text = valueWithError("设备mac地址", macError)) },
            modifier = Modifier,
            value = mac,
            maxLines = 1,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            isError = macError != null,
            onValueChange = {
                mac = autoCompleteMac(it, macLast)
                macError = if (it.text.matches(MAC_REGEX)) null else "无效mac"
                macLast = mac.text
            }
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onDone) { Text(text = if (isAdd) "新增" else "保存") }
            if (!isAdd) {
                Spacer(modifier = Modifier.width(20.dp))
                Button({
                    scope.launch(Dispatchers.IO) {
                        LocalVm.remove(machine!!.id)
                    }
                }) { Text("删除") }
            }
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