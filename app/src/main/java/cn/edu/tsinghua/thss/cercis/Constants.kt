package cn.edu.tsinghua.thss.cercis

object Constants {
    // API
    const val URL_BASE = "https://cercis.cn/api/v1/"
    const val WSS_BASE = "ws://localhost/api/v1/"
    const val WSS_MESSAGES = "${WSS_BASE}messages"

    const val REFRESH_COUNT: Long = 15

    const val SEND_CODE_COUNTDOWN: Int = 60
}