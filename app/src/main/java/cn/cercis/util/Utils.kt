package cn.cercis.util

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
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
fun getTempFile(extension: String): File {
    return File.createTempFile("${System.currentTimeMillis()}_${atomicInt.getAndIncrement()}",
        extension,
        CercisApplication.application.cacheDir)
}
