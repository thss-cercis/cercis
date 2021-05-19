package cn.cercis.dao

import androidx.room.*
import cn.cercis.common.UserId
import cn.cercis.entity.FriendEntry
import cn.cercis.entity.FriendRequest
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [FriendEntry::class, FriendRequest::class],
    version = 1,
    exportSchema = false
)
abstract class FriendDatabase : RoomDatabase() {
    abstract fun friendDao(): FriendDao
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
