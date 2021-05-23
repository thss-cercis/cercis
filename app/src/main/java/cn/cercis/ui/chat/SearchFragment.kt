package cn.cercis.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import cn.cercis.R
import cn.cercis.databinding.CommonListItemBinding
import cn.cercis.databinding.FragmentSearchBinding
import cn.cercis.entity.User
import cn.cercis.http.WrappedSearchUserPayload.UserSearchResult
import cn.cercis.util.helper.DataBindingViewHolder
import cn.cercis.util.helper.closeIme
import cn.cercis.util.helper.requireMainActivity
import cn.cercis.viewmodel.CommonListItemData
import cn.cercis.viewmodel.SearchViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class SearchFragment : Fragment() {
    val searchViewModel: SearchViewModel by viewModels()

    inner class SearchResultAdapter :
        PagingDataAdapter<UserSearchResult, DataBindingViewHolder<CommonListItemBinding>>(
            object : DiffUtil.ItemCallback<UserSearchResult>() {
                override fun areItemsTheSame(
                    oldItem: UserSearchResult,
                    newItem: UserSearchResult,
                ): Boolean {
                    return oldItem.id == newItem.id
                }

                override fun areContentsTheSame(
                    oldItem: UserSearchResult,
                    newItem: UserSearchResult,
                ): Boolean {
                    return oldItem == newItem
                }
            }
        ) {
        override fun onBindViewHolder(
            holder: DataBindingViewHolder<CommonListItemBinding>,
            position: Int,
        ) {
            holder.binding.apply {
                getItem(position)?.let {
                    data = MutableLiveData(CommonListItemData(
                        ""/* TODO: it.avatar */,
                        it.nickname,
                        getString(R.string.profile_cercis_id).format(it.id)
                    ))
                } ?: run {
                    data = null
                }
                root.setOnClickListener {
                    requireMainActivity().openUserInfo(
                        getItem(position)!!.let {
                            User(it.id, it.nickname, it.mobile, it.avatar, "", 0L)
                        }
                    )
                }
                executePendingBindings()
            }
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): DataBindingViewHolder<CommonListItemBinding> {
            return DataBindingViewHolder(
                CommonListItemBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)
                    .apply {
                        lifecycleOwner = viewLifecycleOwner
                    }
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentSearchBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = searchViewModel
        binding.fragmentSearchCancel.setOnClickListener {
            findNavController().popBackStack()
        }
        val adapter = SearchResultAdapter().apply {
            // TODO add loading animation
            binding.fragmentSearchResultView.adapter = this
        }
        // TODO check if changing to [Dispatchers.IO] causes bugs
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            searchViewModel.searchResultFlow.collectLatest { pagingData ->
                adapter.submitData(pagingData)
            }
        }
        binding.fragmentSearchTextEdit.setOnEditorActionListener { view, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    searchViewModel.doSearch()
                    closeIme()
                    true
                }
                else -> false
            }
        }
        binding.fragmentSearchTextInputLayout.setEndIconOnClickListener {
            searchViewModel.clearSearch()
        }
        return binding.root
    }
}