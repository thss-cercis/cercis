package cn.edu.tsinghua.thss.cercis.module

import android.content.Context
import androidx.room.Room
import cn.edu.tsinghua.thss.cercis.dao.CommonDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CommonDatabaseModule {
    @Provides
    @Singleton
    fun providesCommonDatabase(@ApplicationContext context: Context): CommonDatabase =
        Room.databaseBuilder(
            context,
            CommonDatabase::class.java,
            "common"
        ).build()

    @Provides
    @Singleton
    fun providesLoginHistoryDao(commonDatabase: CommonDatabase) = commonDatabase.loginHistoryDao()
}
