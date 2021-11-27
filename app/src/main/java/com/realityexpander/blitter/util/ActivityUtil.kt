package com.realityexpander.blitter.util

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.google.android.material.textfield.TextInputLayout

class ActivityUtil {

    companion object {

        // Monitor for text change and remove the error message for the TIL
        fun setTextChangeListener(et: EditText, til: TextInputLayout) {
            et.addTextChangedListener( object: TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    // do nothing
                }
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    til.isErrorEnabled = false
                }
                override fun afterTextChanged(s: Editable?) {
                    // do nothing
                }
            })
        }
    }
}