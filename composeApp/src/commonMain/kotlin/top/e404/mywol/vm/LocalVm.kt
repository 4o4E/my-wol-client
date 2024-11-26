package top.e404.mywol.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import top.e404.mywol.dao.Machine
import top.e404.mywol.model.MachineState
import top.e404.mywol.repository.MachineRepository
import top.e404.mywol.util.debug
import top.e404.mywol.util.logger
import java.net.InetAddress

object LocalVm : ViewModel(), KoinComponent {
    private val log = logger()
    private val machineRepository: MachineRepository by inject()

    val itemList = list().stateIn(
        viewModelScope,
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

    var machineState = mapOf<String, MachineState>()
    private lateinit var machineStateSyncJob: Job
    fun startSync() {
        log.debug { "startSync" }
        machineStateSyncJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                val list = listNormal()
                val stateMap = list.map {
                    async(Dispatchers.IO) { it to getMachineState(it) }
                }.awaitAll().toMap()
                // 变更再广播
                val changed = stateMap.entries.filter { (machine, state) ->
                    machineState[machine.id] != state
                }
                if (changed.isNotEmpty()) {
                    log.debug { "sendChange" }
                    RemoteVm.syncMachines()
                }
                machineState = stateMap.entries.associate { it.key.id to it.value }
                delay(1000)
            }
        }
    }

    private const val PING_TIMEOUT = 3000
    private suspend fun getMachineState(machine: Machine) = withContext(Dispatchers.IO) {
        try {
            val address = InetAddress.getByName(machine.deviceIp)
            if (address?.isReachable(PING_TIMEOUT) == true) MachineState.ON
            else MachineState.OFF
        } catch (t: Throwable) {
            MachineState.OFF
        }
    }
}
