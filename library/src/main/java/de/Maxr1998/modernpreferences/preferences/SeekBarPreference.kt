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

import android.view.LayoutInflater
import android.widget.SeekBar
import android.widget.Space
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import de.Maxr1998.modernpreferences.Preference
import de.Maxr1998.modernpreferences.PreferencesAdapter
import de.Maxr1998.modernpreferences.R
import de.Maxr1998.modernpreferences.helpers.onSeek

class SeekBarPreference(key: String) : Preference(key) {

    var min = 0
    var max = 0
    var step = 1
        set(value) {
            if (value <= 0)
                throw IllegalArgumentException("Stepping value must be >= 1")
            field = value
        }
    var formatter: (Int) -> String = Int::toString

    override fun getWidgetLayoutResource() = R.layout.map_preference_widget_empty

    override fun bindViews(holder: PreferencesAdapter.ViewHolder) {
        super.bindViews(holder)
        holder.root.apply {
            background = null
            clipChildren = false
        }
        holder.title.updateLayoutParams<ConstraintLayout.LayoutParams> {
            goneBottomMargin = 0
        }
        holder.summary?.updateLayoutParams<ConstraintLayout.LayoutParams> {
            bottomMargin = 0
        }
        val widget = holder.widget as Space
        val inflater = LayoutInflater.from(widget.context)
        val sb = (widget.tag
                ?: inflater.inflate(R.layout.map_preference_widget_seekbar, holder.root)
                        .findViewById(android.R.id.progress)) as SeekBar
        val tv = (sb.tag ?: holder.itemView.findViewById(R.id.progress_text)) as TextView
        widget.tag = sb.apply {
            isEnabled = enabled
            max = (this@SeekBarPreference.max - this@SeekBarPreference.min) / step
            progress = (getInt(0) - this@SeekBarPreference.min) / step
            onSeek { v, done ->
                val value = this@SeekBarPreference.min + (v * step)
                tv.text = formatter(value)
                if (done) commitInt(value)
            }
        }
        sb.tag = tv.apply {
            isEnabled = enabled
            text = formatter(min + (sb.progress * step))
        }
    }
}