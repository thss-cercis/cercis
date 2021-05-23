package cn.cercis.dao

import androidx.room.*
import cn.cercis.common.ActivityId
import cn.cercis.entity.Activity
import cn.cercis.entity.Comment
import cn.cercis.entity.Medium
import kotlinx.coroutines.flow.Flow


@Database(
    entities = [Activity::class, Medium::class, Comment::class],
    version = 1,
    exportSchema = false
)
abstract class ActivityDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
}

//class UserWithRepos {
//    @Embedded
//    var user: User? = null
//
//    @Relation(parentColumn = "id", entityColumn = "userId")
//    var repoList: List<Repo>? = null
//}

data class EntireActivity(
    @Embedded val activity: Activity,
    @Relation(parentColumn = "id", entityColumn = "activityId")
    val media: List<Medium>,
    @Relation(parentColumn = "id", entityColumn = "activityId")
    val comments: List<Comment>,
)

@Dao
interface ActivityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveActivity(vararg activities: Activity)

    fun saveActivityList(activities: List<Activity>) {
        saveActivity(*activities.toTypedArray())
    }

    @Query("SELECT * FROM activity WHERE id = :activityId")
    fun loadEntireActivity(activityId: ActivityId): Flow<EntireActivity>

    @Query("SELECT * FROM activity")
    fun loadEntireActivityList(): Flow<List<EntireActivity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveMedium(vararg activities: Medium)

    fun saveMediumList(activities: List<Medium>) {
        saveMedium(*activities.toTypedArray())
    }

    @Query("SELECT * FROM medium WHERE activityId = :activityId")
    fun loadMediumList(activityId: ActivityId): Flow<List<Medium>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveComment(vararg activities: Comment)

    fun saveCommentList(activities: List<Comment>) {
        saveComment(*activities.toTypedArray())
    }

    @Query("SELECT * FROM comment WHERE activityId = :activityId")
    fun loadCommentList(activityId: ActivityId): Flow<List<Comment>>

    @Transaction
    fun saveEntireActivityList(activities: List<Activity>, media: List<Medium>, comments: List<Comment>) {
        saveActivityList(activities)
        saveMediumList(media)
        saveCommentList(comments)
    }
}
