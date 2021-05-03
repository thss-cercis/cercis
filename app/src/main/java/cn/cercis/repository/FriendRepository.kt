package cn.cercis.repository

import cn.cercis.entity.FriendEntry
import cn.cercis.entity.FriendRequest
import cn.cercis.http.NetworkBoundResource
import cn.cercis.util.ApplyId
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import javax.inject.Inject

@ActivityRetainedScoped
@FlowPreview
@ExperimentalCoroutinesApi
class FriendRepository @Inject constructor() {
    fun getFriendList(): NetworkBoundResource<List<FriendEntry>, List<FriendEntry>> {
        TODO("httpApi")
    }

    fun getFriendRequestList(): NetworkBoundResource<List<FriendRequest>, List<FriendRequest>> {
        TODO("httpApi")
    }

    fun acceptFriendRequest(applyId: ApplyId) {
        TODO("httpApi")
    }

    fun rejectFriendRequest(applyId: ApplyId) {
        TODO("httpApi")
    }
}
