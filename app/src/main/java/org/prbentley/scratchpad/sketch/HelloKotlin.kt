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

import java.security.Security
import org.prbentley.scratchpad.BaseSketch
import org.prbentley.scratchpad.BooleanWidget
import org.prbentley.scratchpad.Sketch
import org.prbentley.scratchpad.Widget

@Sketch
class HelloKotlin : BaseSketch() {
    var properties = listOf(
        "java.version",
        "java.home",
        "os.name",
        "os.arch",
        "os.version",
        "user.home",
        "user.dir"
    )

    private val check1 = BooleanWidget("First", false)

    private val showProperties =
        BooleanWidget("Would you like to see some Java and Security properties?", true)

    override val widgets = listOf<Widget>(check1, showProperties)

    override fun go() {
        out("Hello World in Kotlin.")

        if (showProperties.checked) {
            out("\nProperties:")
            properties.forEach { property ->
                out("%s: %s", property, System.getProperty(property))
            }

            out("\nSecurity Properties for Providers")
            for (i in 1..9) {
                Security.getProperty("security.provider.$i") ?.let { value ->
                    out("security.provider.$i: $value")
                }
            }
        }
    }
}
