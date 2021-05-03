package cn.cercis.ui.contacts;

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import cn.cercis.databinding.FragmentFriendRequestListBinding
import cn.cercis.viewmodel.FriendRequestListViewModel
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
class FriendRequestListFragment : Fragment() {
    private val friendRequestListViewModel: FriendRequestListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentFriendRequestListBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = friendRequestListViewModel

        return binding.root
    }
}
