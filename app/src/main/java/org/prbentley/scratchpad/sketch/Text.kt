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
package org.prbentley.scratchpad.sketch

import androidx.compose.ui.semantics.text
import org.prbentley.scratchpad.BaseSketch
import org.prbentley.scratchpad.Sketch
import org.prbentley.scratchpad.TextWidget
import org.prbentley.scratchpad.Widget

@Sketch
class Text : BaseSketch() {
    private val userName = TextWidget("Enter your name", "Pete")

    private val statusDisplay = TextWidget("Current Status", "Idle", isEditable = false)

    override val widgets = listOf<Widget>(userName, statusDisplay)

    override fun go() {
        out("Hello, ${userName.text}!")

        // Update the display widget programmatically
        statusDisplay.text = "Processing..."

        statusDisplay.text = "Finished"
    }
}
