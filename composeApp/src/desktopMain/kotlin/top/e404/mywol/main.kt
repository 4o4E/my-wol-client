package top.e404.mywol

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
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
import top.e404.mywol.util.error
import top.e404.mywol.util.getCommonKoinModule
import top.e404.mywol.util.logger
import java.io.File

private val logger by lazy { logger("Wol") }

object WolDesktop {
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
        val windowState = WindowState(
            size = kotlin.runCatching {
                calculateWindowSize(defaultSize.width, defaultSize.height)
            }.onFailure {
                logger.error(it) { "Failed to calculate window size" }
            }.getOrElse {
                defaultSize
            },
            position = WindowPosition.Aligned(Alignment.Center),
        )
        val context = DesktopContext(
            windowState,
            File("data"),
            File("cache"),
            File("logs"),
        )
        startKoin {
            modules(getCommonKoinModule({ context }))
        }


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

@Preview
@Composable
fun DesktopPreview() = App()