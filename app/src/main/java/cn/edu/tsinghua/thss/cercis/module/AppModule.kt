package cn.edu.tsinghua.thss.cercis.module

import androidx.lifecycle.MutableLiveData
import cn.edu.tsinghua.thss.cercis.Constants
import cn.edu.tsinghua.thss.cercis.api.CercisHttpService
import cn.edu.tsinghua.thss.cercis.util.HttpStatusCode
import cn.edu.tsinghua.thss.cercis.util.SingleLiveEvent
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.net.CookieManager
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
     * This value is only to be used by [cn.edu.tsinghua.thss.cercis.repository.UserRepository]
     * and [provideOkHttpClient].
     *
     * To check login status, please refer to
     * [cn.edu.tsinghua.thss.cercis.repository.UserRepository.loggedIn]
     */
    @Provides
    @AuthorizedLiveEvent
    fun provideAuthorized() = SingleLiveEvent<Boolean?>(null)

    @Singleton
    @Provides
    fun provideOkHttpClient(@AuthorizedLiveEvent authorized: SingleLiveEvent<Boolean?>) = run {
        val cookieManager = CookieManager()
        val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .cookieJar(JavaNetCookieJar(cookieManager))
                .addInterceptor { chain ->
                    val request = chain.request()
                    val response = chain.proceed(request)
                    if (response.code == HttpStatusCode.StatusUnauthorized) {
                        authorized.postValue(false)
                    } else if (response.code >= 400) {
                        return@addInterceptor response.newBuilder().code(200).build()
                    }
                    return@addInterceptor response
                }
                .build()
        okHttpClient
    }

    @Singleton
    @Provides
    fun provideRetrofit(okHttpClient: OkHttpClient, @BaseUrl baseUrl: String): Retrofit {
        val retrofit = Retrofit.Builder()
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create())
                .baseUrl(baseUrl)
                .build()
        return retrofit
    }

    @Provides
    @Singleton
    fun provideCercisHttpService(retrofit: Retrofit): CercisHttpService = retrofit.create(CercisHttpService::class.java)

}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthorizedLiveEvent

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BaseUrl