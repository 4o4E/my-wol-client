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
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import top.e404.mywol.model.MachineState
import top.e404.mywol.model.WolMachine
import top.e404.mywol.model.WsC2sData
import top.e404.mywol.model.WsS2cData
import top.e404.mywol.model.WsSshC2s
import top.e404.mywol.model.WsSshHistoryC2s
import top.e404.mywol.model.WsSshHistoryS2c
import top.e404.mywol.model.WsSshS2c
import top.e404.mywol.model.WsSshShutdownC2s
import top.e404.mywol.model.WsSshShutdownS2c
import top.e404.mywol.model.WsSyncC2s
import top.e404.mywol.model.WsSyncS2c
import top.e404.mywol.model.WsWolC2s
import top.e404.mywol.model.WsWolS2c
import top.e404.mywol.util.debug
import top.e404.mywol.util.info
import top.e404.mywol.util.logger
import top.e404.mywol.util.sendWolPacket
import top.e404.mywol.util.warn
import top.e404.mywol.vm.LocalVm
import top.e404.mywol.vm.RemoteVm
import top.e404.mywol.vm.Result
import top.e404.mywol.vm.SshReq
import top.e404.mywol.vm.SshVm
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

suspend fun WebsocketService.Companion.sendSshReq(
    clientId: String,
    machineId: String,
    command: String
) = instance!!
    .handler!!
    .sendSshReq(clientId, machineId, command)

suspend fun WebsocketService.Companion.sendSshShutdown(
    clientId: String,
    machineId: String,
) = instance!!
    .handler!!
    .sendSshShutdown(clientId, machineId)

