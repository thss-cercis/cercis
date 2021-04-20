package cn.edu.tsinghua.thss.cercis.viewmodel

import android.os.Bundle
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.navigation.NavDirections
import cn.edu.tsinghua.thss.cercis.MainActivity
import cn.edu.tsinghua.thss.cercis.R
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor() : ViewModel() {
    // this initial value is related to the one in [master_nav_graph]
    val detailHasNavigationDestination = MutableLiveData(false)
    val masterVisible = Transformations.map(detailHasNavigationDestination) {
        when (it) {
            null, false -> View.VISIBLE
            else -> View.GONE
        }
    }
    val detailVisible = Transformations.map(detailHasNavigationDestination) {
        when (it) {
            true -> View.VISIBLE
            else -> View.GONE
        }
    }
}
