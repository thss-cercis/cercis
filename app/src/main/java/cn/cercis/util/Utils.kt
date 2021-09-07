package cn.cercis.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.MediaStore
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import cn.cercis.CercisApplication
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

fun getString(@StringRes resId: Int) = CercisApplication.application.getString(resId)

fun getColor(@ColorRes resId: Int) = CercisApplication.application.getColor(resId)

fun ConstraintLayout.setDimensionRatio(viewId: Int, ratio: String) {
    val constraintSet = ConstraintSet()
    constraintSet.clone(this)
    constraintSet.setDimensionRatio(viewId, ratio)
    constraintSet.applyTo(this)
}

fun getTempFile(filename: String, extension: String): File {
    return File.createTempFile(filename, extension, CercisApplication.application.cacheDir)
}

private val atomicInt = AtomicInteger(0)
fun getTempFilename(): String {
    return "${System.currentTimeMillis()}_${atomicInt.getAndIncrement()}"
}

fun getTempFilename(extension: String): String {
    return "${getTempFilename()}.${extension}"
}

fun getTempFile(extension: String): File {
    return File.createTempFile(getTempFilename(),
        extension,
        CercisApplication.application.cacheDir)
}

/**
 * Gets a temp file that can be accessed by other programs, e.g. TakePicture.
 */
fun getSharedTempFile(extension: String): Pair<Uri, File> {
    return getTempFile(extension).let {
        FileProvider.getUriForFile(CercisApplication.application,
            CercisApplication.application.packageName + ".provider",
            it
        ) to it
    }
}

fun setClipboard(context: Context, text: String) {
    ContextCompat.getSystemService(context, ClipboardManager::class.java)
        ?.setPrimaryClip(ClipData.newPlainText("label", text))
}

fun saveImage(context: Context, drawable: Drawable, title: String): String? {
    return MediaStore.Images.Media.insertImage(context.contentResolver,
        drawable.toBitmap(),
        title,
        "")
}
