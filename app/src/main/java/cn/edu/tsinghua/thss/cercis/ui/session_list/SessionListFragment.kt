package cn.edu.tsinghua.thss.cercis.ui.session_list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import cn.edu.tsinghua.thss.cercis.databinding.FragmentSessionListBinding
import cn.edu.tsinghua.thss.cercis.databinding.MessageSessionListItemBinding
import cn.edu.tsinghua.thss.cercis.ui.session.SessionFragment
import cn.edu.tsinghua.thss.cercis.util.ChatId
import cn.edu.tsinghua.thss.cercis.util.doDetailNavigation
import cn.edu.tsinghua.thss.cercis.viewmodel.SessionListItemData
import cn.edu.tsinghua.thss.cercis.viewmodel.SessionListViewModel

class SessionListFragment : Fragment() {
    private val sessionListViewModel: SessionListViewModel by activityViewModels()
    inner class SessionListAdapter : Adapter<SessionListAdapter.ChatSessionViewHolder>() {
        init {
            setHasStableIds(true)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatSessionViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = MessageSessionListItemBinding.inflate(inflater, parent, false)
            return ChatSessionViewHolder(binding = binding)
        }

        override fun getItemCount(): Int {
            return sessions.size
        }

        override fun onBindViewHolder(holder: ChatSessionViewHolder, position: Int) {
            holder.binding.viewModel = sessions[position]
            holder.binding.executePendingBindings()
        }

        override fun getItemId(position: Int): Long {
            return sessions[position].sessionId
        }

        fun getChatId(position: Int): ChatId {
            return sessions[position].sessionId
        }

        /**
         * Replaces all data within the message session adapter, but does not notify the changes.
         *
         * To update the view, call [Adapter.notifyItemChanged] and other similar functions to inform
         * the data changes.
         */
        fun replaceDataWithoutNotify(list: List<SessionListItemData>) {
            sessions.clear()
            sessions.addAll(list)
        }

        private val sessions = ArrayList<SessionListItemData>()

        inner class ChatSessionViewHolder(
                val binding: MessageSessionListItemBinding,
        ) : RecyclerView.ViewHolder(binding.root) {
            init {
                itemView.setOnClickListener {
                    doDetailNavigation(SessionFragment.navDirection(getChatId(absoluteAdapterPosition)))
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentSessionListBinding.inflate(inflater, container, false)
        binding.viewModel = sessionListViewModel
        binding.lifecycleOwner = viewLifecycleOwner
        // bind refresh listener
        binding.scrollRefreshLayout.setOnRefreshListener {
            sessionListViewModel.onRefreshListener()
            binding.scrollRefreshLayout.isRefreshing = false
        }
        // bind item click listener
        // todo: use real data and somehow refactor this
        val adapter = SessionListAdapter()
        binding.sessionListView.adapter = adapter
        sessionListViewModel.sessions.observe(viewLifecycleOwner) {
            it?.let {
                adapter.replaceDataWithoutNotify(it)
                adapter.notifyDataSetChanged()
            }
        }
        return binding.root
    }
}