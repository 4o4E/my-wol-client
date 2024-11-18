package top.e404.mywol.util


import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.awt.Toolkit

object ScreenUtils {

    private fun getScreenDensity(): Density {
        return Density(
            GraphicsEnvironment.getLocalGraphicsEnvironment()
                .defaultScreenDevice.defaultConfiguration.defaultTransform.scaleX.toFloat(),
            fontScale = 1f,
        )
    }

    /**
     * 获取经过缩放后的, 实际可用的屏幕大小. 将窗口设置为这个大小即可占满整个屏幕
     */
    fun getScreenSize(): DpSize {
        val dimension: Dimension = Toolkit.getDefaultToolkit().screenSize

        // windows 的 dimension 是没有经过缩放的
        // Get screen dimensions
        val screenWidth = dimension.width
        val screenHeight = dimension.height

        // Convert screen dimensions to dp
        // See ui-desktop-1.6.10-sources.jar!/desktopMain/androidx/compose/ui/window/LayoutConfiguration.desktop.kt:45
        val density = getScreenDensity()
        val screenWidthDp = density.run { screenWidth.toDp() }
        val screenHeightDp = density.run { screenHeight.toDp() }

        return DpSize(screenWidthDp, screenHeightDp)
    }
}
