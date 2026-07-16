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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Controls(viewModel)
            viewModel.SketchUi()
            Logger(viewModel)
        }
    }
}

@Composable
fun Controls(viewModel: MainViewModel) {
    val sketch by viewModel.currentSketch.collectAsStateWithLifecycle()
    val isRunning by sketch.isRunningFlow.collectAsStateWithLifecycle()
    val verboseLogging by viewModel.verboseLogging.collectAsStateWithLifecycle()
    val timestampsEnabled by viewModel.showTimestamps.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            LabeledCheckbox(
                label = "Verbose",
                modifier = Modifier,
                isChecked = verboseLogging,
                onClick = viewModel::setVerbosity
            )
            LabeledCheckbox(
                label = "Timestamps",
                modifier = Modifier,
                isChecked = timestampsEnabled,
                onClick = viewModel::setShowTimestamps
            )
            Button(
                modifier = Modifier
                    .padding(horizontal = 8.dp),
                onClick = { viewModel.runOrStop() }
            ) {
                Text(
                    text = if (isRunning) "Stop" else "Go",
                    textAlign = TextAlign.Center
                )
            }
        }
        Row {
            SketchSelector(
                sketches = viewModel.sketchNames,
                selectedSketchName = viewModel.defaultSketch,
                onSketchSelected = { viewModel.selectSketch(it) }
            )
        }
    }
}

@Composable
fun Logger(viewModel: MainViewModel) {
    viewModel.logger.composable?.Compose(
        modifier = Modifier
            .fillMaxSize()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SketchSelector(
    sketches: List<String>,
    modifier: Modifier = Modifier,
    selectedSketchName: String? = null,
    onSketchSelected: (String) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(selectedSketchName) }

    LaunchedEffect(sketches, selectedSketchName) {
        if (selectedSketchName == null && sketches.isNotEmpty()) {
            selected = sketches.first()
        }
    }

    Box(
        modifier = modifier
            .padding(vertical = 8.dp)
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            TextField(
                modifier = Modifier
                    .menuAnchor(
                        type = MenuAnchorType.PrimaryNotEditable,
                        enabled = true
                    )
                    .padding(vertical = 2.dp)
                    .fillMaxWidth(),
                readOnly = true,
                value = selected ?: "Select a Sketch",
                onValueChange = { },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.textFieldColors()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                sketches.forEach { sketch ->
                    DropdownMenuItem(
                        text = { Text(sketch) },
                        onClick = {
                            selected = sketch
                            onSketchSelected(sketch)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
                if (sketches.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No sketches available") },
                        onClick = { expanded = false },
                        enabled = false
                    )
                }
            }
        }
    }
}
