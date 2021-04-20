package cn.edu.tsinghua.thss.cercis.util

import android.animation.LayoutTransition
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import cn.edu.tsinghua.thss.cercis.MainActivity

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

/**
 * Navigates on global_nav_graph.
 *
 * This is done via a hole in MainActivity.
 * @param id resId for navigation action
 */
fun Fragment.doDetailNavigation(id: Int) {
    (requireActivity() as MainActivity).doDetailNavigation(id)
}

/**
 * Navigates on global_nav_graph.
 *
 * This is done via a hole in MainActivity.
 * @param navDirections directions bound with safe args
 */
fun Fragment.doDetailNavigation(navDirections: NavDirections) {
    (requireActivity() as MainActivity).doDetailNavigation(navDirections)
}
