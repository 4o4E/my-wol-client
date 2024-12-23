package top.e404.mywol.vm

import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.apache.sshd.client.ClientBuilder
import org.apache.sshd.client.SshClient
import org.apache.sshd.client.config.hosts.HostConfigEntryResolver
import org.apache.sshd.client.session.ClientSession
import org.apache.sshd.common.NamedResource
import org.apache.sshd.common.util.security.SecurityUtils
import top.e404.mywol.dao.Machine
import top.e404.mywol.dao.SshSecretType
import top.e404.mywol.model.SshHistory
import top.e404.mywol.model.SshResult
import top.e404.mywol.util.logger
import top.e404.mywol.util.warn
import java.io.Closeable
import java.nio.charset.Charset
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object SshVm {
    val handlers: MutableMap<String, SshHandler> = ConcurrentHashMap()

    fun getOrCreate(machine: Machine): SshHandler {
        return handlers.getOrPut(machine.id) { SshHandler(machine) }
    }

    fun close(id: String) {
        handlers.remove(id)?.close()
    }
}

class SshHandler(private val info: Machine) : Closeable {
    private val log = logger()
    private val sshScope = CoroutineScope(SupervisorJob() + CoroutineExceptionHandler { _, t ->
        log.warn(t) { "uncaught exception in sshScope: " }
        UiVm.showSnackbar("SSH连接失败: ${t.message}")
    })
    private val client: SshClient = ClientBuilder.builder()
        .hostConfigEntryResolver(HostConfigEntryResolver.EMPTY)
        .build()
    val isStart = mutableStateOf(false)
    val history = mutableStateOf(listOf<SshHistory>())
    private var session: ClientSession? = null
    private var initialized = false
    private suspend fun start(background: Boolean = false) {
        if (!client.isStarted) client.start()
        if (initialized) return
        initialized = sshScope.async {
            val feature = client.connect(info.sshUsername, info.deviceHost, info.sshPort)
            try {
                feature.verify(3, TimeUnit.SECONDS)
            } catch (t: Throwable) {
                log.warn(t) { "ssh连接失败: " }
                // 连接失败后重新初始化链接
                session?.runCatching { close() }
                initialized = false
                if (!background) UiVm.showSnackbar("SSH连接失败: ${t.message}")
                return@async false
            }
            val session = feature.session
            isStart.value = true
            this@SshHandler.session = session
            when (info.sshSecretType) {
                SshSecretType.PASSWORD -> session.addPasswordIdentity(info.sshSecretValue)
                SshSecretType.KEY -> {
                    val keyPair = SecurityUtils.loadKeyPairIdentities(
                        null,
                        NamedResource.ofName("ssh-rsa"),
                        info.sshSecretValue.byteInputStream(),
                        null
                    ).firstOrNull() ?: run {
                        UiVm.showSnackbar("SSH密钥加载失败")
                        return@async false
                    }
                    session.addPublicKeyIdentity(keyPair)
                }
            }
            session.auth().verify().await()
            if (!background) UiVm.showSnackbar("SSH连接成功")
            true
        }.await()
    }

    suspend fun exec(command: String): Result<SshResult> {
        start()
        val session = session ?: return Result.fail("ssh未连接")
        return withContext(Dispatchers.IO) {
            try {
                val channel = session.createExecChannel(command)
                channel.open().verify()
                val result = channel.invertedOut.bufferedReader(Charset.forName(info.sshCharset)).use { it.readText() }
                val exitStatus = channel.exitStatus!!
                val error = channel.invertedErr.bufferedReader(Charset.forName(info.sshCharset)).use { it.readText() }
                return@withContext Result.success(SshResult(exitStatus, result, error))
            } catch (t: Throwable) {
                log.warn("创建ssh通道失败: ", t)
                return@withContext Result.fail("创建ssh通道失败: ${t.message}")
            }
        }
    }

    override fun close() {
        val session = session ?: return
        if (!session.isClosed) session.close()
        if (client.isStarted) client.stop()
        isStart.value = false
    }
}