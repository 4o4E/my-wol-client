package top.e404.mywol.dao

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import top.e404.mywol.sendMagicPacket

/**
 * 设备数据
 *
 * @property id 主键
 * @property name 名字
 * @property deviceIp 设备ip地址
 * @property broadcastIp 广播ip地址
 * @property mac 设备mac地址
 * @property time 创建时间
 */
@Serializable
@Entity(tableName = Machine.TABLE_NAME)
data class Machine(
    @PrimaryKey
    var id: String,
    var name: String,
    var deviceIp: String,
    var broadcastIp: String,
    var mac: String,
    var time: Long,
) {
    companion object {
        const val TABLE_NAME = "machine"
    }

    suspend fun sendMagicPacket() = sendMagicPacket(deviceIp, mac)
}

@Dao
interface MachineDao {
    @Query("SELECT * FROM ${Machine.TABLE_NAME} WHERE id = :id")
    suspend fun getById(id: String): Machine?

    @Query("SELECT * FROM ${Machine.TABLE_NAME} ORDER BY time DESC")
    fun list(): Flow<List<Machine>>

    @Query("SELECT * FROM ${Machine.TABLE_NAME} ORDER BY time DESC")
    suspend fun listNormal(): List<Machine>

    @Insert
    suspend fun insert(machine: Machine)

    @Insert
    suspend fun insert(machines: Collection<Machine>)

    @Update
    suspend fun update(machine: Machine)

    @Query("DELETE FROM ${Machine.TABLE_NAME} WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM ${Machine.TABLE_NAME}")
    suspend fun deleteAll()

    @Transaction
    suspend fun import(machines: List<Machine>) {
        deleteAll()
        insert(machines)
    }
}
