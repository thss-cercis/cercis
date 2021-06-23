package cn.cercis.util

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import cn.cercis.R
import cn.cercis.util.resource.NetworkResponse
import com.google.android.material.snackbar.BaseTransientBottomBar.Duration
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * @see [Snackbar.make]
 */
fun snackbarMakeError(
    view: View,
    text: CharSequence,
    @Duration duration: Int,
    decorations: Snackbar.() -> Unit = {},
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
    decorations: Snackbar.() -> Unit = {},
) = Snackbar.make(view, text, duration)
    .setBackgroundTint(getColor(R.color.snackbar_success_background))
    .setTextColor(getColor(R.color.snackbar_success_text))
    .apply(decorations)
    .show()

fun <T : Fragment, E> T.makeSnackbar(
    makeRequest: suspend T.() -> NetworkResponse<E>,
    successMessage: (E) -> String,
    errorMessage: (NetworkResponse<E>) -> String = { it.message ?: "" },
) {
    lifecycleScope.launch(Dispatchers.IO) {
        val result = MutableLiveData(makeRequest())
        lifecycleScope.launch(Dispatchers.Main) {
            result.observe(viewLifecycleOwner) {
                when (it) {
                    is NetworkResponse.NetworkError, is NetworkResponse.Reject -> {
                        snackbarMakeError(requireView(),
                            errorMessage(it),
                            Snackbar.LENGTH_SHORT) {
                            setAction(R.string.snackbar_retry) {
                                launch(Dispatchers.IO) {
                                    result.postValue(makeRequest())
                                }
                            }
                        }
                    }
                    is NetworkResponse.Success -> {
                        snackbarMakeSuccess(requireView(),
                            successMessage(it.data),
                            Snackbar.LENGTH_SHORT
                        )
                    }
                    null -> Unit
                }
            }
        }
    }
}
