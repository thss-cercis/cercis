package cn.cercis.module

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import cn.cercis.Constants
import cn.cercis.R
import cn.cercis.http.CercisHttpService
import cn.cercis.http.EmptyPayload
import cn.cercis.http.PayloadResponseBody
import cn.cercis.util.HttpStatusCode
import cn.cercis.util.LOG_TAG
import cn.cercis.util.NetworkResponse
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import com.squareup.moshi.Types
import com.squareup.moshi.rawType
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @BaseUrl
    fun provideBaseUrl() = Constants.URL_BASE

    /**
     * This LiveData is set to false when 401 received in [provideOkHttpClient].
     * It sets [cn.edu.tsinghua.thss.cercis.viewmodel.LoginViewModel.loggedIn]
     */
    @Singleton
    @Provides
    @AuthorizedEvent
    fun provideAuthorizedEvent() = MutableLiveData<Boolean?>(null)

    @Singleton
    @Provides
    fun provideOkHttpClient(
        @AuthorizedEvent authorized: MutableLiveData<Boolean?>,
        @ApplicationContext context: Context,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .cookieJar(PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(context)))
            .addInterceptor { chain ->
                val request = chain.request()
                try {
                    val response = chain.proceed(request)
                    when {
                        response.code == HttpStatusCode.StatusUnauthorized -> {
                            authorized.postValue(false)
                            Log.d(LOG_TAG, "401 detected! ${authorized.hashCode()}")
                            Response.Builder()
                                .request(request)
                                .protocol(Protocol.HTTP_1_1)
                                .code(HttpStatusCode.StatusOK)
                                .message(context.getString(R.string.error_authorization))
                                .body(context.getString(R.string.error_authorization)
                                    .toResponseBody(ServerErrorMediaType))
                                .build()
                        }
                        response.code >= HttpStatusCode.StatusBadRequest -> {
                            response.newBuilder().code(HttpStatusCode.StatusOK).build()
                        }
                        else -> {
                            response
                        }
                    }
                } catch (e: Exception) {
                    Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(HttpStatusCode.StatusOK)
                        .message(e.message!!)
                        .body(context.getString(R.string.error_network_exception)
                            .toResponseBody(ServerErrorMediaType))
                        .build()
                }
            }
            .build()
    }

    @Singleton
    @Provides
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        @BaseUrl baseUrl: String,
        @ApplicationContext context: Context,
    ): Retrofit {
        return Retrofit.Builder()
            .client(okHttpClient)
            .addConverterFactory(object : Converter.Factory() {
                val moshi = MoshiConverterFactory.create()
                val serverErrorMsg = context.getString(R.string.error_server_error)

                override fun responseBodyConverter(
                    type: Type,
                    annotations: Array<Annotation?>,
                    retrofit: Retrofit,
                ): Converter<ResponseBody?, *>? {
                    if (type.rawType != NetworkResponse::class.java) {
                        return moshi.responseBodyConverter(type, annotations, retrofit)
                    }
                    val responseType = (type as ParameterizedType).actualTypeArguments[0]
                    val responseBodyConverter = moshi.responseBodyConverter(
                        Types.newParameterizedType(
                            PayloadResponseBody::class.java,
                            responseType
                        ),
                        annotations,
                        retrofit
                    )

                    return Converter<ResponseBody?, NetworkResponse<Any>> {
                        it ?: return@Converter null
                        if (it.contentType()?.subtype.equals(ServerErrorMediaType.subtype)) {
                            val errorMsg = it.string()
                            return@Converter NetworkResponse.NetworkError(errorMsg)
                        }
                        try {
                            val value = responseBodyConverter?.convert(it)
                                ?: return@Converter NetworkResponse.NetworkError(serverErrorMsg)
                            val body = value as PayloadResponseBody<*>
                            if (!body.successful) {
                                return@Converter NetworkResponse.Reject(body.code, body.msg)
                            }
                            if (body.payload != null || responseType.rawType == EmptyPayload::class.java) {
                                NetworkResponse.Success(body.payload ?: EmptyPayload())
                            } else {
                                NetworkResponse.NetworkError(serverErrorMsg)
                            }
                        } catch (e: Exception) {
                            Log.d(LOG_TAG, e.stackTraceToString())
                            NetworkResponse.NetworkError(serverErrorMsg)
                        }
                    }
                }

                override fun requestBodyConverter(
                    type: Type,
                    parameterAnnotations: Array<Annotation>,
                    methodAnnotations: Array<Annotation>,
                    retrofit: Retrofit,
                ): Converter<*, RequestBody>? {
                    return moshi.requestBodyConverter(
                        type,
                        parameterAnnotations,
                        methodAnnotations,
                        retrofit
                    )
                }
            })
            .baseUrl(baseUrl)
            .build()
    }

    @Singleton
    @Provides
    fun provideCercisHttpService(retrofit: Retrofit): CercisHttpService =
        retrofit.create(CercisHttpService::class.java)

    val ServerErrorMediaType = "application/server_error".toMediaType()
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthorizedEvent

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BaseUrl