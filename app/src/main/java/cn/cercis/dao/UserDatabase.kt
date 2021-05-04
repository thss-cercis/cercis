package cn.cercis.dao

import androidx.room.*
import cn.cercis.entity.FriendEntry
import cn.cercis.entity.User
import cn.cercis.entity.UserDetail
import cn.cercis.util.UserId
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [User::class, FriendEntry::class, UserDetail::class],
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

    fun insertUserWithTimestamp(vararg users: User) {
        insertUser(*((users.map { it.copy(updated = System.currentTimeMillis()) }.toTypedArray())))
    }

    @Query("SELECT * FROM user")
    fun loadAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM user WHERE id = :userId")
    fun loadUser(userId: UserId): Flow<User>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFriend(vararg friends: FriendEntry)

    @Delete
    fun deleteFriend(vararg friends: FriendEntry)

    @Query("SELECT * FROM friendEntry")
    fun loadAllFriendEntries(): Flow<Array<FriendEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveUserDetail(userDetail: UserDetail)

    @Query("SELECT * FROM userDetail")
    fun loadUserDetail(): Flow<UserDetail?>
}