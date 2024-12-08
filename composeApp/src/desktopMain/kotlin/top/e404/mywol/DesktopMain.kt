package top.e404.mywol

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import org.koin.core.context.startKoin
import top.e404.mywol.util.ScreenUtils
import top.e404.mywol.util.debug
import top.e404.mywol.util.error
import top.e404.mywol.util.getCommonKoinModule
import top.e404.mywol.util.logger
import java.io.File

object DesktopMain {
    private val log = logger()
    private fun calculateWindowSize(
        desiredWidth: Dp,
        desiredHeight: Dp,
        screenSize: DpSize = ScreenUtils.getScreenSize()
    ): DpSize {
        return DpSize(
            width = if (desiredWidth > screenSize.width) screenSize.width else desiredWidth,
            height = if (desiredHeight > screenSize.height) screenSize.height else desiredHeight,
        )
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val defaultSize = DpSize(1301.dp, 855.dp)
        log.debug { "Screen size: ${ScreenUtils.getScreenSize()}" }
        val windowState = WindowState(
            size = kotlin.runCatching {
                calculateWindowSize(defaultSize.width, defaultSize.height)
            }.onFailure {
                log.error(it) { "Failed to calculate window size" }
            }.getOrElse {
                defaultSize
            },
            position = WindowPosition.Aligned(Alignment.Center),
        )
        val context = DesktopContext(
            windowState,
            File("data"),
            File("cache"),
        )
        startKoin {
            modules(getCommonKoinModule({ context }))
        }

        log.debug { "Starting application" }

        application {
            Window(
                onCloseRequest = ::exitApplication,
                title = "MyWol",
            ) {
                App()
            }
        }
    }
}