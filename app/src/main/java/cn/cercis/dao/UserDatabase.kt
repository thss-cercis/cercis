package cn.cercis.dao

import androidx.room.*
import cn.cercis.common.UserId
import cn.cercis.entity.*
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [User::class, UserDetail::class, FriendEntry::class, FriendRequest::class],
    version = 1,
    exportSchema = false
)
abstract class UserDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    abstract fun friendDao(): FriendDao
}

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveUser(vararg users: User)

    fun saveUserList(users: List<User>) {
        saveUser(*users.toTypedArray())
    }

    @Query("SELECT * FROM user WHERE id = :userId")
    fun loadUser(userId: UserId): Flow<User>

    @Query("SELECT * FROM user WHERE id = :userId")
    fun loadUserOnce(userId: UserId): User?

    @Query("SELECT * FROM user")
    fun loadUserList(): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveUserDetail(userDetail: UserDetail)

    @Query("SELECT * FROM userDetail")
    fun loadUserDetail(): Flow<UserDetail?>

    @Query("SELECT * FROM user WHERE id IN (:userIds)")
    fun loadUsers(userIds: List<UserId>): Flow<List<User>>

    @Query("SELECT * FROM user WHERE id IN (:userIds)")
    suspend fun loadUsersOnce(userIds: List<UserId>): List<User>
}

@Dao
interface FriendDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveFriend(vararg friends: FriendEntry)

    @Transaction
    fun replaceFriendList(friends: List<FriendEntry>) {
        deleteAllFriends()
        saveFriend(*friends.toTypedArray())
    }

    @Query("DELETE FROM friendEntry")
    fun deleteAllFriends()

    @Delete
    fun deleteFriend(vararg friends: FriendEntry)

    @Query("SELECT * FROM friendEntry")
    fun loadFriendList(): Flow<List<FriendEntry>>

    @Query("SELECT * FROM friendEntry WHERE friendUserId = :friendUserId")
    fun loadFriendEntry(friendUserId: UserId): Flow<FriendEntry?>

    @Query("""SELECT friendUserId, displayName, nickname, avatar, bio
        FROM friendEntry LEFT OUTER JOIN user on friendEntry.friendUserId = user.id""")
    fun loadFriendDisplayList(): Flow<List<FriendUser>>

    @Query("SELECT friendUserId FROM friendEntry WHERE NOT EXISTS (SELECT id FROM user WHERE id = friendUserId)")
    fun unloadedUsers(): Flow<List<UserId>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveFriendRequest(vararg friendRequests: FriendRequest)

    fun saveFriendRequestList(friendRequests: List<FriendRequest>) {
        saveFriendRequest(*friendRequests.toTypedArray())
    }

    @Query("SELECT * FROM friendRequest")
    fun loadFriendRequestList(): Flow<List<FriendRequest>>

    @Query("SELECT * FROM friendRequest WHERE fromId = :userId")
    fun loadFriendRequestSentList(userId: UserId): Flow<List<FriendRequest>>

    @Query("SELECT * FROM friendRequest WHERE toId = :userId")
    fun loadFriendRequestReceivedList(userId: UserId): Flow<List<FriendRequest>>
}
