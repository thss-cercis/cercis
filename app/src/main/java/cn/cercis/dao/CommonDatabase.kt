package cn.cercis.dao

import androidx.room.*
import cn.cercis.entity.LoginHistory
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [LoginHistory::class],
    version = 1,
    exportSchema = false
)
abstract class CommonDatabase : RoomDatabase() {
    abstract fun loginHistoryDao(): LoginHistoryDao
}

@Dao
interface LoginHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveLoginHistory(loginHistory: LoginHistory)

    @Query("SELECT * FROM loginHistory")
    fun loadLoginHistoryList(): Flow<List<LoginHistory>>
}
