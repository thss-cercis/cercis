package cn.edu.tsinghua.thss.cercis.ui.session_list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import cn.edu.tsinghua.thss.cercis.databinding.LayoutSessionListBinding
import cn.edu.tsinghua.thss.cercis.ui.session.SessionFragment
import cn.edu.tsinghua.thss.cercis.util.doDetailNavigation
import cn.edu.tsinghua.thss.cercis.viewmodel.SessionListViewModel

class SessionListFragment : Fragment() {
    private val sessionListViewModel: SessionListViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = LayoutSessionListBinding.inflate(inflater, container, false)
        binding.viewModel = sessionListViewModel
        binding.lifecycleOwner = viewLifecycleOwner
        // bind refresh listener
        binding.scrollRefreshLayout.setOnRefreshListener {
            sessionListViewModel.onRefreshListener()
            binding.scrollRefreshLayout.isRefreshing = false
        }
        // bind item click listener
        sessionListViewModel.openedSession.observe(viewLifecycleOwner) {
            if (it != null) {
                doDetailNavigation(SessionFragment.navDirection(it))
                sessionListViewModel.openedSession.value = null
            }
        }
        // todo: use real data and somehow refactor this
        binding.sessionListView.adapter = sessionListViewModel.sessionListAdapter
        return binding.root
    }
}