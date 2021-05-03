package cn.cercis.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor() : ViewModel() {
    /**
     * This is simply a placeholder class, for that navigation operations cannot be directly done
     * through view model, and that indirectly doing the operation is dumb.
     */
}
