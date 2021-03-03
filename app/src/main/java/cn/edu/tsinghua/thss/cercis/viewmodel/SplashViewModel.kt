package cn.edu.tsinghua.thss.cercis.viewmodel

import android.content.ContentValues.TAG
import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModel
import cn.edu.tsinghua.thss.cercis.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor() : ViewModel() {
    /**
     * This is simply a placeholder class, for that navigation operations cannot be directly done
     * through view model, and that indirectly doing the operation is dumb.
     */
}
