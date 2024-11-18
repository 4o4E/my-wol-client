package top.e404.mywol.util

fun valueWithError(value: String, error: String?) = buildString {
    append(value)
    error?.let { append(" - ").append(it) }
}