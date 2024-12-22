package top.e404.mywol.util

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import org.koin.core.KoinApplication
import org.koin.core.scope.Scope
import org.koin.dsl.module
import top.e404.mywol.Context
import top.e404.mywol.createDatabaseBuilder
import top.e404.mywol.dao.Migration1to2
import top.e404.mywol.dao.WolDatabase
import top.e404.mywol.repository.MachineRepository
import top.e404.mywol.repository.MachineRepositoryImpl

private val Scope.database get() = get<WolDatabase>()

fun KoinApplication.getCommonKoinModule(getContext: () -> Context) = module {
    single<WolDatabase> {
        getContext().createDatabaseBuilder()
            .fallbackToDestructiveMigrationOnDowngrade(true)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .addMigrations(Migration1to2)
            .build()
    }
    single<MachineRepository> { MachineRepositoryImpl(database.machineDao) }
//    single<ScheduleRepository> { ScheduleRepositoryImpl(database.scheduleDao) }
}