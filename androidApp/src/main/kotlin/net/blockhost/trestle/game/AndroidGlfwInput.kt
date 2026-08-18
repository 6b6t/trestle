package net.blockhost.trestle.game

import android.view.KeyEvent

internal object AndroidGlfwInput {
    const val RELEASE = 0
    const val PRESS = 1
    const val REPEAT = 2

    fun key(keyCode: Int): Int? = when (keyCode) {
        in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z -> 'A'.code + keyCode - KeyEvent.KEYCODE_A
        in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> '0'.code + keyCode - KeyEvent.KEYCODE_0
        KeyEvent.KEYCODE_SPACE -> 32
        KeyEvent.KEYCODE_APOSTROPHE -> 39
        KeyEvent.KEYCODE_COMMA -> 44
        KeyEvent.KEYCODE_MINUS -> 45
        KeyEvent.KEYCODE_PERIOD -> 46
        KeyEvent.KEYCODE_SLASH -> 47
        KeyEvent.KEYCODE_SEMICOLON -> 59
        KeyEvent.KEYCODE_EQUALS -> 61
        KeyEvent.KEYCODE_LEFT_BRACKET -> 91
        KeyEvent.KEYCODE_BACKSLASH -> 92
        KeyEvent.KEYCODE_RIGHT_BRACKET -> 93
        KeyEvent.KEYCODE_GRAVE -> 96
        KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_BACK -> 256
        KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> 257
        KeyEvent.KEYCODE_TAB -> 258
        KeyEvent.KEYCODE_DEL -> 259
        KeyEvent.KEYCODE_INSERT -> 260
        KeyEvent.KEYCODE_FORWARD_DEL -> 261
        KeyEvent.KEYCODE_DPAD_RIGHT -> 262
        KeyEvent.KEYCODE_DPAD_LEFT -> 263
        KeyEvent.KEYCODE_DPAD_DOWN -> 264
        KeyEvent.KEYCODE_DPAD_UP -> 265
        KeyEvent.KEYCODE_PAGE_UP -> 266
        KeyEvent.KEYCODE_PAGE_DOWN -> 267
        KeyEvent.KEYCODE_MOVE_HOME -> 268
        KeyEvent.KEYCODE_MOVE_END -> 269
        KeyEvent.KEYCODE_CAPS_LOCK -> 280
        KeyEvent.KEYCODE_SCROLL_LOCK -> 281
        KeyEvent.KEYCODE_NUM_LOCK -> 282
        KeyEvent.KEYCODE_SYSRQ -> 283
        KeyEvent.KEYCODE_BREAK -> 284
        in KeyEvent.KEYCODE_F1..KeyEvent.KEYCODE_F12 -> 290 + keyCode - KeyEvent.KEYCODE_F1
        in KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_9 -> 320 + keyCode - KeyEvent.KEYCODE_NUMPAD_0
        KeyEvent.KEYCODE_NUMPAD_DOT -> 330
        KeyEvent.KEYCODE_NUMPAD_DIVIDE -> 331
        KeyEvent.KEYCODE_NUMPAD_MULTIPLY -> 332
        KeyEvent.KEYCODE_NUMPAD_SUBTRACT -> 333
        KeyEvent.KEYCODE_NUMPAD_ADD -> 334
        KeyEvent.KEYCODE_NUMPAD_EQUALS -> 336
        KeyEvent.KEYCODE_SHIFT_LEFT -> 340
        KeyEvent.KEYCODE_CTRL_LEFT -> 341
        KeyEvent.KEYCODE_ALT_LEFT -> 342
        KeyEvent.KEYCODE_META_LEFT -> 343
        KeyEvent.KEYCODE_SHIFT_RIGHT -> 344
        KeyEvent.KEYCODE_CTRL_RIGHT -> 345
        KeyEvent.KEYCODE_ALT_RIGHT -> 346
        KeyEvent.KEYCODE_META_RIGHT -> 347
        KeyEvent.KEYCODE_MENU -> 348
        else -> null
    }

    fun modifiers(metaState: Int): Int =
        (if (metaState and KeyEvent.META_SHIFT_ON != 0) 0x0001 else 0) or
            (if (metaState and KeyEvent.META_CTRL_ON != 0) 0x0002 else 0) or
            (if (metaState and KeyEvent.META_ALT_ON != 0) 0x0004 else 0) or
            (if (metaState and KeyEvent.META_META_ON != 0) 0x0008 else 0) or
            (if (metaState and KeyEvent.META_CAPS_LOCK_ON != 0) 0x0010 else 0) or
            (if (metaState and KeyEvent.META_NUM_LOCK_ON != 0) 0x0020 else 0)
}
