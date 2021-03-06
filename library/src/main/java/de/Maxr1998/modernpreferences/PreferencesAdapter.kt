/*
 * Copyright (C) 2018 Max Rumpf alias Maxr1998
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.Maxr1998.modernpreferences

import android.os.Parcelable
import android.preference.Preference
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.annotation.MainThread
import androidx.core.view.get
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.Maxr1998.modernpreferences.helpers.emptyScreen
import de.Maxr1998.modernpreferences.preferences.CategoryHeader
import de.Maxr1998.modernpreferences.preferences.CollapsePreference
import kotlinx.android.parcel.Parcelize
import java.util.*

class PreferencesAdapter(root: PreferenceScreen? = null) : RecyclerView.Adapter<PreferencesAdapter.ViewHolder>() {

    private val screenStack: Stack<PreferenceScreen> = Stack<PreferenceScreen>().apply {
        push(emptyScreen)
    }

    val currentScreen: PreferenceScreen
        get() = screenStack.peek()

    /**
     * Listener which will be notified of screen change events
     *
     * Will dispatch the initial state when attached.
     */
    var onScreenChangeListener: OnScreenChangeListener? = null
        set(value) {
            field = value
            field?.onScreenChanged(currentScreen, isInSubScreen())
        }

    var secondScreenAdapter: PreferencesAdapter? = null

    init {
        root?.let(::setRootScreen)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        if (recyclerView.layoutManager !is LinearLayoutManager)
            throw UnsupportedOperationException("ModernAndroidPreferences requires a LinearLayoutManager")
    }

    @MainThread
    fun setRootScreen(root: PreferenceScreen) {
        currentScreen.adapter = null
        while (screenStack.peek() != emptyScreen) {
            screenStack.pop()
        }
        screenStack.push(root)
        currentScreen.adapter = this
        notifyDataSetChanged()
        onScreenChangeListener?.onScreenChanged(root, false)
    }

    @MainThread
    private fun openScreen(screen: PreferenceScreen) {
        secondScreenAdapter?.setRootScreen(screen) ?: /* ELSE */ run {
            currentScreen.adapter = null
            screenStack.push(screen)
            currentScreen.adapter = this
            notifyDataSetChanged()
        }
        onScreenChangeListener?.onScreenChanged(screen, true)
    }

    fun isInSubScreen() = screenStack.size > 2

    /**
     * If possible, return to the previous screen.
     *
     * @return true if it returned to an earlier screen, false if we're already at the root
     */
    @MainThread
    fun goBack(): Boolean {
        if (secondScreenAdapter?.goBack() == true) // Check if the second screen can still go back
            return true
        currentScreen.adapter = null
        if (isInSubScreen()) { // If we're in a sub-screen...
            val oldScreen = screenStack.pop() // ...remove current screen from stack
            currentScreen.adapter = this
            notifyDataSetChanged()
            onScreenChangeListener?.onScreenChanged(currentScreen, isInSubScreen())
            for (i in 0 until oldScreen.size()) {
                val p = oldScreen[i]
                if (p.javaClass == CollapsePreference::class.java)
                    (p as CollapsePreference).reset()
            }
            return true
        }
        return false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val layout = if (viewType == CategoryHeader.RESOURCE_CONST) R.layout.map_preference_category else R.layout.map_preference
        val view = layoutInflater.inflate(layout, parent, false)
        if (viewType > 0)
            layoutInflater.inflate(viewType, view.findViewById(R.id.map_widget_frame), true)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pref = currentScreen[position]
        pref.bindViews(holder)

        holder.itemView.setOnClickListener {
            // Item was clicked, check enabled state (not for PreferenceScreens) and send click event
            if (pref is PreferenceScreen) {
                openScreen(pref)
            } else if (!pref.enabled) return@setOnClickListener

            pref.performClick(holder)
        }
    }

    override fun getItemCount() = currentScreen.size()

    @LayoutRes
    override fun getItemViewType(position: Int) = currentScreen[position].getWidgetLayoutResource()

    /**
     * Restores the last scroll position if needed and (re-)attaches this adapter's scroll listener.
     *
     * Should be called from [OnScreenChangeListener.onScreenChanged].
     */
    fun restoreAndObserveScrollPosition(preferenceView: RecyclerView) {
        if (currentScreen.scrollPosition != 0 || currentScreen.scrollOffset != 0) {
            (preferenceView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                    currentScreen.scrollPosition,
                    currentScreen.scrollOffset
            )
        }
        preferenceView.removeOnScrollListener(scrollListener) // We don't want to be added twice
        preferenceView.addOnScrollListener(scrollListener)
    }

    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(r: RecyclerView, state: Int) {
            if (state == RecyclerView.SCROLL_STATE_IDLE) currentScreen.apply {
                scrollPosition = (r.layoutManager as LinearLayoutManager)
                        .findFirstCompletelyVisibleItemPosition()
                scrollOffset = r.findViewHolderForAdapterPosition(scrollPosition)?.itemView?.top ?: 0
            }
        }
    }

    /**
     * Common ViewHolder in [PreferencesAdapter] for every [Preference] object/every preference extending it
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val root: ViewGroup get() = itemView as ViewGroup
        val iconFrame: View = itemView.findViewById(R.id.map_icon_frame)
        val icon: ImageView? = itemView.findViewById(android.R.id.icon)
        val title: TextView = itemView.findViewById(android.R.id.title)
        val summary: TextView? = itemView.findViewById(android.R.id.summary)
        val widget: View? = itemView.findViewById<ViewGroup>(R.id.map_widget_frame)?.getChildAt(0)

        internal fun setEnabledState(enabled: Boolean) {
            setEnabledStateRecursive(itemView, enabled)
        }

        private fun setEnabledStateRecursive(v: View, enabled: Boolean) {
            v.isEnabled = enabled
            if (v is ViewGroup) {
                for (i in v.childCount - 1 downTo 0) {
                    setEnabledStateRecursive(v[i], enabled)
                }
            }
        }
    }

    /**
     * An interface to notify observers in [PreferencesAdapter] of screen change events,
     * when a sub-screen was opened or closed
     */
    interface OnScreenChangeListener {
        fun onScreenChanged(screen: PreferenceScreen, subScreen: Boolean)
    }

    fun getSavedState(): SavedState {
        val screenPath = IntArray(screenStack.size - 2)
        for (i in 1 until screenStack.size) {
            if (i > 1) screenPath[i - 2] = screenStack[i].screenPosition
        }
        return SavedState(screenPath)
    }

    /**
     * Loads the specified state into this adapter
     *
     * @return whether the state could be loaded
     */
    @MainThread
    fun loadSavedState(state: SavedState): Boolean {
        if (screenStack.size != 2) return false
        state.screenPath.forEach {
            val screen = currentScreen[it]
            if (screen is PreferenceScreen)
                screenStack.push(screen)
            else return@forEach
        }
        currentScreen.adapter = this
        notifyDataSetChanged()
        return true
    }

    @Parcelize
    data class SavedState(val screenPath: IntArray) : Parcelable {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return javaClass == other?.javaClass && screenPath.contentEquals((other as SavedState).screenPath)
        }

        override fun hashCode(): Int {
            return screenPath.contentHashCode()
        }
    }
}