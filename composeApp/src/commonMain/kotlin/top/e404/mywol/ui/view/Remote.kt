package top.e404.mywol.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.russhwolf.settings.set
import top.e404.mywol.ui.components.ClientItem
import top.e404.mywol.util.valueWithError
import top.e404.mywol.vm.RemoteVm
import top.e404.mywol.vm.SettingsVm
import top.e404.mywol.vm.WsState

@Composable
fun Remote() {
    val settings = SettingsVm.remote
    var clientName by remember { mutableStateOf(settings.getString("clientName", "")) }
    var serverAddress by remember { mutableStateOf(settings.getString("serverAddress", "")) }
    var serverSecret by remember { mutableStateOf(settings.getString("serverSecret", "")) }

    if (serverAddress.isBlank()) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            InputUrl(clientName, serverAddress, serverSecret) { setName, setUrl, setSecret ->
                settings["serverAddress"] = setUrl
                serverAddress = setUrl
                settings["clientName"] = setName
                clientName = setName
                settings["serverSecret"] = setSecret
                serverSecret = setSecret
            }
        }
        return
    }
    RemoteClientsView()
}

private val URL_REGEX = Regex(
    "(?i)" +
            "(?<host>localhost|(\\d{1,3}.){3}\\d{1,3}|[\\da-z]{2,}(\\.[\\da-z]{2,}){1,2})" +
            "(?<port>:\\d{1,5})?" +
            "(?<path>/.*)?"
)

@Composable
private fun InputUrl(
    defaultName: String,
    defaultUrl: String,
    serverSecret: String,
    onConfirm: (clientName: String, serverAddress: String, setSecret: String) -> Unit
) {
    var url by remember { mutableStateOf(defaultUrl) }
    var urlError by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf(defaultName) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var secret by remember { mutableStateOf(serverSecret) }

    val onClick = {
        var error = false
        if (!url.matches(URL_REGEX)) {
            urlError = "地址格式错误"
            error = true
        }
        if (name.isBlank()) {
            nameError = "不可为空"
            error = true
        }
        if (!error) onConfirm(name, url, secret)
    }

    OutlinedTextField(
        value = name,
        onValueChange = {
            nameError = if (it.isNotBlank()) null else "不可为空"
            name = it
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
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
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        label = { Text(valueWithError("请输入服务器地址", urlError)) },
        isError = urlError != null,
        placeholder = { Text("localhost:8080") },
        modifier = Modifier.fillMaxWidth().padding(10.dp)
    )
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(
        value = secret,
        onValueChange = {
            secret = it
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { onClick() }),
        label = { Text("请输入服务器密钥") },
        modifier = Modifier.fillMaxWidth().padding(10.dp)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text("输入服务器地址以使用远程功能")
    Spacer(modifier = Modifier.height(16.dp))
    Button(onClick = onClick) {
        Text("确定")
    }
}

/**
 * 远程客户端列表
 */
@Composable
private fun RemoteClientsView() {
    LaunchedEffect(Unit) {
        RemoteVm.startWebsocket()
    }
    if (RemoteVm.state != WsState.OPEN) {
        Text(RemoteVm.state.display)
        return
    }
    val clients by remember { RemoteVm.clients }
    Column(
        Modifier.fillMaxSize().padding(10.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(5.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(Modifier.fillMaxWidth().padding(15.dp)) {
                Text("服务器已连接${RemoteVm.serverAddress}")
            }
        }
        Spacer(Modifier.height(10.dp))
        for (client in clients) ClientItem(client)
    }
}