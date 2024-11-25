package top.e404.mywol.util

import android.os.Build

inline fun <T> ver(
    minVersion: Int,
    before: () -> T,
    after: () -> T
): T = if (Build.VERSION.SDK_INT >= minVersion) {
    before()
} else {
    after()
}

inline fun <T> afterVer(
    minVersion: Int,
    after: () -> T
) {
    if (Build.VERSION.SDK_INT >= minVersion) after()
}