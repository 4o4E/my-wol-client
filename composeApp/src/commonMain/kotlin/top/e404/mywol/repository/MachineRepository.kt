package top.e404.mywol.repository

import kotlinx.coroutines.flow.Flow
import top.e404.mywol.dao.Machine
import top.e404.mywol.dao.MachineDao

interface MachineRepository : Repository {
    suspend fun getById(id: String): Machine?
    fun list(): Flow<List<Machine>>
    suspend fun listNormal(): List<Machine>
    suspend fun save(machine: Machine)
    suspend fun update(machine: Machine)
    suspend fun remove(id: String)
    suspend fun import(machines: List<Machine>)
}

class MachineRepositoryImpl(
    private val machineDao: MachineDao
) : MachineRepository {
    override suspend fun getById(id: String) = machineDao.getById(id)
    override fun list() = machineDao.list()
    override suspend fun listNormal() = machineDao.listNormal()
    override suspend fun save(machine: Machine) = machineDao.insert(machine)
    override suspend fun update(machine: Machine) = machineDao.update(machine)
    override suspend fun remove(id: String) = machineDao.delete(id)
    override suspend fun import(machines: List<Machine>) = machineDao.import(machines)
}