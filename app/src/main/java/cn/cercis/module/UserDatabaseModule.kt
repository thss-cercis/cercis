package cn.cercis.module

import androidx.room.Room
import cn.cercis.dao.ActivityDatabase
import cn.cercis.dao.MessageDatabase
import cn.cercis.dao.UserDatabase
import cn.cercis.repository.AuthRepository
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
    fun provideActivityDatabase(authRepository: AuthRepository) =
        Room.databaseBuilder(
            authRepository.context,
            ActivityDatabase::class.java,
            authRepository.getUserDatabaseAbsolutePath("activities")
        ).build()

    @Provides
    @ActivityRetainedScoped
    fun providesUserDao(userDatabase: UserDatabase) = userDatabase.userDao()

    @Provides
    @ActivityRetainedScoped
    fun providesFriendDao(userDatabase: UserDatabase) = userDatabase.friendDao()

    @Provides
    @ActivityRetainedScoped
    fun providesMessageDao(messageDatabase: MessageDatabase) = messageDatabase.messageDao()

    @Provides
    @ActivityRetainedScoped
    fun providesChatDao(messageDatabase: MessageDatabase) = messageDatabase.chatDao()

    @Provides
    @ActivityRetainedScoped
    fun providesChatMemberDao(messageDatabase: MessageDatabase) = messageDatabase.chatMemberDao()

    @Provides
    @ActivityRetainedScoped
    fun providesActivityDao(activityDatabase: ActivityDatabase) = activityDatabase.activityDao()
}
