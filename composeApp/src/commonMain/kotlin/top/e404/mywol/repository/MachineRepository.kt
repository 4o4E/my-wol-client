package top.e404.mywol.repository

import kotlinx.coroutines.flow.Flow
import top.e404.mywol.dao.Machine
import top.e404.mywol.dao.MachineDao

interface MachineRepository : Repository {
    fun list(): Flow<List<Machine>>
    fun save(machine: Machine)
    fun update(machine: Machine)
    fun remove(id: String)
    fun import(machines: List<Machine>)
}

class MachineRepositoryImpl(
    private val machineDao: MachineDao
) : MachineRepository {
    override fun list() = machineDao.list()
    override fun save(machine: Machine) = machineDao.insert(machine)
    override fun update(machine: Machine) = machineDao.update(machine)
    override fun remove(id: String) = machineDao.delete(id)
    override fun import(machines: List<Machine>) = machineDao.import(machines)
}