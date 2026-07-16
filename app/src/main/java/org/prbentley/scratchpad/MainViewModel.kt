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

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

open class MainViewModel(application: Application) : AndroidViewModel(application) {
    internal val logger: Logger = Logger.get("ScratchPad")
    private val applicationContext: Context = getApplication<Application>().applicationContext
    val model = SketchModel(logger, applicationContext)
    internal val sketchNames = model.sketchNames
    internal val defaultSketch = model.defaultSketch

    private val _currentSketch = MutableStateFlow(
        model.createSketch(defaultSketch) { onSketchFinished() }
    )
    internal var currentSketch = _currentSketch.asStateFlow()
    internal var selectedSketch = defaultSketch

    private val _verboseLogging = MutableStateFlow(false)
    val verboseLogging = _verboseLogging.asStateFlow()

    private val _showTimestamps = MutableStateFlow(true)
    val showTimestamps = _showTimestamps.asStateFlow()

    internal fun setVerbosity(value: Boolean) {
        logger.composable?.verbose = value
        _verboseLogging.value = value
    }

    internal fun setShowTimestamps(value: Boolean) {
        logger.composable?.let { logger ->
            logger.timeFormat = if (value) logger.defaultTimeFormat else ""
        }
        _showTimestamps.value = value
    }

    internal fun runOrStop() = when (_currentSketch.value.isRunning) {
        true -> stop()
        false -> run()
    }

    private fun run() {
        _currentSketch.value.let { sketch ->
            if (!sketch.isRunning) {
                logger
                    .clear()
                    .verbose("Invoking ${sketch.name}")
                sketch.job = viewModelScope.launch(Dispatchers.IO) {
                    sketch.run()
                }
            }
        }
    }

    private fun stop() {
        _currentSketch.value.let { sketch ->
            if (sketch.isRunning) {
                logger.verbose("Stopping ${sketch.name}")
                sketch.stop()
            }
        }
    }

    internal fun selectSketch(name: String) {
        if (name != selectedSketch) {
            stop()
        }
        selectedSketch = name
        _currentSketch.value = model.createSketch(selectedSketch) { onSketchFinished() }
    }

    private fun onSketchFinished() {
        logger.verbose("Finished.")
        currentSketch.value.job = null
    }

    @Composable
    fun SketchUi() {
        currentSketch.collectAsState().value.apply {
            Ui()
        }
    }
}
