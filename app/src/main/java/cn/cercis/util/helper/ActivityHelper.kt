package cn.cercis.util.helper

import android.animation.LayoutTransition
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.annotation.MainThread
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import cn.cercis.MainActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview


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
@FlowPreview
@ExperimentalCoroutinesApi
fun Fragment.doDetailNavigation(id: Int) {
    requireMainActivity().doDetailNavigation(id)
}

/**
 * Navigates on global_nav_graph.
 *
 * This is done via a hole in MainActivity.
 * @param navDirections directions bound with safe args
 */
@FlowPreview
@ExperimentalCoroutinesApi
fun Fragment.doDetailNavigation(navDirections: NavDirections) {
    requireMainActivity().doDetailNavigation(navDirections)
}

@FlowPreview
@ExperimentalCoroutinesApi
@MainThread
fun Fragment.requireMainActivity(): MainActivity {
    return requireActivity() as MainActivity
}

fun Fragment.closeIme() {
    val ime: InputMethodManager? =
        requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
    ime?.hideSoftInputFromWindow(
        view?.applicationWindowToken,
        InputMethodManager.HIDE_NOT_ALWAYS
    )
}

fun Fragment.openApplicationSettingsPage() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val uri = Uri.fromParts("package", requireContext().packageName, null)
    intent.data = uri
    ContextCompat.startActivity(requireContext(), intent, Bundle())
}

fun Context.openApplicationSettingsPage() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val uri = Uri.fromParts("package", this.packageName, null)
    intent.data = uri
    ContextCompat.startActivity(this, intent, Bundle())
}
