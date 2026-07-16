/*
 * Copyright 2026 Pete Bentley <pete@sorted.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.prbentley.scratchpad

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class SliderWidget(val label: String, val range: IntRange, initialValue: Int = range.first) :
    Widget() {

    // The single source of truth for the slider's value
    var value by mutableStateOf(initialValue.coerceIn(range))

    @Composable
    override fun Compose() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = value.toString(),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Slider(
                value = value.toFloat(),
                onValueChange = { value = it.toInt() },
                valueRange = range.first.toFloat()..range.last.toFloat(),
                // Use steps to make the slider "snap" to integer values
                steps = range.last - range.first - 1,
                // XXX enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
