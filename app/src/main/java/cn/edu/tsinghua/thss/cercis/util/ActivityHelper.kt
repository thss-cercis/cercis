package cn.edu.tsinghua.thss.cercis.util

import android.content.Context
import android.content.Intent
import cn.edu.tsinghua.thss.cercis.StartupActivity

object ActivityHelper {
    fun switchToStartupActivity(context: Context) {
        val intent = Intent(context, StartupActivity::class.java)
        context.startActivity(intent)
    }
}