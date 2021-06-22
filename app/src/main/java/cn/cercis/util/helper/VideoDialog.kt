package cn.cercis.util.helper

import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import cn.cercis.R
import cn.cercis.databinding.DialogPlayVideoBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

fun showVideoDialog(context: Context, uri: String): AlertDialog {
    return MaterialAlertDialogBuilder(context)
        .setTitle(R.string.video_dialog_title)
        .setView(R.layout.dialog_play_video)
        .show().apply {
            DialogPlayVideoBinding.bind(findViewById<ConstraintLayout>(R.id.dialog_play_video)!!)
                .apply {
                    buttonVisible = false
                    progressVisible = true
                    dialogPlayVideoButton.setOnClickListener {
                        dialogPlayVideoView.start()
                        buttonVisible = false
                        executePendingBindings()
                    }
                    dialogPlayVideoView.apply {
                        setVideoURI(Uri.parse(uri))
                        setOnPreparedListener {
                            progressVisible = false
                            buttonVisible = true
                            executePendingBindings()
                        }
                        setOnCompletionListener {
                            buttonVisible = true
                            executePendingBindings()
                        }
                    }
                }
        }
}
