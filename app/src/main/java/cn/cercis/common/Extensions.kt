package cn.cercis.common

val Any.LOG_TAG: String
    get() = javaClass.simpleName

infix fun String.matches(pattern: String) = this matches Regex(pattern)
