package cn.edu.tsinghua.thss.cercis.module

import android.content.Context
import android.util.Log
import cn.edu.tsinghua.thss.cercis.Constants
import cn.edu.tsinghua.thss.cercis.R
import cn.edu.tsinghua.thss.cercis.http.CercisHttpService
import cn.edu.tsinghua.thss.cercis.http.EmptyPayload
import cn.edu.tsinghua.thss.cercis.http.PayloadResponseBody
import cn.edu.tsinghua.thss.cercis.util.HttpStatusCode
import cn.edu.tsinghua.thss.cercis.util.LOG_TAG
import cn.edu.tsinghua.thss.cercis.util.NetworkResponse
import cn.edu.tsinghua.thss.cercis.util.SingleLiveEvent
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import com.squareup.moshi.internal.Util
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
    @AuthorizedLiveEvent
    fun provideAuthorized() = SingleLiveEvent<Boolean?>(null)

    @Singleton
    @Provides
    fun provideOkHttpClient(
        @AuthorizedLiveEvent authorized: SingleLiveEvent<Boolean?>,
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
                    type: Type, annotations: Array<Annotation?>, retrofit: Retrofit,
                ): Converter<ResponseBody?, *>? {
                    if (type.rawType != NetworkResponse::class.java) {
                        return moshi.responseBodyConverter(type, annotations, retrofit)
                    }
                    val respValueType: Type = (type as ParameterizedType).actualTypeArguments[0]
                    val moshiResponseConverter = moshi.responseBodyConverter(
                        Util.ParameterizedTypeImpl(null, PayloadResponseBody::class.java, respValueType),
                        annotations,
                        retrofit
                    )

                    return Converter<ResponseBody?, NetworkResponse<Any>> {
                        it?.let {
                            if (it.contentType()?.subtype.equals(ServerErrorMediaType.subtype)) {
                                val errorMsg = it.string()
                                NetworkResponse.NetworkError(errorMsg)
                            } else {
                                try {
                                    val value = moshiResponseConverter?.convert(it)
                                    if (value == null) {
                                        NetworkResponse.NetworkError(serverErrorMsg)
                                    } else {
                                        val body = value as PayloadResponseBody<*>
                                        if (body.successful) {
                                            if (body.payload != null || respValueType.rawType == EmptyPayload::class.java) {
                                                NetworkResponse.Success(body.payload ?: EmptyPayload())
                                            } else {
                                                NetworkResponse.NetworkError(serverErrorMsg)
                                            }
                                        } else {
                                            NetworkResponse.Reject(body.code, body.msg)
                                        }
                                    }
                                } catch (e: Exception) {
                                    NetworkResponse.NetworkError(serverErrorMsg)
                                }
                            }
                        }
                    }
                }

                override fun requestBodyConverter(
                    type: Type,
                    parameterAnnotations: Array<Annotation>,
                    methodAnnotations: Array<Annotation>,
                    retrofit: Retrofit,
                ): Converter<*, RequestBody>? {
                    return moshi.requestBodyConverter(type,
                        parameterAnnotations,
                        methodAnnotations,
                        retrofit)
                }
            })
            .baseUrl(baseUrl)
            .build()
    }

    @Provides
    @Singleton
    fun provideCercisHttpService(retrofit: Retrofit): CercisHttpService =
        retrofit.create(CercisHttpService::class.java)

    val ServerErrorMediaType = "application/server_error".toMediaType()
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthorizedLiveEvent

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BaseUrl

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CurrentUserId
