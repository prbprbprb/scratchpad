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

import android.util.Log
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.PrintWriter
import java.io.StringWriter
import org.prbentley.scratchpad.LoggerImpl.ComposableLogSink

interface Logger : Appendable {
    var composable: ComposableLogSink?

    fun log(o: Any) {
        log(o.toString())
    }

    fun log(throwable: Throwable) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        log(sw.toString())
    }

    fun log(line: String) {
        log(line, false)
    }

    fun verbose(o: Any) {
        verbose(o.toString())
    }

    fun verbose(throwable: Throwable) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        verbose(sw.toString())
    }

    fun verbose(line: String) {
        log(line, true)
    }

    fun log(line: String, verbose: Boolean)

    fun clear(): LoggerImpl

    companion object {
        private var composable: LoggerImpl.LogSink? = null

        @Synchronized
        fun get(tag: String): Logger {
            if (composable == null) {
                composable = ComposableLogSink()
            }
            return LoggerImpl(LoggerImpl.LogcatSink(tag), composable!!)
        }
    }
}

class LoggerImpl(vararg logSinks: LogSink) : Logger {
    private val output = mutableListOf<LogLine>()
    private var startTime = System.currentTimeMillis()
    private val sinks = mutableListOf<LogSink>()
    override var composable: ComposableLogSink? = null

    init {
        logSinks.forEach(::addSink)
    }

    fun addSink(logSink: LogSink) {
        sinks.add(logSink)
        if (logSink is ComposableLogSink) {
            // euw
            if (composable != null) {
                error("Only one composable sink allowed")
            }
            composable = logSink
        }
    }

    override fun log(line: String, verbose: Boolean) {
        val logLine = LogLine(line, System.currentTimeMillis() - startTime, verbose)
        output.add(logLine)
        sinks.forEach { it.output(logLine) }
    }

    override fun clear(): LoggerImpl {
        output.clear()
        resetTime()
        redraw()
        return this
    }

    private fun resetTime() {
        startTime = System.currentTimeMillis()
    }

    private fun redraw() {
        sinks.forEach { it.redraw(getAll()) }
    }

    private fun getAll(): List<LogLine> = output

    override fun append(csq: CharSequence?): java.lang.Appendable {
        csq?.let { log(it) }
        return this
    }

    override fun append(csq: CharSequence?, start: Int, end: Int): java.lang.Appendable {
        csq?.let {
            return append(it.subSequence(start, end))
        }
        throw NullPointerException()
    }

    override fun append(c: Char): java.lang.Appendable = append(c.toString())

    /** Log lines. */
    data class LogLine(val text: String, val time: Long, val verbose: Boolean) {

        fun format(isVerbose: Boolean, timeFormat: String) =
            if (isVerbose || !verbose) "${timeFormat.format(time)}$text" else null
    }

    /** Sink classes. */
    abstract class LogSink {
        abstract fun output(line: LogLine)

        open fun redraw(lines: List<LogLine>) {
            // Nothing by default.
        }
    }

    /** Logcat sink. */
    class LogcatSink(private val tag: String) : LogSink() {
        override fun output(line: LogLine) {
            if (line.verbose) {
                Log.v(tag, line.text)
            } else {
                Log.i(tag, line.text)
            }
        }
    }

    /** Composable sink. */
    class ComposableLogSink : LogSink() {
        val defaultTimeFormat = "%4dms "

        // e.g. logger.composable?.verbose = true to show verbose lines
        var verbose by mutableStateOf(false)

        // e.g. logger.composable?.timeFormat = "" to disable timestamps
        var timeFormat by mutableStateOf(defaultTimeFormat)
        private val lines = mutableStateListOf<LogLine>()

        override fun output(line: LogLine) {
            lines.add(line)
        }

        override fun redraw(lines: List<LogLine>) {
            this.lines.clear()
            this.lines.addAll(lines)
        }

        @Composable
        fun Compose(modifier: Modifier) {
            ScrollableLogList(lines, verbose, timeFormat)
        }
    }
}

@Composable
fun LogList(
    lines: SnapshotStateList<LoggerImpl.LogLine>,
    verbose: Boolean,
    timeFormat: String,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(lines) { line ->
            line.format(verbose, timeFormat)?.let { text ->
                LogLineEntry(text)
            }
        }
    }
}

@Composable
fun LogLineEntry(text: String) {
    val horizontalScrollState = rememberScrollState()
    Text(
        text = text,
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(horizontalScrollState)
            .padding(vertical = 1.dp, horizontal = 2.dp),
        softWrap = false
    )
}

@Composable
fun ScrollableLogList(
    lines: SnapshotStateList<LoggerImpl.LogLine>,
    verbose: Boolean,
    timeFormat: String,
    modifier: Modifier = Modifier
) {
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()

    val logText by remember(lines, verbose, timeFormat) {
        derivedStateOf {
            lines
                .mapNotNull { it.format(verbose, timeFormat) }
                .joinToString("\n")
        }
    }

    // Automatically scroll to the bottom when new lines are added
    LaunchedEffect(logText) {
        verticalScrollState.animateScrollTo(verticalScrollState.maxValue)
    }

    Text(
        text = logText,
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        softWrap = false, // Critical: prevents wrapping
        modifier = modifier
            .fillMaxSize() // Fill the area provided by the parent
            // Apply horizontal scrolling to the entire Text block
            .horizontalScroll(horizontalScrollState)
            .verticalScroll(verticalScrollState) // Apply vertical scrolling
            .padding(vertical = 1.dp, horizontal = 2.dp)
    )
}
