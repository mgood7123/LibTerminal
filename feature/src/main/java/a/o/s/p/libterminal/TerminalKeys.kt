/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package a.o.s.p.libterminal

import android.util.Log
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View

class TerminalKeys {

    private var mTerm: Terminal? = null

    fun getCharacter(event: KeyEvent): Int {
        val c = event.unicodeChar
        // TODO: Actually support dead keys
        if (c and KeyCharacterMap.COMBINING_ACCENT != 0) {
            Log.w(TAG, "Received dead key, ignoring")
            return 0
        }
        return c
    }

    fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
        if (DEBUG) {
            Log.d(TAG, "dispatched key event: " +
                    "keyCode='" + keyCode + "'")
        }
        if (mTerm == null || event.action == KeyEvent.ACTION_UP) return false

        val modifiers = getModifiers(event)

        var c = getKey(event)
        if (c != 0) {
            if (DEBUG) {
                Log.d(TAG, "dispatched key event: " +
                        "mod=" + modifiers + ", " +
                        "keys=" + getKeyName(c))
            }
            return mTerm!!.dispatchKey(modifiers, c)
        }

        c = getCharacter(event)
        if (c != 0) {
            if (DEBUG) {
                Log.d(TAG, "dispatched key event: " +
                        "mod=" + modifiers + ", " +
                        "character='" + c.toString() + "'")
            }
            return mTerm!!.dispatchCharacter(modifiers, c)
        }

        return false
    }

    fun setTerminal(term: Terminal) {
        mTerm = term
    }

    companion object {
        private val TAG = "TerminalKeys"
        private val DEBUG = true
        // Taken from vterm_input.h
        // TODO: Consider setting these via jni
        val VTERM_KEY_NONE = 0
        val VTERM_KEY_ENTER = 1
        val VTERM_KEY_TAB = 2
        val VTERM_KEY_BACKSPACE = 3
        val VTERM_KEY_ESCAPE = 4
        val VTERM_KEY_UP = 5
        val VTERM_KEY_DOWN = 6
        val VTERM_KEY_LEFT = 7
        val VTERM_KEY_RIGHT = 8
        val VTERM_KEY_INS = 9
        val VTERM_KEY_DEL = 10
        val VTERM_KEY_HOME = 11
        val VTERM_KEY_END = 12
        val VTERM_KEY_PAGEUP = 13
        val VTERM_KEY_PAGEDOWN = 14

        val VTERM_KEY_FUNCTION_0 = 256
        val VTERM_KEY_FUNCTION_MAX = VTERM_KEY_FUNCTION_0 + 255

        val VTERM_KEY_KP_0 = 512
        val VTERM_KEY_KP_1 = 513
        val VTERM_KEY_KP_2 = 514
        val VTERM_KEY_KP_3 = 515
        val VTERM_KEY_KP_4 = 516
        val VTERM_KEY_KP_5 = 517
        val VTERM_KEY_KP_6 = 518
        val VTERM_KEY_KP_7 = 519
        val VTERM_KEY_KP_8 = 520
        val VTERM_KEY_KP_9 = 521
        val VTERM_KEY_KP_MULT = 522
        val VTERM_KEY_KP_PLUS = 523
        val VTERM_KEY_KP_COMMA = 524
        val VTERM_KEY_KP_MINUS = 525
        val VTERM_KEY_KP_PERIOD = 526
        val VTERM_KEY_KP_DIVIDE = 527
        val VTERM_KEY_KP_ENTER = 528
        val VTERM_KEY_KP_EQUAL = 529

        val VTERM_MOD_NONE = 0x00
        val VTERM_MOD_SHIFT = 0x01
        val VTERM_MOD_ALT = 0x02
        val VTERM_MOD_CTRL = 0x04

        fun getModifiers(event: KeyEvent): Int {
            var mod = 0
            if (event.isCtrlPressed) {
                mod = mod or VTERM_MOD_CTRL
            }
            if (event.isAltPressed) {
                mod = mod or VTERM_MOD_ALT
            }
            if (event.isShiftPressed) {
                mod = mod or VTERM_MOD_SHIFT
            }
            return mod
        }

        fun getKey(event: KeyEvent): Int {
            when (event.keyCode) {
                KeyEvent.KEYCODE_ENTER -> return VTERM_KEY_ENTER
                KeyEvent.KEYCODE_TAB -> return VTERM_KEY_TAB
                KeyEvent.KEYCODE_DEL -> return VTERM_KEY_BACKSPACE
                KeyEvent.KEYCODE_ESCAPE -> return VTERM_KEY_ESCAPE
                KeyEvent.KEYCODE_DPAD_UP -> return VTERM_KEY_UP
                KeyEvent.KEYCODE_DPAD_DOWN -> return VTERM_KEY_DOWN
                KeyEvent.KEYCODE_DPAD_LEFT -> return VTERM_KEY_LEFT
                KeyEvent.KEYCODE_DPAD_RIGHT -> return VTERM_KEY_RIGHT
                KeyEvent.KEYCODE_INSERT -> return VTERM_KEY_INS
                KeyEvent.KEYCODE_FORWARD_DEL -> return VTERM_KEY_DEL
                KeyEvent.KEYCODE_MOVE_HOME -> return VTERM_KEY_HOME
                KeyEvent.KEYCODE_MOVE_END -> return VTERM_KEY_END
                KeyEvent.KEYCODE_PAGE_UP -> return VTERM_KEY_PAGEUP
                KeyEvent.KEYCODE_PAGE_DOWN -> return VTERM_KEY_PAGEDOWN
                else -> return 0
            }
        }

        fun getKeyName(key: Int): String {
            when (key) {
                VTERM_KEY_ENTER -> return "VTERM_KEY_ENTER"
                VTERM_KEY_TAB -> return "VTERM_KEY_TAB"
                VTERM_KEY_BACKSPACE -> return "VTERM_KEY_BACKSPACE"
                VTERM_KEY_ESCAPE -> return "VTERM_KEY_ESCAPE"
                VTERM_KEY_UP -> return "VTERM_KEY_UP"
                VTERM_KEY_DOWN -> return "VTERM_KEY_DOWN"
                VTERM_KEY_LEFT -> return "VTERM_KEY_LEFT"
                VTERM_KEY_RIGHT -> return "VTERM_KEY_RIGHT"
                VTERM_KEY_INS -> return "VTERM_KEY_INS"
                VTERM_KEY_DEL -> return "VTERM_KEY_DEL"
                VTERM_KEY_HOME -> return "VTERM_KEY_HOME"
                VTERM_KEY_END -> return "VTERM_KEY_END"
                VTERM_KEY_PAGEUP -> return "VTERM_KEY_PAGEUP"
                VTERM_KEY_PAGEDOWN -> return "VTERM_KEY_PAGEDOWN"
                VTERM_KEY_NONE -> return "VTERM_KEY_NONE"
                else -> return "UNKNOWN KEY"
            }
        }
    }
}
