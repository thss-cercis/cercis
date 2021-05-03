package cn.edu.tsinghua.thss.cercis.module

import androidx.room.Room
import cn.edu.tsinghua.thss.cercis.dao.*
import cn.edu.tsinghua.thss.cercis.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped

@Module
@InstallIn(ActivityRetainedComponent::class)
object UserDatabaseModule {
    @Provides
    @ActivityRetainedScoped
    fun provideUserDatabase(authRepository: AuthRepository) =
        Room.databaseBuilder(
            authRepository.context,
            UserDatabase::class.java,
            authRepository.getUserDatabaseAbsolutePath("users")
        ).build()

    @Provides
    @ActivityRetainedScoped
    fun provideMessageDatabase(authRepository: AuthRepository) =
        Room.databaseBuilder(
            authRepository.context,
            MessageDatabase::class.java,
            authRepository.getUserDatabaseAbsolutePath("messages")
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
