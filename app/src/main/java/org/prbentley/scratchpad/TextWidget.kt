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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class TextWidget(val label: String, initialValue: String = "", val isEditable: Boolean = true) :
    Widget() {

    // Single source of truth for both UI and go() method
    var text by mutableStateOf(initialValue)

    @Composable
    override fun Compose() {
        if (isEditable) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                // XXX enabled = enabled // Inherited from your Widget base class
            )
        } else {
            // Read-only display mode
            Text(
                text = if (label.isEmpty()) text else "$label: $text",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
    }
}
