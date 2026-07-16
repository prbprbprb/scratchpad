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
package org.prbentley.scratchpad.ksp

class ValueMapper {
    private val substitutions = mutableMapOf<String, MutableList<String>>()

    fun interpolate(input: String) = interpolateLines(input.split("\n"))
        .joinToString("\n")

    fun interpolateLines(lines: List<String>) = lines.flatMap(::interpolateLine)

    fun interpolateLinex(line: String): List<String> {
        val subs = substitutions.entries
            .filter { line.contains(it.key) }
            .sortedBy { it.key }

        return if (subs.isNotEmpty()) {
            interpolateLines(subs.flatMap { it.value.map { v -> line.replace(it.key, v) } })
        } else {
            listOf(line)
        }
    }

    fun interpolateLine(line: String): List<String> {
        val matches = substitutions.keys
            .filter { line.contains(it) }
            .sorted()

        return expand(line, matches)
    }

    private fun expand(line: String, remainingMatches: List<String>): List<String> {
        if (remainingMatches.isEmpty()) {
            return listOf(line)
        }

        val currentMatch = remainingMatches[0]
        val nextMatches = remainingMatches.drop(1)

        if (!line.contains(currentMatch)) {
            return expand(line, nextMatches)
        }

        val values = substitutions[currentMatch] ?: return expand(line, nextMatches)

        return values.flatMap { value ->
            expand(line.replace(currentMatch, value), nextMatches)
        }
    }

    fun add(key: String, value: String) {
        substitutions.getOrPut(key) { mutableListOf() }.add(value)
    }

    fun add(key: String, values: List<String>) {
        substitutions.getOrPut(key) { mutableListOf() }.addAll(values)
    }
}
