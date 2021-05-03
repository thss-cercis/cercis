package cn.cercis.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject

@ActivityRetainedScoped
class FriendRequestListViewModel @Inject constructor() : ViewModel() {
}