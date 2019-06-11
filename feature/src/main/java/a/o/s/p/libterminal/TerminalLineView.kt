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
import android.graphics.Canvas
import android.graphics.Color
import android.util.Log
import android.view.View

import a.o.s.p.libterminal.TerminalView.TerminalMetrics

/**
 * Rendered contents of a single line of a [Terminal] session.
 */
class TerminalLineView(context: Context, private val mTerm: Terminal?, private val mMetrics: TerminalMetrics) : View(context) {
    var pos: Int = 0
    var row: Int = 0
    var cols: Int = 0

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(View.getDefaultSize(0, widthMeasureSpec),
                View.getDefaultSize(mMetrics.charHeight, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (mTerm == null) {
            Log.w(TAG, "onDraw() without a terminal")
            canvas.drawColor(Color.MAGENTA)
            return
        }

        val m = mMetrics

        var col = 0
        while (col < cols) {
            mTerm.getCellRun(row, col, m.run)

            m.bgPaint.color = m.run.bg
            m.textPaint.color = m.run.fg

            val x = col * m.charWidth
            val xEnd = x + m.run.colSize * m.charWidth

            canvas.save()
            canvas.translate(x.toFloat(), 0f)
            canvas.clipRect(0, 0, m.run.colSize * m.charWidth, m.charHeight)

            canvas.drawPaint(m.bgPaint)
            canvas.drawPosText(m.run.data!!, 0, m.run.dataSize, m.pos, m.textPaint)

            canvas.restore()

            col += m.run.colSize
        }

        if (mTerm.cursorVisible && mTerm.cursorRow == row) {
            canvas.save()
            canvas.translate((mTerm.cursorCol * m.charWidth).toFloat(), 0f)
            canvas.drawRect(0f, 0f, m.charWidth.toFloat(), m.charHeight.toFloat(), m.cursorPaint)
            canvas.restore()
        }

    }
}
