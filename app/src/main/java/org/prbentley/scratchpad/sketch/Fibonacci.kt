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

import java.time.Duration
import org.prbentley.scratchpad.BaseSketch
import org.prbentley.scratchpad.Sketch
import org.prbentley.scratchpad.SliderWidget
import org.prbentley.scratchpad.Widget

/**
 * Terribly slow Fibonacci...O(n!) with a sleep thrown in for good measure,
 * but demonstrates an interruptible sketch.
 */
@Sketch
class Fibonacci : BaseSketch() {
    private val iterations = SliderWidget("Number of iterations", 1..20, 15)
    private val delayMs = SliderWidget("Delay (ms)", 0..20, 1)

    override val widgets = listOf<Widget>(iterations, delayMs)

    override fun go() {
        for (i in 1..iterations.value) {
            out("$i -> ${fib(i)}")
            if (!shouldContinue) {
                break
            }
        }
    }

    private fun fib(i: Int): Int {
        sleep(Duration.ofMillis(delayMs.value.toLong()))
        return when (i) {
            1, 2 -> 1
            else -> fib(i - 1) + fib(i - 2)
        }
    }
}
