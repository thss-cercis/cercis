package cn.edu.tsinghua.thss.cercis.util

import android.animation.LayoutTransition
import android.content.Context
import android.content.Intent
import android.view.ViewGroup
import cn.edu.tsinghua.thss.cercis.StartupActivity

object ActivityHelper {
    fun switchToStartupActivity(context: Context) {
        val intent = Intent(context, StartupActivity::class.java)
        context.startActivity(intent)
    }
}

/**
 * Enables transition animation for a given view.
 */
fun ViewGroup.enableTransition() {
    val transition = this.layoutTransition
    if (transition == null) {
        val newTransition = LayoutTransition()
        newTransition.enableTransitionType(LayoutTransition.CHANGING)
        this.layoutTransition = newTransition
    } else {
        this.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
    }
}