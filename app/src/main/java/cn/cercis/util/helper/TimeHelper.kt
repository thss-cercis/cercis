package cn.cercis.util.helper

import android.util.Log
import cn.cercis.common.Timestamp
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.ToJson
import org.joda.time.DateTime
import kotlin.annotation.AnnotationRetention.RUNTIME

fun String.toTimestamp(): Timestamp {
    return try {
        DateTime.parse(this).millis
    } catch (ignore: Exception) {
        Log.d("toTimestamp", "Malformed time string: $this")
        0L
    }
}

@Retention(RUNTIME)
@JsonQualifier
annotation class TimeString

class TimeStringAdapter {
    @ToJson
    fun toJson(@TimeString timestamp: Timestamp): String {
        return DateTime(timestamp).toString()
    }

    @FromJson
    @TimeString
    fun fromJson(timeString: String): Timestamp {
        return timeString.toTimestamp()
    }
}
