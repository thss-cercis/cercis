package cn.cercis.ui.chat

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.Context.CLIPBOARD_SERVICE
import android.content.pm.PackageManager
import android.graphics.Rect
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent.*
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import cn.cercis.R
import cn.cercis.SelectLocationActivity
import cn.cercis.SelectedLocation
import cn.cercis.ShowLocationActivity
import cn.cercis.common.LOG_TAG
import cn.cercis.databinding.*
import cn.cercis.entity.ChatType
import cn.cercis.entity.MessageType
import cn.cercis.entity.asMessageType
import cn.cercis.util.getSharedTempFile
import cn.cercis.util.getTempFile
import cn.cercis.util.helper.*
import cn.cercis.util.setClipboard
import cn.cercis.viewmodel.ChatViewModel
import cn.cercis.viewmodel.ChatViewModel.MessageDirection.INCOMING
import cn.cercis.viewmodel.ChatViewModel.MessageDirection.OUTGOING
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*


@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class ChatFragment : Fragment() {
    private val viewModel: ChatViewModel by viewModels()
    private val mediaPlayer: MediaPlayer = MediaPlayer()

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

        const val PAGE_AUDIO = 0
        const val PAGE_IMAGE = 1
        const val PAGE_EMOJI = 2
        const val PAGE_ADDITION = 3

        const val REQ_SHARE_LOCATION = 100
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentChatBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        // initialize media player
        mediaPlayer.setOnPreparedListener {
            it.start()
            Log.d(LOG_TAG, "mediaplayer prepared, starting...")
        }

        // initialize recycler view
        binding.chatRecyclerView.apply {
            val adapter = DiffRecyclerViewAdapter.getInstance(
                dataSource = viewModel.chatMessageList,
                viewLifecycleOwnerSupplier = { viewLifecycleOwner },
                itemIndex = { messageComposeId },
                contentsSameCallback = Objects::equals,
                inflater = { layoutInflater, parent, viewType ->
                    val mvt = MessageViewType.fromViewType(viewType)
                    when (val msgType = mvt.messageType.asMessageType()) {
                        // delete and withdraw has special layouts
                        MessageType.DELETED -> {
                            ChatItemDeletedBinding.inflate(layoutInflater, parent, false)
                        }
                        MessageType.WITHDRAW -> {
                            ChatItemWithdrawedBinding.inflate(layoutInflater, parent, false)
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
                                    textView.setCompoundDrawablesWithIntrinsicBounds(
                                        R.drawable.ic_video_24, 0, 0, 0
                                    )
                                    imageView.visibility = View.GONE
                                }
                                MessageType.LOCATION -> {
                                    textView.setCompoundDrawablesWithIntrinsicBounds(
                                        R.drawable.ic_location_40, 0, 0, 0
                                    )
                                    imageView.visibility = View.GONE
                                }
                                else -> throw IllegalStateException("unexpected message type")
                            }
                            when (msgType) {
                                MessageType.UNKNOWN, MessageType.WITHDRAW, MessageType.DELETED -> {
                                }
                                else -> {
                                    if (mvt.direction == OUTGOING.type || msgType == MessageType.TEXT) {
                                        when (mvt.direction) {
                                            OUTGOING.type -> (ret as ChatItemOutgoingBinding).chatItemOutgoingBubble
                                            INCOMING.type -> (ret as ChatItemIncomingBinding).chatItemIncomingBubble
                                            else -> throw IllegalStateException("invalid message direction")
                                        }.apply {
                                            setOnCreateContextMenuListener { menu, _, _ ->
                                                if (mvt.direction == OUTGOING.type) {
                                                    menu.add(R.string.chat_message_action_withdraw)
                                                        .setOnMenuItemClickListener {
                                                            viewModel.withdrawMessage((ret as ChatItemOutgoingBinding).messageId)
                                                            true
                                                        }
                                                }
                                                if (msgType == MessageType.TEXT) {
                                                    menu.add(R.string.chat_message_action_copy)
                                                        .setOnMenuItemClickListener {
                                                            setClipboard(requireContext(),
                                                                when (mvt.direction) {
                                                                    OUTGOING.type -> (ret as ChatItemOutgoingBinding).chatItemMessageText
                                                                    INCOMING.type -> (ret as ChatItemIncomingBinding).chatItemMessageText
                                                                    else -> throw IllegalStateException(
                                                                        "invalid message direction")
                                                                }.text.toString())
                                                            true
                                                        }
                                                }
                                            }
                                        }
                                    }
                                }
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
                            (holder.binding as ChatItemWithdrawedBinding).apply {
                                if (data is ChatViewModel.SentDisplayMessage) {
                                    holder.binding.root.visibility = View.VISIBLE
                                    user = viewModel.loadUser(data.senderId)
                                } else {
                                    holder.binding.root.visibility = View.GONE
                                }
                            }
                        }
                        else -> {
                            val itemBinding = holder.binding
                            val (imageView, textView, bubble) = when (mvt.direction) {
                                OUTGOING.type -> (itemBinding as ChatItemOutgoingBinding)
                                    .let {
                                        it.user = viewModel.loadUser(data.senderId)
                                        // bind messageId for withdraw
                                        it.messageId = data.messageComposeId.messageId
                                        Triple(it.chatItemMessageImage,
                                            it.chatItemMessageText,
                                            it.chatItemOutgoingBubble)
                                    }
                                INCOMING.type -> (itemBinding as ChatItemIncomingBinding)
                                    .let {
                                        it.user = viewModel.loadUser(data.senderId)
                                        Triple(it.chatItemMessageImage,
                                            it.chatItemMessageText,
                                            it.chatItemIncomingBubble)
                                    }
                                else -> throw IllegalStateException("erroneous view type")
                            }

                            // important for bubble to receive click
                            bubble.isClickable = true

                            if (itemBinding is ChatItemOutgoingBinding) {
                                if (data is ChatViewModel.PendingDisplayMessage) {
                                    itemBinding.apply {
                                        sending = data.isSending
                                        failed = data.isFailed

                                        // add retry button on click
                                        if (failed) {
                                            chatItemOutgoingRetry.setOnClickListener {
                                                viewModel.retryMessage(data.msgProgress)
                                            }
                                        }
                                    }
                                } else {
                                    itemBinding.apply {
                                        sending = false
                                        failed = false
                                    }
                                }
                            }

                            // bind message for different types
                            when (msgType) {
                                MessageType.TEXT -> {
                                    textView.text = data.message
                                }
                                MessageType.IMAGE -> {
                                    if (data is ChatViewModel.SentDisplayMessage) {
                                        textView.visibility = View.GONE
                                        Glide.with(imageView)
                                            .load(data.message)
                                            .into(imageView)
                                        bubble.setOnClickListener {
                                            showImageDialog(requireContext(), data.message)
                                        }
                                    } else {
                                        bubble.setOnClickListener(null)
                                    }
                                }
                                MessageType.AUDIO -> {
                                    if (data is ChatViewModel.SentDisplayMessage) {
                                        bubble.setOnClickListener {
                                            mediaPlayer.reset()
                                            mediaPlayer.setDataSource(requireContext(),
                                                data.message.toUri())
                                            mediaPlayer.prepareAsync()
                                            Log.d(LOG_TAG, "preparing media: ${data.message}")
                                        }
                                    } else {
                                        bubble.setOnClickListener(null)
                                    }
                                    textView.text = getString(R.string.message_type_audio)
                                }
                                MessageType.VIDEO -> {
                                    if (data is ChatViewModel.SentDisplayMessage) {
                                        bubble.setOnClickListener {
                                            showVideoDialog(requireContext(), data.message)
                                        }
                                    } else {
                                        bubble.setOnClickListener(null)
                                    }
                                    textView.text = getString(R.string.message_type_video)
                                }
                                MessageType.LOCATION -> {
                                    if (data is ChatViewModel.SentDisplayMessage) {
                                        val locationContent =
                                            SelectedLocation.fromMessageContent(data.message)
                                        bubble.setOnClickListener {
                                            startActivity(Intent(requireContext(),
                                                ShowLocationActivity::class.java).apply {
                                                putExtra("location", locationContent)
                                            })
                                        }
                                        textView.text =
                                            locationContent.address.takeIf { it.isNotEmpty() }
                                                ?: "[${getString(R.string.message_type_location)}]"
                                    } else {
                                        textView.text =
                                            "[${getString(R.string.message_type_location)}]"
                                        bubble.setOnClickListener(null)
                                    }
                                }
                                MessageType.UNKNOWN -> {
                                    textView.text = getString(R.string.message_type_unknown)
                                }
                                else -> throw IllegalStateException("unexpected message type")
                            }
                        }
                    }
                },
                itemViewType = { MessageViewType(viewModel.side(senderId).type, type).viewType }
            )
            this.adapter = adapter
            var autoScrollToBottom = false
            var firstLoad = true
            val linearLayoutManager = layoutManager as LinearLayoutManager
            linearLayoutManager.reverseLayout = true
            itemAnimator = null
//            (itemAnimator as DefaultItemAnimator).supportsChangeAnimations = false
            // when scrolled to bottom and autoScrollToBottom is true, chat will scroll to bottom on new messages
            setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                val latestVisible = linearLayoutManager.findFirstCompletelyVisibleItemPosition()
                val oldestVisible = linearLayoutManager.findLastCompletelyVisibleItemPosition()
                adapter.currentList.getOrNull(latestVisible)?.messageComposeId?.let {
                    viewModel.submitLastRead(it.messageId)
                }
                if (latestVisible != -1 && oldestVisible != -1) {
                    viewModel.informVisibleRange(
                        adapter.currentList[oldestVisible].messageComposeId.messageId,
                        adapter.currentList[latestVisible].messageComposeId.messageId,
                    )
                }
                if (latestVisible == 0 && adapter.currentList.firstOrNull()?.messageComposeId?.messageId == viewModel.latestMessage.value?.messageId) {
                    viewModel.lockToLatest()
                    autoScrollToBottom = true
                    Log.d(this@ChatFragment.LOG_TAG, "scrolled to bottom")
                } else if (oldScrollY != scrollY) {
                    autoScrollToBottom = false
                }
            }

            // layout would change if new messages are added to the list
            addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                if (autoScrollToBottom) {
                    Log.d(this@ChatFragment.LOG_TAG, "post scrolling to bottom")
                    post {
                        smoothScrollToPosition(0)
                        autoScrollToBottom = true

                        val latestVisible =
                            linearLayoutManager.findFirstCompletelyVisibleItemPosition()
                        val oldestVisible =
                            linearLayoutManager.findLastCompletelyVisibleItemPosition()
                        if (latestVisible != -1 && oldestVisible != -1) {
                            viewModel.informVisibleRange(
                                adapter.currentList[oldestVisible].messageComposeId.messageId,
                                adapter.currentList[latestVisible].messageComposeId.messageId,
                            )
                        }
                    }
                }
            }

            // add click listener for go to bottom button
            binding.chatGoLatest.setOnClickListener {
                autoScrollToBottom = true
                viewModel.lockToLatest()
                adapter.currentList.firstOrNull()?.let {
                    if (linearLayoutManager.findFirstCompletelyVisibleItemPosition() > 50) {
                        // too long, direct jump
                        scrollToPosition(0)
                    } else {
                        smoothScrollToPosition(0)
                    }
                }
            }

            // when clicked, collapse the panel
            setOnClickListener {
                viewModel.foldPanel()
            }
        }

        // listen to IME option send
        binding.chatTextBox.apply {
            setOnEditorActionListener { view, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    val textMsg = view.text.toString()
                    view.text = ""
                    viewModel.sendTextMessage(textMsg)
                }
                true
            }
            setOnFocusChangeListener { v, hasFocus ->
                if (!hasFocus) {
                    closeIme()
                } else {
                    viewModel.foldPanel()
                }
            }
            setRawInputType(InputType.TYPE_CLASS_TEXT)
        }

        // when swiped down, triggers message list refresh
        binding.chatSwipeRefreshLayout.apply {
            setOnRefreshListener {
                viewModel.onSwipeRefresh()
                isRefreshing = false
            }
        }

        // add retry menu action on toolbar
        binding.topAppBar.menu.findItem(R.id.action_chat_show_failed_messages).apply {
            isVisible = false
            var value = 0
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                viewModel.failedMessageCount.collectLatest {
                    isVisible = it != 0
                    value = it
                }
            }
            setOnMenuItemClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.chat_unsent_messages)
                    .setMessage(getString(R.string.chat_unsent_messages_ask_retry).format(
                        value))
                    .setPositiveButton(getString(R.string.chat_unsent_messages_retry_all)) { _, _ ->
                        viewModel.retryAllPendingMessages()
                    }
                    .setNegativeButton(getString(R.string.chat_unsent_messages_drop_all)) { _, _ ->
                        viewModel.dropAllPendingMessages()
                    }
                    .setNeutralButton(getString(R.string.chat_unsent_messages_do_nothing)) { _, _ -> }
                    .show()
                true
            }
        }

        // add click listener for chat detail
        binding.topAppBar.menu.findItem(R.id.action_chat_info).setOnMenuItemClickListener {
            if (viewModel.chatInitData.type == ChatType.CHAT_GROUP) {
                findNavController().navigate(
                    GroupInfoFragmentDirections.actionGlobalGroupInfoFragment(
                        viewModel.chatInitData
                    )
                )
            } else {
                lifecycleScope.launch(Dispatchers.IO) {
                    val otherUser = viewModel.getOtherUser()
                    otherUser.data?.let {
                        launch(Dispatchers.Main) {
                            requireMainActivity().openUserInfo(it)
                        }
                    }
                }
            }
            true
        }

        // listen to panel expansion
        viewModel.expanded.observe(viewLifecycleOwner) {
            if (it == true) {
                binding.chatMotionLayout.transitionToEnd()
                closeIme()
            } else if (it == false) {
                binding.chatMotionLayout.transitionToStart()
            }
        }

        // actions
        val switchToPanel = { index: Int ->
            if (viewModel.expanded.value == true && viewModel.selectedPage.value == index) {
                viewModel.foldPanel()
            } else {
                viewModel.selectedPage.value = index
                viewModel.expandPanel()
            }
        }

        // change page
        viewModel.selectedPage.observe(viewLifecycleOwner) {
            binding.chatActionFlipper.displayedChild = it!!
        }

        // close panel on scrim touched
        binding.chatActionFlipperScrim.setOnTouchListener { _, _ -> viewModel.foldPanel(); true }

        val recordingPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()
            ) { granted ->
                if (granted[Manifest.permission.RECORD_AUDIO] != true) {
                    // if permission is not granted, pop up a dialog.
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.permission_not_granted_dialog_title)
                        .setMessage(R.string.permission_not_granted_dialog_record_audio_required)
                        .setPositiveButton(R.string.permission_not_granted_dialog_jump_to_app_settings) { _, _ ->
                            openApplicationSettingsPage()
                        }
                        .setNegativeButton(R.string.permission_not_granted_dialog_cancel) { _, _ -> }
                        .show()
                }
            }

        // bind audio recoding page
        binding.chatActionSendAudioPage.apply {
            viewModel.isRecording.observe(viewLifecycleOwner) {
                if (it == true) {
                    this.chatActionSendAudioPage.transitionToState(R.id.state_send_audio_recording)
                }
            }

            var rect: Rect? = null
            var recordStartTime: Long = 0
            var inRange: Boolean = true
            this.chatActionSendAudioRecordButton.setOnTouchListener { v, event ->
                val recording = viewModel.isRecording.value == true
                when (event.action) {
                    ACTION_DOWN -> {
                        v.isPressed = true
                        rect = Rect(v.left, v.top, v.right, v.bottom)
                        if (ActivityCompat.checkSelfPermission(requireContext(),
                                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                        ) {
                            // if not granted, request for permission
                            recordingPermissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                        } else {
                            viewModel.startRecording()
                            recordStartTime = System.currentTimeMillis()
                        }
                    }
                    ACTION_UP -> {
                        v.isPressed = false
                        if (inRange) {
                            if (System.currentTimeMillis() - recordStartTime < 500) {
                                Toast.makeText(requireContext(),
                                    getString(R.string.chat_send_audio_page_toast_record_too_short),
                                    Toast.LENGTH_SHORT).show()
                            }
                            viewModel.finishRecording()
                        } else {
                            viewModel.cancelRecording()
                        }
                        this.chatActionSendAudioPage.transitionToStart()
                    }
                    ACTION_CANCEL -> {
                        v.isPressed = false
                        viewModel.cancelRecording()
                        this.chatActionSendAudioPage.transitionToStart()
                    }
                    ACTION_MOVE -> {
                        // https://stackoverflow.com/a/8069887
                        rect?.let {
                            if (recording) {
                                if (!it.contains(v.left + event.x.toInt(), v.top + event.y.toInt())
                                ) {
                                    // User moved outside bounds
                                    inRange = false
                                    this.chatActionSendAudioPage.transitionToState(R.id.state_send_audio_about_to_delete)
                                } else {
                                    inRange = true
                                    this.chatActionSendAudioPage.transitionToState(R.id.state_send_audio_recording)
                                }
                            }
                        }
                    }
                }
                true
            }
        }

        // bind image sharing page
        binding.chatActionSendImagePage.apply {
            var imageFile: File? = null
            val takePictureLauncher =
                registerForActivityResult(ActivityResultContracts.TakePicture()
                ) { bool ->
                    if (bool) {
                        imageFile?.let { viewModel.sendImageMessage(it) }
                    }
                }

            this.chatActionSendImageTakePhotoButton.setOnClickListener {
                takePictureLauncher.launch(getSharedTempFile(".jpg").let {
                    imageFile = it.second
                    it.first
                })
            }

            var videoFile: File? = null
            val takeVideoLauncher =
                registerForActivityResult(ActivityResultContracts.TakeVideo()
                ) { bitmap ->
                    if (videoFile != null) {
                        videoFile?.let { viewModel.sendVideoMessage(it) }
                    }
                }
            this.chatActionSendImageTakeVideoButton.setOnClickListener {
                takeVideoLauncher.launch(getSharedTempFile(".mp4").let {
                    videoFile = it.second
                    it.first
                })
            }

            val fromGalleryLauncher =
                registerForActivityResult(object : ActivityResultContracts.GetContent() {
                    override fun createIntent(context: Context, input: String): Intent {
                        return super.createIntent(context, input).apply {
                            this.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
                        }
                    }
                }) { uri: Uri? ->
                    uri?.let { it ->
                        val cr: ContentResolver = requireContext().contentResolver
                        cr.getType(it)?.let { mime ->
                            try {
                                getTempFile(".tmp").apply {
                                    requireContext().contentResolver.openInputStream(uri)
                                        ?.let { input ->
                                            FileOutputStream(this).use {
                                                input.copyTo(it)
                                            }
                                            if (mime.startsWith("image/")) {
                                                // image
                                                viewModel.sendImageMessage(this)
                                            } else if (mime.startsWith("video/")) {
                                                // video
                                                viewModel.sendVideoMessage(this)
                                            }
                                        }
                                }
                            } catch (ex: IOException) {
                                ex.printStackTrace()
                            }
                        }
                    }
                }
            this.chatActionSendImageFromGalleryButton.setOnClickListener {
                fromGalleryLauncher.launch("image/*")
            }
        }

        binding.chatActionExtraPage.apply {
            chatActionExtraShareLocation.setOnClickListener {
                @Suppress("DEPRECATION")
                startActivityForResult(Intent(requireContext(), SelectLocationActivity::class.java),
                    REQ_SHARE_LOCATION)
            }
        }

        // bind send image/video page
        binding.chatActionSendAudio.setOnClickListener { switchToPanel(PAGE_AUDIO) }
        binding.chatActionSendImage.setOnClickListener { switchToPanel(PAGE_IMAGE) }
        binding.chatActionSendEmoji.setOnClickListener { switchToPanel(PAGE_EMOJI) }
        binding.chatActionSendAddition.setOnClickListener { switchToPanel(PAGE_ADDITION) }
        return binding.root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_SHARE_LOCATION && resultCode == SelectLocationActivity.RESULT_CODE_SUCCESS) {
            data?.let {
                val location = data.getParcelableExtra<SelectedLocation>("location")!!
                Log.d(LOG_TAG, "$location")
                viewModel.sendLocationMessage(location)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }
}
