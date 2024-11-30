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
    suspend fun start(background: Boolean = false) {
        if (client.isStarted) return
        client.start()
        sshScope.async {
            val feature = client.connect(info.sshUsername, info.deviceHost, info.sshPort)
            try {
                feature.verify(10, TimeUnit.SECONDS)
            } catch (t: Throwable) {
                log.warn(t) { "ssh连接失败: " }
                if (!background) UiVm.showSnackbar("SSH连接成功: ${t.message}")
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
                    ).first()
                    session.addPublicKeyIdentity(keyPair)
                }
            }
            session.auth().verify().await()
            if (!background) UiVm.showSnackbar("SSH连接成功")
            true
        }.await()
    }

    suspend fun exec(command: String): Result<String> {
        if (session == null) start()
        val session = session ?: return Result.fail("ssh未连接")
        val result = withContext(Dispatchers.IO) {
            val channel = session.createExecChannel(command)
            channel.open().verify()
            val inputStream = channel.invertedOut
            inputStream.bufferedReader(Charset.forName(info.sshCharset)).use { it.readText() }
        }

        return Result.success(result)
    }

    override fun close() {
        val session = session ?: return
        if (!session.isClosed) session.close()
        if (client.isStarted) client.stop()
        isStart.value = false
    }
}