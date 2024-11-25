package top.e404.mywol.vm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.russhwolf.settings.set
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import top.e404.mywol.model.WolClient
import top.e404.mywol.platform.WebsocketService
import top.e404.mywol.platform.sendWolReq
import top.e404.mywol.platform.syncMachines
import top.e404.mywol.util.logger
import java.util.UUID

object RemoteVm : KoinComponent {
    private val log = logger()
    private val remoteSettings by lazy { SettingsVm.remote }

    var initializing by mutableStateOf(true)
    val _state = mutableStateOf(WsState.INITIALIZING)
    var state by _state
    private val id by lazy {
        remoteSettings.getStringOrNull("id")
            ?: UUID.randomUUID().toString()
                .also { remoteSettings["id"] = it }
    }
    lateinit var clientName: String private set
    lateinit var serverAddress: String private set
    lateinit var serverSecret: String private set

    val clients = mutableStateOf(listOf<WolClient>())

    fun close() {
        WebsocketService.stop()
    }

    fun startWebsocket() {
        clientName = remoteSettings.getString("clientName", "")
        serverAddress = remoteSettings.getString("serverAddress", "")
        serverSecret = remoteSettings.getString("serverSecret", "")
        log.debug("初始化服务器连接")
        WebsocketService.start(serverAddress, id, clientName, serverSecret)
    }

    suspend fun syncMachines() = WebsocketService.syncMachines()

    suspend fun sendWolReq(
        clientId: String,
        machineId: String
    ) = WebsocketService.sendWolReq(clientId, machineId)
}

enum class WsState(val display: String) {
    INITIALIZING("初始化中..."),
    CONNECTING("连接中..."),
    OPEN("已连接"),
    RECONNECTING("重连中..."),
}

@Serializable
data class WolReq(
    val clientId: String,
    val machineId: String
)