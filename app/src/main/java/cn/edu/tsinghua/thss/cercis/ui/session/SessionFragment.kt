package cn.edu.tsinghua.thss.cercis.ui.session

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import cn.edu.tsinghua.thss.cercis.R
import cn.edu.tsinghua.thss.cercis.databinding.ChatItemBinding
import cn.edu.tsinghua.thss.cercis.databinding.LayoutSessionBinding
import cn.edu.tsinghua.thss.cercis.entity.Message
import cn.edu.tsinghua.thss.cercis.util.ChatId
import cn.edu.tsinghua.thss.cercis.viewmodel.MessageViewModel
import cn.edu.tsinghua.thss.cercis.viewmodel.SessionViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SessionFragment : Fragment() {
    private val sessionViewModel: SessionViewModel by viewModels()

    companion object {
        fun navDirection(chatId: ChatId): NavDirections {
            return object : NavDirections {
                override fun getActionId(): Int {
                    return R.id.action_to_sessionFragment
                }

                override fun getArguments(): Bundle {
                    return Bundle().apply {
                        putLong("chatId", chatId)
                    }
                }
            }
        }
    }

    private class MessageViewHolder(
            val binding: ChatItemBinding
    ) : RecyclerView.ViewHolder(binding.root)

    private val messageListAdapter = object : RecyclerView.Adapter<MessageViewHolder>() {
        init {
            setHasStableIds(true)
        }

        private val messageList = ArrayList<Message>()

        fun updateMessages(messages: List<Message>) {
            messageList.clear()
            messageList.addAll(messages)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = ChatItemBinding.inflate(inflater, parent, false)
            return MessageViewHolder(binding)
        }

        override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
            val msg = messageList[position];
            holder.binding.viewModel = MessageViewModel(
                    side = sessionViewModel.side(msg.senderId),
                    message = msg,
            )
            holder.binding.executePendingBindings()
        }

        override fun getItemCount(): Int {
            return messageList.size
        }

        override fun getItemId(position: Int): Long {
            return messageList[position].chatId
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = LayoutSessionBinding.inflate(inflater, container, false)
        binding.viewModel = sessionViewModel
        binding.executePendingBindings()
        sessionViewModel.messageListDataSource.observe(viewLifecycleOwner, {
            it?.let {
                messageListAdapter.updateMessages(it)
            }
        })
        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        return binding.root
    }
}