package cn.edu.tsinghua.thss.cercis.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import cn.edu.tsinghua.thss.cercis.api.CercisHttpService
import cn.edu.tsinghua.thss.cercis.dao.CurrentUser
import cn.edu.tsinghua.thss.cercis.dao.User
import cn.edu.tsinghua.thss.cercis.dao.UserDao
import cn.edu.tsinghua.thss.cercis.module.AuthorizedLiveEvent
import cn.edu.tsinghua.thss.cercis.util.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    @ApplicationContext val context: Context,
    @AuthorizedLiveEvent val authorized: SingleLiveEvent<Boolean?>,
    val httpService: CercisHttpService,
    val userDao: UserDao,
) {
    private val sharedPreferences: SharedPreferences = run {
        context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    }
    private val userLiveDataCache = HashMap<UserId, NetworkBoundResource<User, User>>()

    /**
     * Checks if a user is logged in.
     *
     * Setting this value would trigger a write-back to shared preferences.
     */
    val loggedIn = run {
        val liveData = MutableLiveData(sharedPreferences.getBoolean("logged_in", false))
        Log.d(LOG_TAG, "read logged_in from preferences: ${liveData.value}")
        liveData.observeForever { value ->
            Log.d(LOG_TAG, "write logged_in: $value")
            sharedPreferences.edit().putBoolean("logged_in", value).apply()
        }
        authorized.observeForever { value ->
            if (value != null && value != liveData.value) {
                liveData.postValue(value)
            }
        }
        liveData
    }

    /**
     * Currently logged in user id.
     *
     * -1 if no previous login.
     */
    val currentUserId = run {
        val liveData = MutableLiveData(sharedPreferences.getLong("current_user", -1))
        liveData.observeForever { value ->
            sharedPreferences.edit().putLong("current_user", value).apply()
        }
        liveData
    }

    val currentUsers = run {
        userDao.loadCurrentUsers().asLiveData()
    }

    private val _currentUser: MediatorLiveData<CurrentUser> by lazy {
        val id: UserId = currentUserId.value!!
        val mediatorLiveData = MediatorLiveData<CurrentUser>()
        var liveData = userDao.loadCurrentUser(id).asLiveData()
        mediatorLiveData.addSource(currentUserId) {
            userDao.loadCurrentUser(it)
            mediatorLiveData.removeSource(liveData)
            liveData = userDao.loadCurrentUser(it).asLiveData()
        }
        mediatorLiveData.addSource(liveData) {
            mediatorLiveData.postValue(it)
        }
        mediatorLiveData
    }

    /**
     * Looks up a user with intermediate cache.
     */
    @MainThread
    fun loadUser(userId: UserId): LiveData<Resource<User>> {
        TODO()
    }

    /**
     * Gets the current user.
     *
     * This method is idempotent, with the calling always return the save LiveData instance.
     */
    @MainThread
    fun currentUser(scope: CoroutineScope): LiveData<CurrentUser> {
        scope.launch(Dispatchers.IO) {
            try {
                val response = httpService.userCurrent()
                if (response is NetworkResponse.Success) {
                    val user = response.data
                    if (currentUserId.value != user.id) {
                        currentUserId.postValue(user.id)
                    }
                    userDao.insertCurrentUser(user)
                }
            } catch (ignore: Throwable) {
                /* do nothing */
            }
        }
        return _currentUser
    }
}
