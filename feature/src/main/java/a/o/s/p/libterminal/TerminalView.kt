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

import a.o.s.p.libterminal.Terminal.Companion.TAG

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.BaseAdapter
import android.widget.ListView

import a.o.s.p.libterminal.Terminal.CellRun
import a.o.s.p.libterminal.Terminal.TerminalClient

/**
 * Rendered contents of a [Terminal] session.
 */
class TerminalView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = R.attr.listViewStyle) : ListView(context, attrs, defStyle) {

    // Populate any current settings
    var terminal: Terminal? = null
        set(term) {
            val orig = terminal
            orig?.setClient(null)
            field = term
            mScrolled = false
            if (term != null) {
                term.setClient(mClient)
                mTermKeys.setTerminal(term)

                mMetrics.cursorPaint.color = -0xf0f10
                mRows = terminal!!.rows
                mCols = terminal!!.cols
                mScrollRows = terminal!!.scrollRows
                mAdapter.notifyDataSetChanged()
            }
        }

    private var mScrolled: Boolean = false

    private var mRows: Int = 0
    private var mCols: Int = 0
    private var mScrollRows: Int = 0

    private val mMetrics = TerminalMetrics()
    private val mTermKeys = TerminalKeys()

    private val mClickListener = OnItemClickListener { parent, v, pos, id ->
        if (parent.requestFocus()) {
            val imm = parent.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(parent, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private val mDamageRunnable = Runnable {
        invalidateViews()
        if (SCROLL_ON_DAMAGE) {
            scrollToBottom(true)
        }
    }

    private val mAdapter = object : BaseAdapter() {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view: TerminalLineView
            if (convertView != null) {
                view = convertView as TerminalLineView
            } else {
                view = TerminalLineView(parent.context, terminal, mMetrics)
            }

            view.pos = position
            view.row = posToRow(position)
            view.cols = mCols
            return view
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getItem(position: Int): Any? {
            return null
        }

        override fun getCount(): Int {
            return if (terminal != null) {
                mRows + mScrollRows
            } else {
                0
            }
        }
    }

    private val mClient = object : TerminalClient {
        override fun onDamage(startRow: Int, endRow: Int, startCol: Int, endCol: Int) {
            post(mDamageRunnable)
        }

        override fun onMoveRect(destStartRow: Int, destEndRow: Int, destStartCol: Int, destEndCol: Int,
                                srcStartRow: Int, srcEndRow: Int, srcStartCol: Int, srcEndCol: Int) {
            post(mDamageRunnable)
        }

        override fun onMoveCursor(posRow: Int, posCol: Int, oldPosRow: Int, oldPosCol: Int, visible: Int) {
            post(mDamageRunnable)
        }

        override fun onBell() {
            Log.i(TAG, "DING!")
        }
    }

    private val mKeyListener = OnKeyListener { v, keyCode, event ->
        val res = mTermKeys.onKey(v, keyCode, event)
        if (res && SCROLL_ON_INPUT) {
            scrollToBottom(true)
        }
        res
    }

    /**
     * Metrics shared between all [TerminalLineView] children. Locking
     * provided by main thread.
     */
    class TerminalMetrics {

        val bgPaint = Paint()
        val textPaint = Paint()
        val cursorPaint = Paint()

        /** Run of cells used when drawing  */
        val run: CellRun
        /** Screen coordinates to draw chars into  */
        val pos: FloatArray

        var charTop: Int = 0
        var charWidth: Int = 0
        var charHeight: Int = 0

        init {
            run = CellRun()
            run.data = CharArray(MAX_RUN_LENGTH)

            // Positions of each possible cell
            // TODO: make sure this works with surrogate pairs
            pos = FloatArray(MAX_RUN_LENGTH * 2)
            setTextSize(20f)
        }

        fun setTextSize(textSize: Float) {
            textPaint.typeface = Typeface.MONOSPACE
            textPaint.isAntiAlias = true
            textPaint.textSize = textSize

            // Read metrics to get exact pixel dimensions
            val fm = textPaint.fontMetrics
            charTop = Math.ceil(fm.top.toDouble()).toInt()

            val widths = FloatArray(1)
            textPaint.getTextWidths("X", widths)
            charWidth = Math.ceil(widths[0].toDouble()).toInt()
            charHeight = Math.ceil((fm.descent - fm.top).toDouble()).toInt()

            // Update drawing positions
            for (i in 0 until MAX_RUN_LENGTH) {
                pos[i * 2] = (i * charWidth).toFloat()
                pos[i * 2 + 1] = (-charTop).toFloat()
            }
        }

        companion object {
            private val MAX_RUN_LENGTH = 128
        }
    }

    init {

        background = null
        divider = null

        isFocusable = true
        isFocusableInTouchMode = true

        adapter = mAdapter
        setOnKeyListener(mKeyListener)

        onItemClickListener = mClickListener
    }

    private fun rowToPos(row: Int): Int {
        return row + mScrollRows
    }

    private fun posToRow(pos: Int): Int {
        return pos - mScrollRows
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        super.onRestoreInstanceState(state)
        mScrolled = true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!mScrolled) {
            scrollToBottom(false)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        val rows = h / mMetrics.charHeight
        val cols = w / mMetrics.charWidth
        val scrollRows = mScrollRows

        val sizeChanged = rows != mRows || cols != mCols || scrollRows != mScrollRows
        if (terminal != null && sizeChanged) {
            terminal!!.resize(rows, cols, scrollRows)

            mRows = rows
            mCols = cols
            mScrollRows = scrollRows

            mAdapter.notifyDataSetChanged()
        }
    }

    fun scrollToBottom(animate: Boolean) {
        val dur = if (animate) 250 else 0
        smoothScrollToPositionFromTop(count, 0, dur)
        mScrolled = true
    }

    fun setTextSize(textSize: Float) {
        mMetrics.setTextSize(textSize)

        // Layout will kick off terminal resize when needed
        requestLayout()
    }

    override fun onCheckIsTextEditor(): Boolean {
        return true
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        outAttrs.imeOptions = outAttrs.imeOptions or (EditorInfo.IME_FLAG_NO_EXTRACT_UI or
                EditorInfo.IME_FLAG_NO_ENTER_ACTION or
                EditorInfo.IME_ACTION_NONE)
        outAttrs.inputType = EditorInfo.TYPE_NULL
        return object : BaseInputConnection(this, false) {
            override fun deleteSurroundingText(leftLength: Int, rightLength: Int): Boolean {
                var k: KeyEvent
                if (rightLength == 0 && leftLength == 0) {
                    k = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL)
                    return this.sendKeyEvent(k)
                }
                for (i in 0 until leftLength) {
                    k = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL)
                    this.sendKeyEvent(k)
                }
                for (i in 0 until rightLength) {
                    k = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_FORWARD_DEL)
                    this.sendKeyEvent(k)
                }
                return true
            }
        }
    }

    companion object {
        private val LOGD = true

        private val SCROLL_ON_DAMAGE = false
        private val SCROLL_ON_INPUT = true
    }
}
