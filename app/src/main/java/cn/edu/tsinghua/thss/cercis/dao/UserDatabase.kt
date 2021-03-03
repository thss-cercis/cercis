package cn.edu.tsinghua.thss.cercis.dao

import androidx.room.*
import cn.edu.tsinghua.thss.cercis.util.UserId
import kotlinx.coroutines.flow.Flow

@Database(
        entities = [User::class, FriendEntry::class, CurrentUser::class],
        version = 1,
        exportSchema = false
)
abstract class UserDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUser(vararg users: User)

    @Query("SELECT * FROM user")
    fun loadAllUsers(): Flow<Array<User>>

    @Query("SELECT * FROM user WHERE id = :userId")
    fun loadUser(userId: UserId): Flow<User>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFriend(vararg friends: FriendEntry)

    @Delete
    fun deleteFriend(vararg friends: FriendEntry)

    @Query("SELECT * FROM friendEntry")
    fun loadAllFriendEntries(): Flow<Array<FriendEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCurrentUser(vararg currentUsers: CurrentUser)

    @Query("SELECT * FROM currentUser WHERE id = :userId")
    fun loadCurrentUser(userId: UserId): Flow<CurrentUser>
}
