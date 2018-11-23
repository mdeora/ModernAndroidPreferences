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

package de.Maxr1998.modernpreferences.preferences

import android.graphics.Typeface
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import de.Maxr1998.modernpreferences.Preference
import de.Maxr1998.modernpreferences.PreferencesAdapter
import de.Maxr1998.modernpreferences.R

class ExpandableTextPreference(key: String) : Preference(key) {
    private var expanded = false

    var text: String? = null
    @StringRes
    var textRes: Int = -1
    var monospace = true

    override fun getWidgetLayoutResource() = R.layout.preference_widget_expand_arrow

    override fun bindViews(holder: PreferencesAdapter.ViewHolder) {
        super.bindViews(holder)
        val widget = holder.widget as ImageView
        val inflater = LayoutInflater.from(widget.context)
        val textView = (widget.tag ?: inflater.inflate(R.layout.preference_expand_text,
                holder.itemView as ViewGroup).findViewById(android.R.id.message)) as TextView
        widget.tag = textView
        textView.apply {
            if (titleRes != -1) setText(textRes) else text = this@ExpandableTextPreference.text
            typeface = if (monospace) Typeface.MONOSPACE else Typeface.SANS_SERIF
            val a = context.obtainStyledAttributes(intArrayOf(R.attr.expandableTextBackgroundColor))
            setBackgroundColor(a.getColor(0, ContextCompat.getColor(context,
                    R.color.expandableTextBackgroundColorDefault)))
            a.recycle()
        }

        refreshArrowState(widget)
        refreshTextExpandState(textView)
    }

    override fun onClick(holder: PreferencesAdapter.ViewHolder) {
        expanded = !expanded
        refreshArrowState(holder.widget as ImageView)
        refreshTextExpandState(holder.widget.tag as TextView)
    }

    /**
     * Update expand/collapse arrow
     */
    private fun refreshArrowState(widget: ImageView) {
        val drawableState = if (expanded) intArrayOf(android.R.attr.state_checked) else IntArray(0)
        widget.setImageState(drawableState, false)
    }

    private fun refreshTextExpandState(text: TextView) {
        TransitionManager.beginDelayedTransition(text.parent as ViewGroup, ChangeBounds())
        text.isVisible = expanded
    }
}