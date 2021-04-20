package cn.edu.tsinghua.thss.cercis.util

val Any.LOG_TAG: String
    get() {
        return this.javaClass.simpleName
    }
