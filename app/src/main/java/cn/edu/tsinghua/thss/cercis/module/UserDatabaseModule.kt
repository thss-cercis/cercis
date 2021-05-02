package cn.edu.tsinghua.thss.cercis.module

import android.content.Context
import androidx.room.Room
import cn.edu.tsinghua.thss.cercis.dao.*
import cn.edu.tsinghua.thss.cercis.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped

@Module
@InstallIn(ActivityRetainedComponent::class)
class UserDatabaseModule {
    @Provides
    @ActivityRetainedScoped
    fun provideUserDatabase(
        @ApplicationContext context: Context,
        authRepository: AuthRepository,
    ): UserDatabase =
        Room.databaseBuilder(
            context,
            UserDatabase::class.java,
            "${authRepository.userId}_users"
        ).build()

    @Provides
    @ActivityRetainedScoped
    fun provideMessageDatabase(
        @ApplicationContext context: Context,
        authRepository: AuthRepository,
    ): MessageDatabase =
        Room.databaseBuilder(
            context,
            MessageDatabase::class.java,
            "${authRepository.userId}_messages"
        ).build()

    @Provides
    @ActivityRetainedScoped
    fun providesUserDao(userDatabase: UserDatabase): UserDao = userDatabase.userDao()

    @Provides
    @ActivityRetainedScoped
    fun providesMessageDao(messageDatabase: MessageDatabase): MessageDao =
        messageDatabase.MessageDao()

    @Provides
    @ActivityRetainedScoped
    fun providesChatDao(messageDatabase: MessageDatabase): ChatDao = messageDatabase.ChatDao()
}
