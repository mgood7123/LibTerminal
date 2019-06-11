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

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.PagerTitleStrip
import androidx.viewpager.widget.ViewPager
import android.util.Log
import android.util.SparseArray
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup

/**
 * Activity that displays all [Terminal] instances running in a bound
 * [TerminalService].
 */
class TerminalActivity : Activity() {

    private var mService: TerminalService? = null

    private var mPager: ViewPager? = null
    private var mTitles: PagerTitleStrip? = null

    private val mServiceConn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mService = (service as TerminalService.ServiceBinder).service

            val size = mService!!.terminals.size()
            Log.d(TAG, "Bound to service with $size active terminals")

            // Give ourselves at least one terminal session
            if (size == 0) {
                mService!!.createTerminal()
            }

            // Bind UI to known terminals
            mTermAdapter.notifyDataSetChanged()
            invalidateOptionsMenu()
            val term = mService!!.terminals.get(0)
            term.dispatchString(0,
                    """
                        echo STANDARD INPUT >& 0
                        echo STANDARD OUTPUT >& 1
                        echo STANDARD ERROR >& 2

                        """.trimIndent())
            Thread {
                (1..Int.MAX_VALUE).forEach {
                    term.dispatchString(0, "echo count $it\n")
                }
            }.start()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mService = null
            throw RuntimeException("Service in same process disconnected?")
        }
    }

    private val mTermAdapter = object : PagerAdapter() {
        private val mSavedState = SparseArray<SparseArray<Parcelable>>()

        override fun getCount(): Int {
            return if (mService != null) {
                mService!!.terminals.size()
            } else {
                0
            }
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val view = TerminalView(container.context)
            view.id = android.R.id.list

            val term = mService!!.terminals.valueAt(position)
            view.terminal = term

            val state = mSavedState.get(term.key)
            if (state != null) {
                view.restoreHierarchyState(state)
            }

            container.addView(view)
            view.requestFocus()
            return view
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            val view = `object` as TerminalView

            val key = view.terminal!!.key
            var state: SparseArray<Parcelable>? = mSavedState.get(key)
            if (state == null) {
                state = SparseArray()
                mSavedState.put(key, state)
            }
            view.saveHierarchyState(state)

            view.terminal = null
            container.removeView(view)
        }

        override fun getItemPosition(`object`: Any): Int {
            val view = `object` as TerminalView
            val key = view.terminal!!.key
            val index = mService!!.terminals.indexOfKey(key)
            return if (index == -1) {
                PagerAdapter.POSITION_NONE
            } else {
                index
            }
        }

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view === `object`
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return mService!!.terminals.valueAt(position).title
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity)

        mPager = findViewById<View>(R.id.pager) as ViewPager
        mTitles = findViewById<View>(R.id.titles) as PagerTitleStrip

        mPager!!.adapter = mTermAdapter
    }

    override fun onStart() {
        super.onStart()
        bindService(
                Intent(this, TerminalService::class.java), mServiceConn, Context.BIND_AUTO_CREATE
        )
    }

    override fun onStop() {
        super.onStop()
        unbindService(mServiceConn)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.menu_close_tab).isEnabled = mTermAdapter.count > 0
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.menu_new_tab) {
            mService!!.createTerminal()
            mTermAdapter.notifyDataSetChanged()
            invalidateOptionsMenu()
            val index = mService!!.terminals.size() - 1
            mPager!!.setCurrentItem(index, true)
            return true
        } else if (itemId == R.id.menu_close_tab) {
            val index = mPager!!.currentItem
            val key = mService!!.terminals.keyAt(index)
            mService!!.destroyTerminal(key)
            mTermAdapter.notifyDataSetChanged()
            invalidateOptionsMenu()
            return true
        }
        return false
    }
}
