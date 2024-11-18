package top.e404.mywol.vm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.russhwolf.settings.set
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.header
import io.ktor.websocket.Frame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import top.e404.mywol.dao.WolDatabase
import top.e404.mywol.getSettings
import top.e404.mywol.model.MachineState
import top.e404.mywol.model.WolClient
import top.e404.mywol.model.WsC2sData
import top.e404.mywol.model.WsS2cData
import top.e404.mywol.model.WsSyncMachineState
import top.e404.mywol.model.WsSyncS2c
import top.e404.mywol.model.WsWolReq
import top.e404.mywol.model.WsWolResp
import top.e404.mywol.sendMagicPacket
import java.util.UUID

object RemoteVm : ViewModel(), KoinComponent {
    var serverUrl: String = ""
    private val settings = getSettings("remote")
    private val db: WolDatabase by inject()
    private val machineDao inline get() = db.machineDao
    private val localVm = LocalVm
    var initializing by mutableStateOf(true)
    private lateinit var id: String
    var clients by mutableStateOf(listOf<WolClient>())

    private val httpClient = HttpClient(OkHttp) {
        install(WebSockets) {
            pingIntervalMillis = 10_000
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private var sendPacket: suspend (packet: WsC2sData) -> Unit = {}

    fun initialize() {
        val existsId = settings.getStringOrNull("id")
        val id = existsId ?: UUID.randomUUID().toString()
        if (existsId == null) {
            settings["id"] = id
        }
        viewModelScope.launch(Dispatchers.IO) { ws() }
        initializing = true
    }

    private suspend fun ws() {
        httpClient.webSocket("$serverUrl/ws", {
            header("id", id)
        }) {
            sendPacket = {
                val json = json.encodeToString(WsC2sData.serializer(), it)
                outgoing.send(Frame.Text(json))
            }
            val list = withContext(Dispatchers.IO) { localVm.listNormal() }
            sendPacket(WsSyncMachineState(list.associate { m ->
                m.id to (LocalVm.machineState[m.id] ?: MachineState.OFF)
            }))
            while (true) {
                val s2c = incoming.receive() as Frame.Text
                launch(Dispatchers.IO) {
                    val s2cData = json.decodeFromString(
                        WsS2cData.serializer(),
                        s2c.data.toString(Charsets.UTF_8)
                    )
                    receivePacket(this@webSocket, s2cData)
                }
            }
        }
    }

    fun onMachineStateChange(changed: Map<String, MachineState>) {
        viewModelScope.launch {
            sendPacket(WsSyncMachineState(changed))
        }
    }

    private suspend fun receivePacket(session: DefaultClientWebSocketSession, s2cData: WsS2cData) {
        when (s2cData) {
            is WsWolReq -> {
                val machine = machineDao.getById(s2cData.machineId)
                if (machine == null) {
                    val packet = WsWolResp(
                        s2cData.id,
                        false,
                        "没有该机器"
                    )
                    session.send(Frame.Text(json.encodeToString(WsWolResp.serializer(), packet)))
                    return
                }
                sendMagicPacket(machine.deviceIp, machine.mac)
                val packet = WsWolResp(
                    s2cData.id,
                    true,
                    ""
                )
                session.send(Frame.Text(json.encodeToString(WsWolResp.serializer(), packet)))
            }

            is WsSyncS2c -> TODO()
            is WsWolResp -> TODO()
        }
    }

    override fun onCleared() {
        super.onCleared()
        httpClient.close()
    }
}
