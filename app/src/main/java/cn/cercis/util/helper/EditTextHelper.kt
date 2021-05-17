package cn.cercis.util.helper

import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.widget.EditText


fun EditText.setCloseImeOnLoseFocus() {
    this.setOnFocusChangeListener { v, hasFocus ->
        if (!hasFocus) {
            val imm: InputMethodManager? = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            imm?.hideSoftInputFromWindow(v.windowToken, 0)
        }
    }
}
