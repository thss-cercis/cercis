package cn.cercis.component

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import cn.cercis.R

class BubbleView(context: Context, attributeSet: AttributeSet?, style: Int) :
    FrameLayout(context, attributeSet, style) {
    val bubbleType: Int
    private var bubbleContent: String? = null

    init {
        val a = context.theme.obtainStyledAttributes(attributeSet, R.styleable.Bubble, 0, 0)
        bubbleType = a.getInt(R.styleable.Bubble_bubbleType, -1)
        setBubbleContent(a.getString(R.styleable.Bubble_bubbleContent))
    }

    fun inflate(type: Int) {
        when (bubbleType) {
            TEXT -> inflate(context, R.layout.bubble_view_text, this)
            IMAGE -> inflate(context, R.layout.bubble_view_image, this)
            AUDIO -> inflate(context, R.layout.bubble_view_audio, this)
            VIDEO -> inflate(context, R.layout.bubble_view_video, this)
            LOCATION -> inflate(context, R.layout.bubble_view_location, this)
        }

    }

    fun setBubbleType() {

    }

    fun setBubbleContent(@StringRes stringRes: Int) {
        setBubbleContent(context.getString(stringRes))
    }

    fun setBubbleContent(string: String?) {

    }

    companion object {
        const val TEXT = 0
        const val IMAGE = 1
        const val AUDIO = 2
        const val VIDEO = 3
        const val LOCATION = 4
    }
}