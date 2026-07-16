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

import android.content.Context

class SketchModel(override val logger: Logger, override val applicationContext: Context) :
    Scratchpad {

    internal val sketchMap: Map<String, Class<out BaseSketch>> =
        SketchProvider.sketches.associateBy { c -> c.simpleName }
    internal val sketchNames = sketchMap.keys.toList()
    internal val defaultSketch = SketchProvider.defaultSketchName ?: sketchNames[0]

    fun createSketch(sketchName: String, onFinished: () -> Unit = {}): BaseSketch {
        sketchMap[sketchName]?.let { cls ->
            val sketch = cls.getDeclaredConstructor().newInstance() // XXX try/catch
            sketch?.let {
                it.scratchpad = this
                it.logger = logger // XXX Set tag
                it.after = onFinished
            }
            return sketch
        }
        logger.log("Sketch not found: $sketchName")
        error("Not found")
    }
}
