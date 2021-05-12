package cn.cercis.common

val Any.LOG_TAG: String
    get() = javaClass.simpleName

infix fun String.matches(pattern: String) = this matches Regex(pattern)

inline fun <T, R> Iterable<T>.mapRun(block: T.() -> R) = map(block)
