package top.e404.mywol.dao


import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        Machine::class,
    ],
    version = 1
)
abstract class WolDatabase : RoomDatabase() {
    abstract val machineDao: MachineDao
}