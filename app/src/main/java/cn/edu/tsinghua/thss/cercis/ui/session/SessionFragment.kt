package cn.edu.tsinghua.thss.cercis.ui.session

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import cn.edu.tsinghua.thss.cercis.R
import cn.edu.tsinghua.thss.cercis.dao.User
import cn.edu.tsinghua.thss.cercis.databinding.*
import cn.edu.tsinghua.thss.cercis.entity.Chat
import cn.edu.tsinghua.thss.cercis.entity.ChatType.CHAT_MULTIPLE
import cn.edu.tsinghua.thss.cercis.entity.ChatType.CHAT_SINGLE
import cn.edu.tsinghua.thss.cercis.entity.Message
import cn.edu.tsinghua.thss.cercis.util.ChatId
import cn.edu.tsinghua.thss.cercis.util.Resource
import cn.edu.tsinghua.thss.cercis.util.UserId
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

        const val OtherWithIcon = 1
        const val OtherWithoutIcon = 2
        const val SelfWithIcon = 3
        const val SelfWithoutIcon = 4
    }

    private class MessageViewHolder(
            val binding: ViewDataBinding
    ) : RecyclerView.ViewHolder(binding.root)

    private val messageListAdapter = object : RecyclerView.Adapter<MessageViewHolder>() {
        init {
            setHasStableIds(true)
        }

        private val messageList = ArrayList<Message>()
        private var chatInfo: Chat? = null

        fun updateMessages(messages: List<Message>) {
            messageList.clear()
            messageList.addAll(messages)
            notifyDataSetChanged()
        }

        fun updateChatInfo(chat: Chat) {
            chatInfo = chat
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = when (viewType) {
                OtherWithIcon -> ChatItemOtherWithIconBinding.inflate(inflater, parent, false)
                OtherWithoutIcon -> ChatItemOtherWithoutIconBinding.inflate(inflater, parent, false)
                SelfWithIcon -> ChatItemSelfWithIconBinding.inflate(inflater, parent, false)
                /* SelfWithoutIcon, */ else -> ChatItemSelfWithoutIconBinding.inflate(inflater, parent, false)
            }
            return MessageViewHolder(binding)
        }

        override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
//            val msg = messageList[position]
//            val vm = MessageViewModel(
//                    message = msg,
//                    user = sessionViewModel.
//            )
//            when (getItemViewType(position)) {
//                OtherWithIcon -> (holder.binding as ChatItemOtherWithIconBinding).message = msg
//                OtherWithoutIcon -> (holder.binding as ChatItemOtherWithoutIconBinding).message = msg
//                SelfWithIcon -> (holder.binding as ChatItemSelfWithIconBinding).message = msg
//                /* SelfWithoutIcon, */ else -> (holder.binding as ChatItemSelfWithoutIconBinding).message = msg
//            }
            holder.binding.executePendingBindings()
        }

        override fun getItemCount(): Int {
            return messageList.size
        }

        override fun getItemId(position: Int): Long {
            return messageList[position].chatId
        }

        override fun getItemViewType(position: Int): Int {
            return when(sessionViewModel.side(messageList[position].senderId)) {
                SessionViewModel.Side.SELF -> when(chatInfo?.type) {
                    CHAT_MULTIPLE -> SelfWithIcon
                    null, CHAT_SINGLE -> SelfWithoutIcon
                    else -> SelfWithoutIcon
                }
                SessionViewModel.Side.OTHER -> when(chatInfo?.type) {
                    CHAT_MULTIPLE -> OtherWithIcon
                    null, CHAT_SINGLE -> OtherWithoutIcon
                    else -> OtherWithoutIcon
                }
            }
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
        sessionViewModel.chat.observe(viewLifecycleOwner, {
            it?.let {
                messageListAdapter.updateChatInfo(it)
            }
        })
        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        return binding.root
    }
}