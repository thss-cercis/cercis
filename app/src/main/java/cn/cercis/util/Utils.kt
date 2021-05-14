package cn.cercis.util

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import cn.cercis.CercisApplication

fun getString(@StringRes resId: Int) = CercisApplication.application.getString(resId)

fun getColor(@ColorRes resId: Int) = CercisApplication.application.getColor(resId)

fun ConstraintLayout.setDimensionRatio(viewId: Int, ratio: String) {
    val constraintSet = ConstraintSet()
    constraintSet.clone(this)
    constraintSet.setDimensionRatio(viewId, ratio)
    constraintSet.applyTo(this)
}
