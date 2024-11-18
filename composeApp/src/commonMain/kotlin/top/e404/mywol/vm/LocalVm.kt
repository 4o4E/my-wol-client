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
import top.e404.mywol.dao.WolDatabase
import top.e404.mywol.model.MachineState
import top.e404.mywol.util.logger
import java.net.InetAddress

object LocalVm : ViewModel(), KoinComponent {
    private val log = logger(LocalVm::class)
    private val db: WolDatabase by inject()
    private val machineDao inline get() = db.machineDao

    val itemList = list().stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        emptyList()
    )

    fun getById(id: String) = machineDao.getById(id)
    fun list() = machineDao.list()
    fun listNormal() = machineDao.listNormal()

    fun save(machine: Machine) = machineDao.insert(machine)

    fun remove(id: String) = machineDao.delete(id)

    fun update(machine: Machine) = machineDao.update(machine)

    var machineState = mapOf<String, MachineState>()
    private lateinit var machineStateSyncJob: Job
    fun startSync() {
        machineStateSyncJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                val stateMap = listNormal().map {
                    async(Dispatchers.IO) {
                        it.id to getMachineState(it).also { state ->
                            log.debug("机器({})状态变更: {}", it.name, state)
                        }
                    }
                }.awaitAll().toMap()
                // 变更再广播
                val changed = stateMap.entries.filter { (id, state) ->
                    machineState[id] != state
                }
                if (changed.isNotEmpty()) {
                    RemoteVm.onMachineStateChange(changed.associate { it.toPair() })
                }
                machineState = stateMap
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
