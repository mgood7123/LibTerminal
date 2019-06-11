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

import android.graphics.Color

/**
 * Single terminal session backed by a pseudo terminal on the local device.
 */
class Terminal {

    val key: Int

    private val mNativePtr: Long
    private val mThread: Thread

    // TODO: hook up to title passed through termprop
    val title: String

    private var mClient: TerminalClient? = null

    var cursorVisible: Boolean = false
        private set
    var cursorRow: Int = 0
        private set
    var cursorCol: Int = 0
        private set

    private val mCallbacks = object : a.o.s.p.libterminal.TerminalCallbacks() {
        override fun damage(startRow: Int, endRow: Int, startCol: Int, endCol: Int): Int {
            if (mClient != null) {
                mClient!!.onDamage(startRow, endRow, startCol, endCol)
            }
            return 1
        }

        override fun moveRect(destStartRow: Int, destEndRow: Int, destStartCol: Int, destEndCol: Int,
                              srcStartRow: Int, srcEndRow: Int, srcStartCol: Int, srcEndCol: Int): Int {
            if (mClient != null) {
                mClient!!.onMoveRect(destStartRow, destEndRow, destStartCol, destEndCol, srcStartRow,
                        srcEndRow, srcStartCol, srcEndCol)
            }
            return 1
        }

        override fun moveCursor(posRow: Int, posCol: Int, oldPosRow: Int, oldPosCol: Int, visible: Int): Int {
            cursorVisible = visible != 0
            cursorRow = posRow
            cursorCol = posCol
            if (mClient != null) {
                mClient!!.onMoveCursor(posRow, posCol, oldPosRow, oldPosCol, visible)
            }
            return 1
        }

        override fun bell(): Int {
            if (mClient != null) {
                mClient!!.onBell()
            }
            return 1
        }
    }

    val rows: Int
        get() = nativeGetRows(mNativePtr)

    val cols: Int
        get() = nativeGetCols(mNativePtr)

    val scrollRows: Int
        get() = nativeGetScrollRows(mNativePtr)

    /**
     * Represents a run of one or more `VTermScreenCell` which all have
     * the same formatting.
     */
    class CellRun {
        internal var data: CharArray? = null
        internal var dataSize: Int = 0
        internal var colSize: Int = 0

        internal var bold: Boolean = false
        internal var underline: Int = 0
        internal var blink: Boolean = false
        internal var reverse: Boolean = false
        internal var strike: Boolean = false
        internal var font: Int = 0

        internal var fg = Color.CYAN
        internal var bg = Color.DKGRAY
    }

    // NOTE: clients must not call back into terminal while handling a callback,
    // since native mutex isn't reentrant.
    interface TerminalClient {
        fun onDamage(startRow: Int, endRow: Int, startCol: Int, endCol: Int)
        fun onMoveRect(destStartRow: Int, destEndRow: Int, destStartCol: Int, destEndCol: Int,
                       srcStartRow: Int, srcEndRow: Int, srcStartCol: Int, srcEndCol: Int)

        fun onMoveCursor(posRow: Int, posCol: Int, oldPosRow: Int, oldPosCol: Int, visible: Int)
        fun onBell()
    }

    init {
        mNativePtr = nativeInit(mCallbacks)
        key = sNumber++
        title = "$TAG $key"
        mThread = object : Thread(title) {
            override fun run() {
                nativeRun(mNativePtr)
            }
        }
    }

    /**
     * Start thread which internally forks and manages the pseudo terminal.
     */
    fun start() {
        mThread.start()
    }

    fun destroy() {
        if (nativeDestroy(mNativePtr) != 0) {
            throw IllegalStateException("destroy failed")
        }
    }

    fun setClient(client: TerminalClient?) {
        mClient = client
    }

    fun resize(rows: Int, cols: Int, scrollRows: Int) {
        if (nativeResize(mNativePtr, rows, cols, scrollRows) != 0) {
            throw IllegalStateException("resize failed")
        }
    }

    fun getCellRun(row: Int, col: Int, run: CellRun) {
        if (nativeGetCellRun(mNativePtr, row, col, run) != 0) {
            throw IllegalStateException("getCell failed")
        }
    }

    fun dispatchKey(modifiers: Int, key: Int): Boolean {
        return nativeDispatchKey(mNativePtr, modifiers, key)
    }

    fun dispatchCharacter(modifiers: Int, character: Int): Boolean {
        return nativeDispatchCharacter(mNativePtr, modifiers, character)
    }

    fun dispatchString(modifiers: Int, character: String): Boolean {
        var len = 0
        val lenActual = character.length
        var index = 0
        var result = true
        character.forEach {
            result = dispatchCharacter(modifiers, it.toInt())
            if (result) len++
        }
        return len == lenActual
    }

    companion object {
        val TAG = "Terminal"

        private var sNumber = 0

        init {
            System.loadLibrary("jni_terminal")
        }

        @JvmStatic
        private external fun nativeInit(callbacks: a.o.s.p.libterminal.TerminalCallbacks): Long
        @JvmStatic
        private external fun nativeDestroy(ptr: Long): Int

        @JvmStatic
        private external fun nativeRun(ptr: Long): Int
        @JvmStatic
        private external fun nativeResize(ptr: Long, rows: Int, cols: Int, scrollRows: Int): Int
        @JvmStatic
        private external fun nativeGetCellRun(ptr: Long, row: Int, col: Int, run: CellRun): Int
        @JvmStatic
        private external fun nativeGetRows(ptr: Long): Int
        @JvmStatic
        private external fun nativeGetCols(ptr: Long): Int
        @JvmStatic
        private external fun nativeGetScrollRows(ptr: Long): Int

        @JvmStatic
        private external fun nativeDispatchKey(ptr: Long, modifiers: Int, key: Int): Boolean
        @JvmStatic
        private external fun nativeDispatchCharacter(ptr: Long, modifiers: Int, character: Int): Boolean
    }
}
