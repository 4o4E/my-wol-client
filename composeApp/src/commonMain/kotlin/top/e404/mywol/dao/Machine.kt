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
import top.e404.mywol.util.sendWolPacket
import java.nio.charset.Charset

/**
 * 设备数据
 *
 * @property id 主键
 * @property name 名字
 * @property deviceHost 设备地址
 * @property wolHost `walk-on-lan` 广播地址
 * @property wolPort `walk-on-lan` 广播端口
 * @property mac 设备mac地址
 * @property sshPort ssh服务端口
 * @property sshUsername ssh用户名
 * @property sshSecretType ssh密码类型
 * @property sshSecretValue ssh密码值
 * @property sshCharset ssh字符集
 * @property sshShutdownCommand ssh关机指令
 * @property time 创建时间
 */
@Serializable
@Entity(tableName = Machine.TABLE_NAME)
data class Machine(
    @PrimaryKey
    var id: String,
    var name: String,
    var deviceHost: String,
    var wolHost: String,
    var wolPort: Int,
    var mac: String,

    var sshPort: Int,
    var sshUsername: String,
    var sshSecretType: SshSecretType,
    var sshSecretValue: String,
    var sshCharset: String,
    var sshShutdownCommand: String,

    var time: Long,
) {
    companion object {
        const val TABLE_NAME = "machine"
    }

    suspend fun sendMagicPacket() = sendWolPacket(deviceHost, mac)
}

interface MachineValidate {
    private companion object {
        private val IP_REGEX = Regex(
            "(?i)((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)(\\.(?!$)|$)){4}|" + // IPv4
                    "([\\da-f]{1,4}:){7}[0-9a-f]{1,4}|" +              // IPv6 (full form)
                    "(([\\da-f]{1,4}:){1,7}:)|" +                      // IPv6 (:: shortcut)
                    "(([\\da-f]{1,4}:){1,6}:[\\da-f]{1,4})|" +         // IPv6 (1 missing group)
                    "(([\\da-f]{1,4}:){1,5}(:[\\da-f]{1,4}){1,2})|" +  // IPv6 (2 missing groups)
                    "(([\\da-f]{1,4}:){1,4}(:[\\da-f]{1,4}){1,3})|" +  // IPv6 (3 missing groups)
                    "(([\\da-f]{1,4}:){1,3}(:[\\da-f]{1,4}){1,4})|" +  // IPv6 (4 missing groups)
                    "(([\\da-f]{1,4}:){1,2}(:[\\da-f]{1,4}){1,5})|" +  // IPv6 (5 missing groups)
                    "([\\da-f]{1,4}:((:[\\da-f]{1,4}){1,6}))|" +       // IPv6 (6 missing groups)
                    "(::((:[\\da-f]{1,4}){1,7}|:))"                    // IPv6 (:: alone))
        )
        private val MAC_REGEX = Regex("(?i)([\\da-z]{2}:){5}([\\da-z]{2})")
    }

    fun String.validateNotBlank() = if (isBlank()) "不可为空" else null
    fun String.validateHost(allowEmpty: Boolean = false) = when {
        allowEmpty && isEmpty() -> null
        isBlank() -> "不可为空"
        !matches(IP_REGEX) -> "格式错误"
        else -> null
    }

    fun String.validatePort() = if (toIntOrNull()?.let { it in 1..65535 } == true) null else "格式错误"
    fun String.validateMac() = when {
        isBlank() -> "不可为空"
        !matches(MAC_REGEX) -> "格式错误"
        else -> null
    }

    fun String.validateCharset() = when {
        isBlank() -> "不可为空"
        runCatching { Charset.forName(this) }.isFailure -> "无效字符集"
        else -> null
    }
}

val validate = object : MachineValidate {}

inline fun validateMachine(scope: MachineValidate.() -> Unit) = scope.invoke(validate)
inline fun validate(scope: MachineValidate.() -> String?) = scope.invoke(validate)

@Serializable
enum class SshSecretType(val display: String) {
    PASSWORD("密码"), KEY("密钥")
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
