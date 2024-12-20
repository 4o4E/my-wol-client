package top.e404.mywol.vm

import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import top.e404.mywol.dao.Machine
import top.e404.mywol.model.MachineState
import top.e404.mywol.repository.MachineRepository
import top.e404.mywol.util.debug
import top.e404.mywol.util.logger
import top.e404.mywol.util.warn
import java.net.InetAddress

object LocalVm : KoinComponent {
    private val log = logger()
    private val machineRepository: MachineRepository by inject()

    val localVmScope = CoroutineScope(SupervisorJob() + CoroutineExceptionHandler { _, t ->
        log.warn(t) { "uncaught exception in localVmScope: " }
    })
    val detailMachineAnimation = mutableStateOf(false)

    val itemList = list().stateIn(
        localVmScope,
        SharingStarted.Lazily,
        emptyList()
    )

    suspend fun getById(id: String) = withContext(Dispatchers.IO) { machineRepository.getById(id) }
    fun list() = machineRepository.list()
    suspend fun listNormal() = withContext(Dispatchers.IO) { machineRepository.listNormal() }
    suspend fun save(machine: Machine) = withContext(Dispatchers.IO) {
        machineRepository.save(machine)
    }

    suspend fun remove(id: String) = withContext(Dispatchers.IO) { machineRepository.remove(id) }
    suspend fun update(machine: Machine) = withContext(Dispatchers.IO) {
        machineRepository.update(machine)
    }

    @Volatile
    var machineState = mapOf<String, MachineState>()
    private lateinit var machineStateSyncJob: Job
    fun startSync() {
        log.debug { "startSync" }
        machineStateSyncJob = localVmScope.launch(Dispatchers.IO) {
            while (true) {
                val list = listNormal()
                val stateMap = list.map {
                    async(Dispatchers.IO) { it to getMachineState(it) }
                }.awaitAll().toMap()
                // 变更再广播
                val changed = stateMap.entries.filter { (machine, state) ->
                    machineState[machine.id] != state
                }
                machineState = stateMap.entries.associate { it.key.id to it.value }
                if (changed.isNotEmpty()) {
                    log.debug { "sendChange" }
                    RemoteVm.syncMachines()
                }
                delay(3000)
            }
        }
    }

    fun stop() = machineStateSyncJob.cancel()

    private const val PING_TIMEOUT = 3000
    private suspend fun getMachineState(machine: Machine) = withContext(Dispatchers.IO) {
        try {
            val address = InetAddress.getByName(machine.deviceHost)
            if (address?.isReachable(PING_TIMEOUT) == true) MachineState.ON
            else MachineState.OFF
        } catch (t: Throwable) {
            MachineState.OFF
        }
    }

    suspend fun exportAll() = Json.encodeToString(ListSerializer(Machine.serializer()), machineRepository.listNormal())
    suspend fun importAll(json: String) {
        val list = Json.decodeFromString(ListSerializer(Machine.serializer()), json)
        machineRepository.import(list)
    }
}
