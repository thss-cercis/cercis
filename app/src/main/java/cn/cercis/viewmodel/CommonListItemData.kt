package cn.cercis.viewmodel

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CommonListItemData(
    val avatar: String,
    val displayName: CharSequence,
    val description: CharSequence,
) : Parcelable
