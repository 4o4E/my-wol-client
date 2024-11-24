package top.e404.mywol.vm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.russhwolf.settings.set
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import top.e404.mywol.dao.WolDatabase
import top.e404.mywol.model.MachineState
import top.e404.mywol.model.WolClient
import top.e404.mywol.model.WolMachine
import top.e404.mywol.model.WsC2sData
import top.e404.mywol.model.WsS2cData
import top.e404.mywol.model.WsSyncC2s
import top.e404.mywol.model.WsSyncS2c
import top.e404.mywol.model.WsWolC2s
import top.e404.mywol.model.WsWolS2c
import top.e404.mywol.sendMagicPacket
import top.e404.mywol.util.debug
import top.e404.mywol.util.logger
import top.e404.mywol.util.warn
import java.util.UUID
import kotlin.coroutines.CoroutineContext

object RemoteVm : ViewModel(), KoinComponent {
    private val log = logger(RemoteVm::class)
    private val remoteSettings by lazy { SettingsVm.remote }
    private val db: WolDatabase by inject()
    private val machineDao inline get() = db.machineDao

    var initializing by mutableStateOf(true)
        private set
    var websocketState by mutableStateOf(WsState.CONNECTING)
    private val id by lazy {
        remoteSettings.getStringOrNull("id")
            ?: UUID.randomUUID().toString()
                .also { remoteSettings["id"] = it }
    }
    val clients = mutableStateOf(listOf<WolClient>())

    lateinit var clientName: String
        private set
    lateinit var serverAddress: String
        private set
    lateinit var serverSecret: String
        private set

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val httpClient = HttpClient(OkHttp) {
        install(WebSockets) {
            pingInterval = 10_000
        }
        install(ContentNegotiation) {
            json(json)
        }
    }

    private var sendPacket: suspend (packet: WsC2sData) -> Unit = {}
    var closeWebsocket: () -> Unit = {}
        private set

    val connectScope = CoroutineScope(SupervisorJob() + CoroutineExceptionHandler { _, t ->
        log.warn(t) { "uncaught exception in connectScope: " }
    })

    private fun setCtx(ctx: CoroutineContext) {
        websocketScope = CoroutineScope(ctx)
    }

    private val wsContext = (SupervisorJob() + CoroutineExceptionHandler { ctx, t ->
        log.debug { "websocketState: $websocketState" }
        websocketScope.launch(Dispatchers.IO) {
            UiVm.showSnackbar("websocket连接异常, 将在3秒后重连")
            delay(3000)
            connectWebsocket()
        }
        log.warn("uncaught exception in websocket: ", t)
    }).also { setCtx(it) }
    private lateinit var websocketScope: CoroutineScope

    private suspend fun connectWebsocket() {
        log.debug("初始化服务器连接")
        websocket()
        UiVm.showSnackbar("初始化服务器连接完成")
        log.debug("初始化ws连接完成")
    }

    fun initialize() {
        log.debug { "initialize: $initializing" }
        if (!initializing) return
        val existsId = remoteSettings.getStringOrNull("id")
        val id = existsId ?: UUID.randomUUID().toString()
        if (existsId == null) {
            remoteSettings["id"] = id
        }
        clientName = remoteSettings.getString("clientName", "")
        serverAddress = remoteSettings.getString("serverAddress", "")
        serverSecret = remoteSettings.getString("serverSecret", "")
        websocketScope.launch {
            connectWebsocket()
        }
    }

    suspend fun syncMachines() {
        val list = withContext(Dispatchers.IO) { LocalVm.listNormal() }
        sendPacket(WsSyncC2s(list.map { machine ->
            WolMachine(
                machine.id,
                machine.name,
                machine.mac,
                machine.deviceIp,
                machine.broadcastIp,
                LocalVm.machineState[machine.id] ?: MachineState.OFF
            )
        }))
    }

    private suspend fun websocket() {
        val url = "ws://$serverAddress/ws"
        log.debug { "启动websocket连接: $url" }
        httpClient.webSocket(url, {
            header("id", id)
            header("name", clientName)
        }) {
            initializing = false
            websocketState = WsState.OPEN
            sendPacket = {
                val json = json.encodeToString(WsC2sData.serializer(), it)
                log.debug("发送数据包: {}", json)
                outgoing.send(Frame.Text(json))
            }
            closeWebsocket = {
                connectScope.launch(Dispatchers.IO) { close() }
            }

            connectScope.launch(Dispatchers.IO) {
                val reason = closeReason.await()!!
                websocketState = WsState.RECONNECTING
                if (initializing) return@launch
                log.debug { "websocket连接关闭, 尝试重连: ${reason.message}" }
                connectScope.launch(Dispatchers.IO) {
                    // 重连延迟
                    delay(3000)
                    withContext(wsContext) { websocket() }
                }
            }
            syncMachines()
            while (true) {
                val s2c = incoming.receive() as Frame.Text
                launch(Dispatchers.IO) {
                    val jsonText = s2c.data.toString(Charsets.UTF_8)
                    log.debug("处理服务器数据包: {}", jsonText)
                    try {
                        val s2cData = json.decodeFromString(WsS2cData.serializer(), jsonText)
                        receivePacket(s2cData)
                    } catch (e: Exception) {
                        log.warn("处理服务器数据包失败: {}", jsonText, e)
                    }
                }
            }
        }
    }

    private suspend fun receivePacket(s2cData: WsS2cData) {
        when (s2cData) {
            is WsWolS2c -> {
                val machine = machineDao.getById(s2cData.machineId)
                if (machine == null) {
                    val packet = WsWolC2s(false, "没有该机器", s2cData.id)
                    sendPacket(packet)
                    return
                }
                UiVm.showSnackbar("正在唤醒${machine.name}")
                sendMagicPacket(machine.deviceIp, machine.mac)
                val packet = WsWolC2s(true, "", s2cData.id)
                sendPacket(packet)
            }

            is WsSyncS2c -> {
                log.debug { "同步远程客户端信息: ${s2cData.clients}" }
                clients.value = s2cData.clients
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        httpClient.close()
    }

    suspend fun sendWolReq(clientId: String, machineId: String): String? {
        log.debug { "发送请求" }
        val response = try {
            httpClient.post("http://$serverAddress/wol") {
                contentType(ContentType.Application.Json)
                setBody(WolReq(clientId, machineId))
            }
        } catch (e: Exception) {
            log.warn("发送wol请求失败: ", e)
            return e.message
        }
        log.debug { "接收响应" }
        return if (response.status == HttpStatusCode.OK) null else "${response.status.value}: ${response.bodyAsText()}"
    }
}

enum class WsState(val display: String) {
    CONNECTING("连接中..."),
    OPEN("已连接"),
    RECONNECTING("重连中..."),
}

@Serializable
data class WolReq(
    val clientId: String,
    val machineId: String
)