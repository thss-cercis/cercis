package cn.cercis.util

inline fun <T, R> Iterable<T>.mapRun(block: T.() -> R) = map(block)
