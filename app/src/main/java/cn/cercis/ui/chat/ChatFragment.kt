package cn.cercis.ui.chat

import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import cn.cercis.R
import cn.cercis.common.LOG_TAG
import cn.cercis.databinding.*
import cn.cercis.entity.MessageLocationContent
import cn.cercis.entity.MessageType
import cn.cercis.entity.asMessageType
import cn.cercis.util.helper.DiffRecyclerViewAdapter
import cn.cercis.util.helper.setCloseImeOnLoseFocus
import cn.cercis.viewmodel.ChatViewModel
import cn.cercis.viewmodel.ChatViewModel.MessageDirection.INCOMING
import cn.cercis.viewmodel.ChatViewModel.MessageDirection.OUTGOING
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class ChatFragment : Fragment() {
    private val chatViewModel: ChatViewModel by viewModels()

    companion object {
        data class MessageViewType(
            val direction: Int,
            val messageType: Int,
        ) {
            val viewType: Int = (messageType.asMessageType().type shl 4) or (direction and 0xf)

            companion object {
                fun fromViewType(viewType: Int): MessageViewType {
                    return MessageViewType(
                        direction = viewType and 0xf,
                        messageType = viewType shr 4,
                    )
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentChatBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = chatViewModel
        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.chatRecyclerView.apply {
            val adapter = DiffRecyclerViewAdapter.getInstance(
                dataSource = chatViewModel.chatMessageList,
                viewLifecycleOwnerSupplier = { viewLifecycleOwner },
                itemIndex = { messageId },
                contentsSameCallback = Objects::equals,
                inflater = { layoutInflater, parent, viewType ->
                    val mvt = MessageViewType.fromViewType(viewType)
                    when (val msgType = mvt.messageType.asMessageType()) {
                        // delete and withdraw has special layouts
                        MessageType.DELETED -> {
                            ChatItemDeletedBinding.inflate(layoutInflater, parent, false)
                        }
                        MessageType.WITHDRAW -> {
                            ChatItemDeletedBinding.inflate(layoutInflater, parent, false)
                        }
                        else -> {
                            val (ret, imageView, textView) = when (mvt.direction) {
                                OUTGOING.type -> ChatItemOutgoingBinding.inflate(
                                    layoutInflater,
                                    parent,
                                    false
                                ).let {
                                    Triple(it, it.chatItemMessageImage, it.chatItemMessageText)
                                }
                                INCOMING.type -> ChatItemIncomingBinding.inflate(
                                    layoutInflater,
                                    parent,
                                    false
                                ).let {
                                    Triple(it, it.chatItemMessageImage, it.chatItemMessageText)
                                }
                                else -> throw IllegalStateException("erroneous view type")
                            }
                            when (msgType) {
                                MessageType.TEXT, MessageType.UNKNOWN -> {
                                    imageView.visibility = View.GONE
                                }
                                MessageType.AUDIO -> {
                                    textView.setCompoundDrawablesWithIntrinsicBounds(
                                        R.drawable.ic_audio_24, 0, 0, 0
                                    )
                                    imageView.visibility = View.GONE
                                }
                                MessageType.IMAGE -> {
                                    textView.visibility = View.GONE
                                }
                                MessageType.VIDEO -> {
                                    ResourcesCompat.getDrawable(
                                        resources,
                                        R.drawable.ic_play_circle_24,
                                        null
                                    )?.let {
                                        imageView.overlay.add(it)
                                    }
                                    textView.visibility = View.GONE
                                }
                                MessageType.LOCATION -> Unit
                                else -> throw IllegalStateException("unexpected message type")
                            }
                            ret
                        }
                    }
                },
                onBindViewHolderWithExecution = { holder, position ->
                    val mvt = MessageViewType.fromViewType(getItemViewType(position))
                    val data = currentList[position]
                    when (val msgType = mvt.messageType.asMessageType()) {
                        // delete and withdraw has special layouts
                        MessageType.DELETED -> Unit
                        MessageType.WITHDRAW -> {
                            (holder.binding as ChatItemWithdrawedBinding)
                                .user = chatViewModel.loadUser(data.senderId)
                        }
                        else -> {
                            val itemBinding = holder.binding
                            val (imageView, textView) = when (mvt.direction) {
                                OUTGOING.type -> (itemBinding as ChatItemOutgoingBinding)
                                    .let {
                                        it.user = chatViewModel.loadUser(data.senderId)
                                        it.chatItemMessageImage to it.chatItemMessageText
                                    }
                                INCOMING.type -> (itemBinding as ChatItemIncomingBinding)
                                    .let {
                                        it.user = chatViewModel.loadUser(data.senderId)
                                        it.chatItemMessageImage to it.chatItemMessageText
                                    }
                                else -> throw IllegalStateException("erroneous view type")
                            }
                            when (msgType) {
                                MessageType.TEXT -> {
                                    textView.text = data.message
                                }
                                MessageType.IMAGE -> {
                                    Glide.with(imageView)
                                        .load(data.message)
                                        .into(imageView)
                                }
                                MessageType.AUDIO -> {
                                    // TODO bind audio playing
                                    textView.text = getString(R.string.message_type_audio)
                                }
                                MessageType.VIDEO -> {
                                    // TODO bind video playing
                                    Glide.with(imageView)
                                        .load(data.message + "")
                                }
                                MessageType.LOCATION -> {
                                    val locationContent =
                                        MessageLocationContent.fromMessageContent(data.message)
                                    imageView.setOnClickListener {
                                        // TODO open location in popup window(?)
                                    }
                                    textView.text = locationContent.description
                                }
                                MessageType.UNKNOWN -> {
                                    textView.text = getString(R.string.message_type_unknown)
                                }
                                else -> throw IllegalStateException("unexpected message type")
                            }
                        }
                    }
                },
                itemViewType = { MessageViewType(chatViewModel.side(senderId).type, type).viewType }
            )
            this.adapter = adapter
            var autoScrollToBottom = false
            var firstLoad = true
            val linearLayoutManager = layoutManager as LinearLayoutManager
            linearLayoutManager.stackFromEnd = true
            linearLayoutManager.reverseLayout = true
            setOnScrollChangeListener { _, _, scrollY, oldScrollX, oldScrollY ->
                val latestVisible = linearLayoutManager.findFirstCompletelyVisibleItemPosition()
                val oldestVisible = linearLayoutManager.findLastCompletelyVisibleItemPosition()
                adapter.currentList.getOrNull(latestVisible)?.messageId?.let {
                    chatViewModel.submitLastRead(it)
                }
                if (latestVisible != -1 && oldestVisible != -1 && !firstLoad) {
                    chatViewModel.informVisibleRange(
                        adapter.currentList[oldestVisible].messageId,
                        adapter.currentList[latestVisible].messageId,
                    )
                }
                if (latestVisible == 0 && adapter.currentList.firstOrNull()?.messageId == chatViewModel.latestMessage.value?.messageId) {
                    // TODO anchor to bottom
                    chatViewModel.lockToLatest()
                    autoScrollToBottom = true
                    Log.d(this@ChatFragment.LOG_TAG, "scrolled to bottom")
                } else if (oldScrollY != scrollY) {
                    autoScrollToBottom = false
                }
            }
            addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
//                Log.d(this@ChatFragment.LOG_TAG,
//                    "layout change from $oldBottom to $bottom, with auto scroll $autoScrollToBottom")
                if (autoScrollToBottom) {
                    Log.d(this@ChatFragment.LOG_TAG, "post scrolling to bottom")
                    post {
                        smoothScrollToPosition(0)
                        autoScrollToBottom = true
                    }
                } else if (firstLoad && adapter.currentList.isNotEmpty()) {
                    Log.d(this@ChatFragment.LOG_TAG, "move to last read")
                    firstLoad = false
                    val targetPos =
                        adapter.currentList.indexOfFirst { it.messageId == chatViewModel.lastRead.value }
                    if (targetPos != -1) {
                        post {
                            scrollToPosition(targetPos)
                        }
                    }
                }
            }
            binding.chatGoLatest.setOnClickListener {
                autoScrollToBottom = true
                chatViewModel.lockToLatest()
                adapter.currentList.firstOrNull()?.let {
                    if (linearLayoutManager.findFirstCompletelyVisibleItemPosition() > 50) {
                        // too long, direct jump
                        scrollToPosition(0)
                    } else {
                        smoothScrollToPosition(0)
                    }
                }
            }
        }
        binding.chatTextBox.apply {
            setOnEditorActionListener { view, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    val textMsg = view.text.toString()
                    view.text = ""
                    chatViewModel.sendTextMessage(textMsg)
                }
                true
            }
            setCloseImeOnLoseFocus()
            setRawInputType(InputType.TYPE_CLASS_TEXT)
        }
        binding.chatSwipeRefreshLayout.apply {
            setOnRefreshListener {
                chatViewModel.onSwipeRefresh()
                isRefreshing = false
            }
        }
        binding.topAppBar.menu.findItem(R.id.action_chat_show_failed_messages).apply {
            isVisible = false
            var value = 0
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                chatViewModel.failedMessageCount.collectLatest {
                    isVisible = it != 0
                    value = it
                }
            }
            setOnMenuItemClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.chat_unsent_messages)
                    .setMessage(getString(R.string.chat_unsent_messages_ask_retry).format(value))
                    .setPositiveButton(getString(R.string.chat_unsent_messages_retry_all)) { _, _ ->
                        chatViewModel.retryAllPendingMessages()
                    }
                    .setNegativeButton(getString(R.string.chat_unsent_messages_drop_all)) { _, _ ->
                        chatViewModel.dropAllPendingMessages()
                    }
                    .setNeutralButton(getString(R.string.chat_unsent_messages_do_nothing)) { _, _ -> }
                    .show()
                true
            }
        }
        return binding.root
    }
}
