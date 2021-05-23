package cn.cercis.util

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import cn.cercis.R
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView

@BindingAdapter("avatarUrl")
fun loadAvatar(view: ShapeableImageView, url: String?) {
    Glide.with(view.context)
        // use null url instead of empty url to allow for quicker fallback without error log
        .load(if (url.isNullOrEmpty()) null else url)
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
