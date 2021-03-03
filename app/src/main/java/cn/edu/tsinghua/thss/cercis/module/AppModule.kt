package cn.edu.tsinghua.thss.cercis.module

import cn.edu.tsinghua.thss.cercis.Constants
import cn.edu.tsinghua.thss.cercis.api.CercisHttpService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.net.CookieManager
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideBaseUrl() = Constants.URL_BASE

    @Singleton
    @Provides
    fun provideOkHttpClient() = run {
        val cookieManager = CookieManager()
        val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .cookieJar(JavaNetCookieJar(cookieManager))
                .build()
        okHttpClient
    }

    @Singleton
    @Provides
    fun provideRetrofit(okHttpClient: OkHttpClient, baseUrl: String): Retrofit = Retrofit.Builder()
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .baseUrl(Constants.URL_BASE)
            .build()

    @Provides
    @Singleton
    fun provideCercisHttpService(retrofit: Retrofit): CercisHttpService = retrofit.create(CercisHttpService::class.java)

}