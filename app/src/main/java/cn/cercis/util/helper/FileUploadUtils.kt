package cn.cercis.util.helper

import android.net.Uri
import android.util.Log
import androidx.core.net.toFile
import cn.cercis.common.LOG_TAG
import cn.cercis.http.CercisHttpService
import cn.cercis.util.resource.NetworkResponse
import com.qiniu.android.storage.UploadManager
import com.qiniu.android.storage.UploadOptions
import kotlinx.coroutines.Job
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileUploadUtils @Inject constructor(
    private val httpService: CercisHttpService,
    private val qiniuUploadManager: UploadManager,
) {
    /**
     * Uploads a file and gets the result key.
     */
    suspend fun uploadFile(uri: Uri): NetworkResponse<String> {
        val tokenRes = httpService.getUploadToken()
        if (tokenRes !is NetworkResponse.Success) {
            return tokenRes.use { "" }
        }
        val token = tokenRes.data.uploadToken
        val job = Job()
        var response: NetworkResponse<String>? = null
        qiniuUploadManager.put(
            uri.toFile(),
            null,
            token,
            { _, info, qiniuRes ->
                Log.d(LOG_TAG, "$qiniuRes")
                response = when {
                    info.isOK -> {
                        NetworkResponse.Success(qiniuRes.getString("key"))
                    }
                    info.isNetworkBroken -> {
                        NetworkResponse.NetworkError(info.error ?: "")
                    }
                    else -> {
                        NetworkResponse.Reject(info.statusCode, info.error ?: "")
                    }
                }
                job.complete()
            },
            UploadOptions(null, null, false, null,
                { !job.isActive }))
        job.join()
        return response!!
    }
}