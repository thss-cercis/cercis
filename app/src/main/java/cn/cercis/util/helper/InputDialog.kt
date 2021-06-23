package cn.cercis.util.helper

import android.content.Context
import androidx.appcompat.app.AlertDialog
import cn.cercis.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

fun showInputDialog(
    context: Context,
    title: String,
    initial: String,
    submit: (String) -> Unit,
): AlertDialog {
    return MaterialAlertDialogBuilder(context)
        .setTitle(title)
        .setView(R.layout.dialog_text_field)
        .setPositiveButton(R.string.dialog_ok) { dialog, _ ->
            dialog as AlertDialog
            val editText =
                dialog.findViewById<TextInputEditText>(R.id.dialog_text_field_edit_text)!!
            editText.text.toString().takeIf { it.isNotEmpty() }?.let {
                submit(it)
            }
        }
        .setNegativeButton(R.string.dialog_cancel) { _, _ -> }
        .show().apply {
            findViewById<TextInputEditText>(R.id.dialog_text_field_edit_text)!!
                .setText(initial)
        }
}