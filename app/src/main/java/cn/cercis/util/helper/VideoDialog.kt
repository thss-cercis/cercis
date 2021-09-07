package cn.cercis.util.helper

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.WindowManager.LayoutParams
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.net.toUri
import cn.cercis.R
import cn.cercis.databinding.DialogPlayVideoBinding
import cn.cercis.util.getString
import cn.cercis.util.getTempFilename
import cn.cercis.util.resource.Resource
import cn.cercis.util.saveImage
import cn.cercis.util.snackbarMakeSuccess
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

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
                var loadingImage: Resource<Drawable> = Resource.Loading(null)
                Glide.with(view)
                    .load(uri.toUri())
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>?,
                            isFirstResource: Boolean,
                        ): Boolean {
                            loadingImage = Resource.Error(-1, "", null)
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable?,
                            model: Any?,
                            target: Target<Drawable>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean,
                        ): Boolean {
                            resource?.let {
                                loadingImage = Resource.Success(resource)
                            }
                            return false
                        }
                    })
                    .into(view)
                view.setOnClickListener {
                    this.cancel()
                }
                view.setOnCreateContextMenuListener { menu, _, _ ->
                    menu.add(R.string.image_save_to_local).setOnMenuItemClickListener {
                        when (val image = loadingImage) {
                            is Resource.Success -> {
                                saveImage(context, image.data, getTempFilename())?.let {
                                    snackbarMakeSuccess(view,
                                        getString(R.string.image_save_to_local_success),
                                        Snackbar.LENGTH_SHORT)
                                }
                            }
                        }
                        true
                    }
                }
            }
        }
}
