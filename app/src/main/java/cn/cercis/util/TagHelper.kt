package cn.cercis.util

val Any.LOG_TAG: String
    get() {
        return this.javaClass.simpleName
    }
