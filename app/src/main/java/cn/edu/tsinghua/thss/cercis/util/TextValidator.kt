package cn.edu.tsinghua.thss.cercis.util

import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher

@FunctionalInterface
interface TextValidator : TextWatcher {

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        /* do nothing */
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        /* do nothing */
    }

    override fun afterTextChanged(s: Editable?)  {
        validate(s.toString())
    }

    fun validate(s: String)

}