package cn.cercis.util

import android.content.Context
import cn.cercis.R
import cn.cercis.common.MediaType
import cn.cercis.entity.Activity
import cn.cercis.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import java.text.SimpleDateFormat
import java.util.*
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
            text = when {
                Random.nextInt(3) == 0 -> ""
                else -> context.getString(R.string.lorem_ipsum)
            },
            mediaTypeCode = Random.nextInt(3) / 2,
            publishedAt = System.currentTimeMillis() - Random.nextLong(0, 3600 * 1000),
        )
    }
}

fun getCurrentTimeString(): String {
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
    dateFormat.timeZone = TimeZone.getTimeZone("Asia/Shanghai")
    return dateFormat.format(Date())
}

fun getFakeMediaUrlList(mediaType: MediaType) = when(mediaType) {
    MediaType.IMAGE -> List(Random.nextInt(0, 9)) { "" }
    MediaType.VIDEO -> listOf(getString(R.string.activity_sample_video_url))
}
