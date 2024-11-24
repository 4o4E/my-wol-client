package top.e404.mywol.vm

import top.e404.mywol.getSettings

object SettingsVm {
    val remote by lazy { getSettings("remote") }
    val local by lazy { getSettings("local") }

}