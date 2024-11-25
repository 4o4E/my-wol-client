package top.e404.mywol.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import kotlinx.serialization.json.Json
import top.e404.mywol.model.MachineState
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
import top.e404.mywol.vm.LocalVm
import top.e404.mywol.vm.RemoteVm
import top.e404.mywol.vm.UiVm
import top.e404.mywol.vm.WolReq
import top.e404.mywol.vm.WsState
import kotlin.coroutines.CoroutineContext

/**
 * 和远程服务器进行数据交互的服务
 */
expect class WebsocketService {
    companion object {
        var instance: WebsocketService?

        /**
         * 启动websocket连接服务
         * @return 是否成功启动, 如果已经启动则返回false
         */
        fun start(address: String, id: String, name: String, secret: String?): Boolean
        fun stop()
    }

    var handler: WebsocketHandler?
        private set
}

suspend fun WebsocketService.Companion.syncMachines() {
    instance?.handler?.syncMachines()
}

suspend fun WebsocketService.Companion.sendWolReq(
    clientId: String,
    machineId: String
) = instance!!
    .handler!!
    .sendWolReq(clientId, machineId)

class WebsocketHandler(
    val id: String,
    val name: String,
    val serverAddress: String,
) {
    private val log = logger()
    private val wsUrl = "ws://${serverAddress}/ws"

    private var state by RemoteVm._state

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
        log.debug { "websocketState: $state" }
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
        log.debug("初始化服务器连接完成")
    }

    fun start() {
        if (state != WsState.INITIALIZING) {
            log.warn { "ws处理器已启动, state: $state" }
            return
        }
        log.debug { "启动ws处理器" }
        state = WsState.CONNECTING
        websocketScope.launch {
            connectWebsocket()
        }
    }

    /**
     * 上传所有本机的设备信息
     */
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

    /**
     * 启动websocket连接
     */
    private suspend fun websocket() {
        log.debug { "启动websocket连接: $wsUrl" }
        httpClient.webSocket(wsUrl, {
            header("id", id)
            header("name", name)
        }) {
            state = WsState.OPEN
            UiVm.showSnackbar("服务器已连接")
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
                if (state == WsState.INITIALIZING) return@launch
                state = WsState.RECONNECTING
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

    /**
     * 处理websocket接收到的数据包
     */
    private suspend fun receivePacket(s2cData: WsS2cData) {
        when (s2cData) {
            is WsWolS2c -> {
                val machine = LocalVm.getById(s2cData.machineId)
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
                RemoteVm.clients.value = s2cData.clients
            }
        }
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