package top.e404.mywol.util

import android.os.Build

inline fun <T> ver(
    minVersion: Int,
    after: () -> T,
    before: () -> T
): T = if (Build.VERSION.SDK_INT >= minVersion) {
    after()
} else {
    before()
}

inline fun <T> afterVer(
    minVersion: Int,
    after: () -> T
) {
    if (Build.VERSION.SDK_INT >= minVersion) after()
}