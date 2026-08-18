package net.blockhost.trestle.game

import android.content.Context
import android.annotation.SuppressLint
import android.graphics.Color
import android.text.InputType
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager

@SuppressLint("ViewConstructor")
internal class MinecraftTextInputView(
    context: Context,
    private val onText: (CharSequence) -> Unit,
    private val onBackspace: () -> Unit,
    private val onEnter: () -> Unit,
) : View(context) {
    init {
        isFocusable = true
        isFocusableInTouchMode = true
        setBackgroundColor(Color.TRANSPARENT)
        alpha = 0.01f
        contentDescription = "Minecraft text input"
    }

    override fun onCheckIsTextEditor(): Boolean = true

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_FLAG_MULTI_LINE or
            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_FULLSCREEN
        return MinecraftInputConnection()
    }

    fun showKeyboard() {
        requestFocus()
        post {
            context.getSystemService(InputMethodManager::class.java)
                .showSoftInput(this, 0)
        }
    }

    fun hideKeyboard() {
        context.getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(windowToken, 0)
        clearFocus()
    }

    private inner class MinecraftInputConnection : BaseInputConnection(this@MinecraftTextInputView, false) {
        private var composingText = ""

        override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
            eraseComposition()
            composingText = text?.toString().orEmpty()
            if (composingText.isNotEmpty()) onText(composingText)
            return true
        }

        override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
            eraseComposition()
            text?.takeIf(CharSequence::isNotEmpty)?.let(onText)
            return true
        }

        override fun finishComposingText(): Boolean {
            composingText = ""
            return true
        }

        override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
            if (composingText.isNotEmpty()) {
                eraseComposition()
            } else {
                repeat(beforeLength.coerceIn(0, MAX_DELETE_COUNT)) { onBackspace() }
            }
            return true
        }

        override fun performEditorAction(actionCode: Int): Boolean {
            onEnter()
            return true
        }

        private fun eraseComposition() {
            repeat(composingText.length.coerceAtMost(MAX_DELETE_COUNT)) { onBackspace() }
            composingText = ""
        }
    }

    private companion object {
        const val MAX_DELETE_COUNT = 256
    }
}
