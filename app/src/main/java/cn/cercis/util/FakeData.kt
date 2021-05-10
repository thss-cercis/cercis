package cn.cercis.util

import android.content.Context
import cn.cercis.R
import cn.cercis.entity.Activity
import cn.cercis.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject
import kotlin.random.Random

@ActivityRetainedScoped
class FakeData @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
) {
    fun getFakeActivityList(range: LongRange) = range.map {
        Activity(
            id = 0,
            userId = authRepository.currentUserId,
            publishedAt = 0,
            type = 0,
            text = context.getString(R.string.lorem_ipsum),
            imageUrls = List(Random.nextInt(1, 9)) { "" },
            videoUrl = null,
            likedUserNames = listOf(),
        )
    }
}