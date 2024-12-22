package top.e404.mywol.dao


import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

@Database(
    entities = [Machine::class],
    version = 2
)
abstract class WolDatabase : RoomDatabase() {
    abstract val machineDao: MachineDao
}

object Migration1to2 : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE machine ADD COLUMN cron TEXT NOT NULL DEFAULT ''")
    }
} 