package cn.cercis.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import cn.cercis.common.LOG_TAG
import cn.cercis.common.SEARCH_PAGE_SIZE
import cn.cercis.common.UserId
import cn.cercis.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import okhttp3.internal.toLongOrDefault
import javax.inject.Inject

@FlowPreview
@HiltViewModel
@ExperimentalCoroutinesApi
class SearchViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {
    enum class SearchType {
        USER_ID, MOBILE, NICKNAME
    }

    data class Search(
        val userId: UserId? = null,
        val mobile: String? = null,
        val nickname: String? = null,
        val searchType: SearchType?,
    )

    val searchText = MutableLiveData("")
    val forceSearchType = MutableLiveData<SearchType?>(null)
    val searchType = MutableLiveData<SearchType>(null)
    private val searchRequestFlow = MutableStateFlow(Search(searchType = null))
    val searchResultFlow = searchRequestFlow.flatMapLatest {
        Log.d(LOG_TAG, "Received $it")
        if (it.searchType == null) {
            MutableStateFlow(PagingData.empty())
        } else {
            Pager(PagingConfig(pageSize = SEARCH_PAGE_SIZE)) {
                userRepository.searchUserPagingSource(it.userId, it.mobile, it.nickname)
            }.flow.cachedIn(viewModelScope)
        }
    }

    fun clearSearch() {
        searchRequestFlow.value = Search(searchType = null)
    }

    fun doSearch() {
        var userId: UserId? = null
        var mobile: String? = null
        var nickname: String? = null
        val currentSearchType = forceSearchType.value
        val text = searchText.value ?: ""
        // clear previous settings
        forceSearchType.postValue(null)
        // guess a search type
        val guessedSearchType = arrayListOf<SearchType>()
        if (text.matches(Regex("""\d{11}"""))) {
            guessedSearchType.add(SearchType.MOBILE)
        }
        if (text.matches(Regex("""\d+"""))) {
            guessedSearchType.add(SearchType.USER_ID)
        }
        guessedSearchType.add(SearchType.NICKNAME)
        // choose the specified search type or first guessed type
        (currentSearchType?.takeIf { it in guessedSearchType } ?: guessedSearchType[0]).let {
            when (it) {
                SearchType.USER_ID -> userId = text.toLongOrDefault(0)
                SearchType.MOBILE -> mobile = text
                SearchType.NICKNAME -> nickname = text
            }
            searchType.postValue(it)
            searchRequestFlow.value = Search(
                userId, mobile, nickname, it
            )
        }
    }
}
