package top.e404.mywol.ui.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.russhwolf.settings.set
import top.e404.mywol.getSettings
import top.e404.mywol.util.valueWithError
import top.e404.mywol.vm.RemoteVm

@Composable
fun Remote() {
    val settings = remember { getSettings("remote") }
    var clientName by remember { mutableStateOf(settings.getString("clientName", "")) }
    var serverUrl by remember { mutableStateOf(settings.getString("serverUrl", "")) }

    if (serverUrl.isBlank()) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            InputUrl(clientName, serverUrl) { setName, setUrl ->
                settings["serverUrl"] = setUrl
                serverUrl = setUrl
                settings["clientName"] = setName
                clientName = setName
            }
        }
        return
    }
    RemoteView()
}

private val URL_REGEX = Regex("(?i)https?://" +
        "(?<host>localhost|(\\d{1,3}.){3}\\d{1,3}|[\\da-z]{2,}(\\.[\\da-z]{2,}){1,2})" +
        "(?<port>:\\d{1,5})?" +
        "(?<path>/.*)?")

@Composable
private fun InputUrl(
    defaultName: String,
    defaultUrl: String,
    onConfirm: (clientName: String, serverUrl: String) -> Unit
) {
    var url by remember { mutableStateOf(defaultUrl) }
    var urlError by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf(defaultName) }
    var nameError by remember { mutableStateOf<String?>(null) }
    OutlinedTextField(
        value = url,
        onValueChange = {
            nameError = if (it.isNotBlank()) null else "不可为空"
            name = it
        },
        label = { Text(valueWithError("请输入客户端名字", nameError)) },
        isError = nameError != null,
        modifier = Modifier.fillMaxWidth().padding(10.dp)
    )
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(
        value = url,
        onValueChange = {
            urlError = if (it matches URL_REGEX) null else "格式错误"
            url = it
        },
        label = { Text(valueWithError("请输入服务器BaseUrl", urlError)) },
        isError = urlError != null,
        placeholder = { TextFieldValue("http://localhost:8080") },
        modifier = Modifier.fillMaxWidth().padding(10.dp)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text("输入服务器的BaseUrl以使用远程功能")
    Button(onClick = {
        var error = false
        if (!url.matches(URL_REGEX)) {
            urlError = "地址格式错误"
            error = true
        }
        if (name.isBlank()) {
            nameError = "不可为空"
            error = true
        }
        if (!error) onConfirm(name, url)
        return@Button
    }) {
        Text("确定")
    }
}

@Composable
private fun RemoteView() {
    val remoteVm = RemoteVm
    LaunchedEffect(Unit) {
        remoteVm.initialize()
    }
    if (remoteVm.initializing) {
        Text("加载中")
        return
    }
}