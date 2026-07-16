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

import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.measureTime
import org.prbentley.scratchpad.BaseSketch
import org.prbentley.scratchpad.Sketch

@Sketch
class Benchmark : BaseSketch() {
    override fun go() {
        benchmark { n ->
            sort(n)
        }
    }

    private fun benchmark(function: (n: Int) -> Unit) {
        val sizes = listOf(20, 200, 2000, 10000)
        val cycles = 0 until 25
        val n = 20

        for (n in sizes) {
            var totalDuration: Duration = Duration.ZERO
            for (cycle in cycles) {
                totalDuration += measureTime {
                    function(n)
                }
            }
            val cycleDuration = totalDuration / cycles.last
            val totalDurationString = totalDuration.toString(2)
            val cycleDurationString = cycleDuration.toString(2)

            out("$n:, total: $totalDurationString, cycle: $cycleDurationString")
        }
    }

    private fun sort(n: Int) {
        val list = mutableListOf<Int>()
        for (i in n downTo 0) {
            list.add(i)
        }
        list.sort()
    }
}

private fun Duration.toString(decimals: Int): String {
    val unit = when {
        this < 1.microseconds -> DurationUnit.NANOSECONDS
        this < 1.milliseconds -> DurationUnit.MICROSECONDS
        this < 1.seconds -> DurationUnit.MILLISECONDS
        else -> DurationUnit.SECONDS
    }
    return toString(unit, decimals)
}
