package cn.edu.tsinghua.thss.cercis.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import cn.edu.tsinghua.thss.cercis.api.*
import cn.edu.tsinghua.thss.cercis.dao.*
import cn.edu.tsinghua.thss.cercis.util.UserId
import dagger.hilt.android.qualifiers.ApplicationContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

class UserRepository @Inject constructor(
        @ApplicationContext val context: Context,
        val httpService: CercisHttpService,
        val UserDao: UserDao,
) {
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    }

    /**
     * Checks if a user is logged in.
     *
     * Setting this value would trigger a write-back to shared preferences.
     */
    val loggedIn by lazy {
        val liveData = MutableLiveData(sharedPreferences.getBoolean("logged_in", false))
        liveData.observeForever { value ->
            sharedPreferences.edit().putBoolean("logged_in", value).apply()
        }
        liveData
    }

    private val currentUserId by lazy {
        val liveData = MutableLiveData(sharedPreferences.getLong("current_user", -1))
        liveData.observeForever { value ->
            sharedPreferences.edit().putLong("current_user", value).apply()
        }
        liveData
    }

    private val _currentUser: MediatorLiveData<CurrentUser?> by lazy {
        val id: UserId = currentUserId.value!!
        val mediatorLiveData = MediatorLiveData<CurrentUser?>()
        var liveData = UserDao.loadCurrentUser(id).asLiveData()
        mediatorLiveData.addSource(currentUserId) {
            UserDao.loadCurrentUser(it)
            mediatorLiveData.removeSource(liveData)
            liveData = UserDao.loadCurrentUser(it).asLiveData()
        }
        mediatorLiveData.addSource(liveData) {
            mediatorLiveData.postValue(it)
        }
        mediatorLiveData
    }

    val currentUser: LiveData<CurrentUser?>
        get() {
            httpService.userCurrent().enqueue(object : Callback<UserCurrentResponse> {
                override fun onResponse(call: Call<UserCurrentResponse>, response: Response<UserCurrentResponse>) {
                    if (!response.authorized) {
                        // todo use a request filter
                        loggedIn.postValue(false)
                    } else if (response.ok) {
                        val user = response.payload
                        if (user != null) {
                            if (currentUserId.value != user.id) {
                                currentUserId.postValue(user.id)
                            }
                            UserDao.insertCurrentUser(user)
                        }
                    }
                }

                override fun onFailure(call: Call<UserCurrentResponse>, t: Throwable) {
                    // todo use a status
                    /* do nothing, use cache */
                }
            })
            return _currentUser
        }
}
