package top.e404.mywol.vm

import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.time.ExecutionTime
import com.cronutils.parser.CronParser
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import top.e404.mywol.repository.MachineRepository
import top.e404.mywol.ui.components.fullDisplayFormatter
import top.e404.mywol.util.debug
import top.e404.mywol.util.logger
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZonedDateTime
import kotlin.math.max
import kotlin.time.Duration.Companion.hours

object ScheduleVm : KoinComponent {
    private val log = logger()
    private val machineRepository: MachineRepository by inject()
    private val scope = CoroutineScope(Dispatchers.IO + CoroutineExceptionHandler { _, t ->
        log.warn("uncaught exception in ScheduleVm: ", t)
    })
    private var scheduleJob: Job? = null

    private val cronDefinition = CronDefinitionBuilder.defineCron()
        .withSeconds().withValidRange(0, 59).and()
        .withMinutes().withValidRange(0, 59).and()
        .withHours().withValidRange(0, 23).and()
        .withDayOfMonth().withValidRange(1, 31).supportsL().supportsW().supportsLW().supportsQuestionMark().and()
        .withMonth().withValidRange(1, 12).and()
        .withDayOfWeek().withValidRange(1, 7).withMondayDoWValue(2).supportsHash().supportsL().supportsQuestionMark().and()
        .withYear().withValidRange(1970, 9999).withStrictRange().optional().and()
        .instance()
    private val parser = CronParser(cronDefinition)
    val scheduleMap = mutableMapOf<String, LocalDateTime>()

    fun start() {
        scheduleJob?.cancel()
        scheduleJob = scope.launch {
            machineRepository.listNormal().forEach { machine ->
                log.debug { "start schedule for machine ${machine.name}" }
                var first = true
                val seq = generate(machine.cron)
                for (time in seq) {
                    scheduleMap[machine.id] = time
                    if (first) {
                        UiVm.showSnackbar("${machine.name}定时任务已启动, 下次执行将在${fullDisplayFormatter.format(time)}")
                        first = false
                    }
                    val now = LocalDateTime.now()
                    val delayMillis = Duration.between(now, time).toMillis()
                    delay(max(delayMillis, 1.hours.inWholeMilliseconds))
                    launch { machine.sendMagicPacket() }
                }
            }
        }
    }

    private fun generate(expression: String): Sequence<LocalDateTime> {
        val cron = parser.parse(expression)
        val executionTime = ExecutionTime.forCron(cron)
        return sequence {
            var current = ZonedDateTime.now()

            while (true) {
                val nextExecution = executionTime.nextExecution(current)
                    .orElse(null)
                    ?: break
                yield(nextExecution.toLocalDateTime())
                current = nextExecution
            }
        }

    }

    fun checkCron(expression: String) = runCatching { parser.parse(expression) }.isSuccess
}