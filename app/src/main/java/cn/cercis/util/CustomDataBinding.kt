package cn.cercis.util

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import cn.cercis.Constants.STATIC_BASE
import cn.cercis.R
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView

@BindingAdapter("avatarUrl")
fun loadAvatar(view: ShapeableImageView, url: String?) {
    view.scaleType = ImageView.ScaleType.CENTER_CROP
    Glide.with(view.context)
        // use null url instead of empty url to allow for quicker fallback without error log
        .load(url?.takeIf(String::isNotEmpty))
        .fallback(R.drawable.ic_default_avatar)
        .placeholder(R.drawable.ic_default_avatar)
        .into(view)
}

@BindingAdapter("imageUrl")
fun loadImage(view: ImageView, url: String?) {
    Glide.with(view.context)
        .load(url?.takeIf(String::isNotEmpty))
        .fallback(R.color.image_placeholder)
        .placeholder(R.color.image_placeholder)
        .into(view)
}
