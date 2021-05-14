package cn.cercis.util

import android.view.View
import cn.cercis.R
import com.google.android.material.snackbar.BaseTransientBottomBar.Duration
import com.google.android.material.snackbar.Snackbar

/**
 * @see [Snackbar.make]
 */
fun snackbarMakeError(
    view: View,
    text: CharSequence,
    @Duration duration: Int,
    decorations: Snackbar.() -> Unit = {}
) = Snackbar.make(view, text, duration)
    .setBackgroundTint(getColor(R.color.snackbar_error_background))
    .setTextColor(getColor(R.color.snackbar_error_text))
    .apply(decorations)
    .show()

/**
 * @see [Snackbar.make]
 */
fun snackbarMakeSuccess(
    view: View,
    text: CharSequence,
    @Duration duration: Int,
    decorations: Snackbar.() -> Unit = {}
) = Snackbar.make(view, text, duration)
    .setBackgroundTint(getColor(R.color.snackbar_success_background))
    .setTextColor(getColor(R.color.snackbar_success_text))
    .apply(decorations)
    .show()
