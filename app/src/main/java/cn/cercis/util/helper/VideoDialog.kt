package cn.cercis.util.helper

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.net.toUri
import cn.cercis.R
import cn.cercis.common.LOG_TAG
import cn.cercis.databinding.DialogPlayVideoBinding
import com.bumptech.glide.Glide
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

fun showImageDialog(context: Context, uri: String): AlertDialog {
    return MaterialAlertDialogBuilder(context)
        .setTitle(R.string.image_dialog_title)
        .setView(R.layout.dialog_show_image)
        .show().apply {
            window?.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            findViewById<ImageView>(R.id.dialog_show_image_view)!!.let { view ->
                Glide.with(view)
                    .load(uri.toUri())
                    .into(view)
                view.setOnClickListener {
                    this.cancel()
                }
            }
        }
}
