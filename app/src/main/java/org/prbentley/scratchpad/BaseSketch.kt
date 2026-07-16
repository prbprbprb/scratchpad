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
import android.widget.Toast
import androidx.compose.runtime.Composable
import java.time.Duration
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class BaseSketch {
    lateinit var scratchpad: Scratchpad
    lateinit var logger: Logger
    lateinit var after: () -> Unit

    var job: Job? = null
    var name: String = javaClass.simpleName
    var shouldContinue by atomic(true)
    private val _isRunningFlow = MutableStateFlow(false)
    val isRunningFlow = _isRunningFlow.asStateFlow()
    var isRunning: Boolean
        get() = _isRunningFlow.value
        set(value) {
            _isRunningFlow.value = value
        }

    open val widgets: List<Widget> = listOf()

    protected val applicationContext: Context
        get() = scratchpad.applicationContext

    protected fun out(text: String) {
        // XXX getLogger().log(logTag(), text);
        logger.log(text)
    }

    protected fun out(o: Any?) = out("$o")

    protected fun out(t: Throwable) {
        // XXX getLogger().log(logTag(), t);
        logger.log(t)
    }

    protected fun out(format: String, vararg args: Any?) {
        out(String.format(format, *args))
    }

    protected fun verbose(text: String) {
        // XXX getLogger().log(logTag(), text, true);
        logger.log(text, true)
    }

    protected fun verbose(format: String, vararg args: Any?) {
        verbose(String.format(format, *args))
    }

    protected fun clear() = logger.clear()

    protected fun toast(text: String?) {
        val toast = Toast.makeText(this.applicationContext, text, Toast.LENGTH_LONG)
        toast.show()
    }

    fun run() {
        try {
            shouldContinue = true
            isRunning = true
            go()
        } catch (_: InterruptedException) {
            out("Interrupted")
        } catch (e: Throwable) {
            out("Unexpected exception in $name")
            out(e)
        } finally {
            after()
            isRunning = false
        }
    }

    fun stop() {
        shouldContinue = false
        job?.cancel()
    }

    protected fun sleep(duration: Duration): Boolean {
        try {
            Thread.sleep(duration.toMillis())
        } catch (e: InterruptedException) {
            return false
        }
        return true
    }

    @Throws(Exception::class)
    abstract fun go()

    private fun logTag(): String = name

    @Composable
    open fun Ui() {
        widgets.forEach {
            it.Compose()
        }
    }
}
