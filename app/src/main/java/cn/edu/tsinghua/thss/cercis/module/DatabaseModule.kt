package cn.edu.tsinghua.thss.cercis.module

import android.content.Context
import androidx.room.Room
import cn.edu.tsinghua.thss.cercis.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {
    @Provides
    @Singleton
    fun provideUserDatabase(@ApplicationContext context: Context): UserDatabase =
            Room.databaseBuilder(
                    context,
                    UserDatabase::class.java,
                    "users"
            ).build()

    @Provides
    @Singleton
    fun provideMessageDatabase(@ApplicationContext context: Context): MessageDatabase =
            Room.databaseBuilder(
                    context,
                    MessageDatabase::class.java,
                    "messages"
            ).build()

    @Provides
    @Singleton
    fun providesUserDao(userDatabase: UserDatabase): UserDao = userDatabase.userDao()

    @Provides
    @Singleton
    fun providesMessageDao(messageDatabase: MessageDatabase): MessageDao = messageDatabase.MessageDao()

    @Provides
    @Singleton
    fun providesChatDao(messageDatabase: MessageDatabase): ChatDao = messageDatabase.ChatDao()
}