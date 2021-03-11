package cn.edu.tsinghua.thss.cercis.ui.session_list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import cn.edu.tsinghua.thss.cercis.databinding.LayoutSessionsBinding
import cn.edu.tsinghua.thss.cercis.viewmodel.SessionListViewModel

class SessionListFragment : Fragment() {
    private val sessionListViewModel: SessionListViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = LayoutSessionsBinding.inflate(inflater, container, false)
        return binding.root
    }
}