class WebsocketHandler(
    val id: String,
    val name: String,
    val serverAddress: String,
    val secret: String?
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

    private var session: WebSocketSession? = null
    private suspend fun sendPacket(packet: WsC2sData) {
        if (session == null) error("websocket未连接")
        try {
            val json = json.encodeToString(WsC2sData.serializer(), packet)
            log.debug("发送数据包: {}", json)
            session!!.outgoing.send(Frame.Text(json))
        } catch (t: Throwable) {
            log.warn("发送数据包失败: ", t)
        }
    }

    fun closeWebsocket() {
        session?.let {
            websocketScope.launch {
                it.close()
            }
        } ?: error("websocket未连接")
    }

    private val connectScope = CoroutineScope(SupervisorJob() + CoroutineExceptionHandler { _, t ->
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
            websocket()
        }
        log.warn("uncaught exception in websocket: ", t)
    }).also { setCtx(it) }
    private lateinit var websocketScope: CoroutineScope

    fun start() {
        if (state != WsState.INITIALIZING) {
            log.warn("ws处理器已启动, state: $state, ", RuntimeException())
            return
        }
        log.debug { "启动ws处理器" }
        state = WsState.CONNECTING
        websocketScope.launch {
            websocket()
        }
    }

    private var autoSyncJob = UiVm.ioScope.launch {
        while (true) {
            delay(10_000 * 60)
            if (state == WsState.OPEN) syncMachines()
        }
    }

    /**
     * 上传所有本机的设备信息
     */
    suspend fun syncMachines() {
        val list = withContext(Dispatchers.IO) { LocalVm.listNormal() }
        log.info { "同步本机设备: ${list.map { it.name }}" }
        sendPacket(WsSyncC2s(list.map { machine ->
            WolMachine(
                machine.id,
                machine.name,
                machine.time,
                LocalVm.machineState[machine.id] ?: MachineState.OFF,
                machine.sshUsername.isNotBlank(),
                machine.sshShutdownCommand.isNotBlank()
            )
        }))
    }

    /**
     * 启动websocket连接
     */
    private suspend fun websocket() {
        log.debug { "开始websocket连接: $wsUrl" }
        httpClient.webSocket(wsUrl, {
            header("id", id)
            header("name", name)
            if (!secret.isNullOrBlank()) header("Authorization", "Bearer $secret")
        }) {
            session = this
            log.debug { "完成websocket连接" }
            UiVm.showSnackbar("服务器已连接")
            state = WsState.OPEN

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
                sendWolPacket(machine.deviceHost, machine.mac)
                val packet = WsWolC2s(true, "", s2cData.id)
                sendPacket(packet)
            }

            is WsSyncS2c -> {
                log.debug { "同步远程客户端信息: ${s2cData.clients}" }
                RemoteVm.clients.value = s2cData.clients
                RemoteVm.sshHistories.keys
                    .filter { id -> s2cData.clients.any { it.id == id } }
                    .forEach { RemoteVm.sshHistories.remove(it) }
            }

            is WsSshHistoryS2c -> {
                val machine = LocalVm.itemList.value.find { it.id == s2cData.machineId }
                if (machine == null) {
                    val packet = WsSshHistoryC2s(false, "没有该机器", emptyList(), s2cData.id)
                    sendPacket(packet)
                    return
                }
                val history = SshVm.getOrCreate(machine).history.value
                val packet = WsSshHistoryC2s(true, "", history, s2cData.id)
                sendPacket(packet)
            }

            is WsSshS2c -> {
                val machine = LocalVm.itemList.value.find { it.id == s2cData.machineId }
                if (machine == null) {
                    val packet = WsSshC2s(false, "没有该机器", s2cData.id)
                    sendPacket(packet)
                    return
                }
                val handler = SshVm.getOrCreate(machine)
                handler.start()
                val result = handler.exec(s2cData.command)
                val packet =
                    if (result.success) WsSshC2s(true, result.result, s2cData.id)
                    else WsSshC2s(false, result.message, s2cData.id)
                sendPacket(packet)
            }

            is WsSshShutdownS2c -> {
                val machine = LocalVm.itemList.value.find { it.id == s2cData.machineId }
                if (machine == null) {
                    val packet = WsSshC2s(false, "没有该机器", s2cData.id)
                    sendPacket(packet)
                    return
                }
                val handler = SshVm.getOrCreate(machine)
                handler.start()
                val result = handler.exec(machine.sshShutdownCommand)
                val packet =
                    if (result.success) WsSshShutdownC2s(true, result.result, s2cData.id)
                    else WsSshShutdownC2s(false, result.message, s2cData.id)
                sendPacket(packet)
            }
        }
    }

    suspend fun sendWolReq(clientId: String, machineId: String): Result<Unit> {
        val response = try {
            httpClient.post("http://$serverAddress/wol") {
                contentType(ContentType.Application.Json)
                setBody(WolReq(clientId, machineId))
            }
        } catch (e: Exception) {
            log.warn("发送wol请求失败: ", e)
            return Result.fail(e.message ?: "发送wol请求失败")
        }
        return if (response.status == HttpStatusCode.OK) Result.success(Unit)
        else Result.fail(response.bodyAsText())
    }

    suspend fun sendSshReq(clientId: String, machineId: String, command: String): Result<String> {
        log.debug { "发送请求" }
        val response = try {
            httpClient.post("http://$serverAddress/ssh") {
                contentType(ContentType.Application.Json)
                setBody(SshReq(clientId, machineId, command))
            }
        } catch (e: Exception) {
            log.warn("发送ssh请求失败: ", e)
            return Result.fail(e.message ?: "发送ssh请求失败")
        }
        log.debug { "接收响应" }
        return if (response.status == HttpStatusCode.OK) Result.success(response.bodyAsText())
        else Result.fail(response.bodyAsText())
    }

    suspend fun sendSshShutdown(clientId: String, machineId: String): Result<String> {
        log.debug { "发送请求" }
        val response = try {
            httpClient.post("http://$serverAddress/ssh/shutdown") {
                contentType(ContentType.Application.Json)
                setBody(WolReq(clientId, machineId))
            }
        } catch (e: Exception) {
            log.warn("发送ssh请求失败: ", e)
            return Result.fail(e.message ?: "发送ssh请求失败")
        }
        log.debug { "接收响应" }
        return if (response.status == HttpStatusCode.OK) Result.success(response.bodyAsText())
        else Result.fail(response.bodyAsText())
    }

    fun close() {
        autoSyncJob.cancel()
        closeWebsocket()
        connectScope.cancel()
        websocketScope.cancel()
    }
}