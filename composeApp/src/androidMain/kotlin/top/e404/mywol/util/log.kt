package top.e404.mywol.util

import android.util.Log
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.core.util.StatusPrinter
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream


object AndroidLoggingConfigurator {
    @Language("xml")
    private fun getXML(logsDir: String) = """
<configuration>
    <appender name="logcat" class="ch.qos.logback.classic.android.LogcatAppender">
        <tagEncoder>
            <pattern>%logger{12}</pattern>
        </tagEncoder>
        <encoder>
            <pattern>%msg%n%ex%n</pattern>
        </encoder>
    </appender>

    <appender name="file" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${logsDir}/mywol.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${logsDir}/mywol.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>3</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%date{yyyy-MM-dd HH:mm:ss} %-5level %logger{10} [%file:%line] %msg%n%ex</pattern>
        </encoder>
    </appender>

    <root level="TRACE">
        <appender-ref ref="logcat" />
        <appender-ref ref="file" />
    </root>
</configuration>
"""

    fun configure(logsDir: String) {
        Log.i("AndroidLoggingConfigurator", "Configuring logback, logsDir=$logsDir")
        LoggerFactory.getILoggerFactory().also { factory ->
            factory as LoggerContext
            factory.stop()
            kotlin.runCatching {
                JoranConfigurator().apply {
                    context = factory
                    doConfigure(ByteArrayInputStream(getXML(logsDir).encodeToByteArray()))
                }
            }.onFailure {
                Log.e("AndroidLoggingConfigurator", "Failed to configure logback", it)
            }
            StatusPrinter.print(factory)
        }
    }
}