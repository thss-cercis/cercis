package cn.cercis.dao

import androidx.room.*
import cn.cercis.common.UserId
import cn.cercis.entity.User
import cn.cercis.entity.UserDetail
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [User::class, UserDetail::class],
    version = 1,
    exportSchema = false
)
abstract class UserDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
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

    @Query("SELECT * FROM user")
    fun loadUserList(): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveUserDetail(userDetail: UserDetail)

    @Query("SELECT * FROM userDetail")
    fun loadUserDetail(): Flow<UserDetail?>
}
