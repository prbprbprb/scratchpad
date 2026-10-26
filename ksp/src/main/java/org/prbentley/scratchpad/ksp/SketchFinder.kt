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

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSName
import java.io.OutputStreamWriter

private const val PACKAGE = "org.prbentley.scratchpad"
private const val CLASS = "SketchProvider"
private const val SKETCH = "$PACKAGE.Sketch"
private const val DEFAULT = "$PACKAGE.Default"
private val TEMPLATE = """
    package PACKAGE;
    
    object CLASS
    {
    val sketches  = listOf(
        SKETCHES::class.java,
    )

    val defaultSketchName: String? = DEFAULT
    }
""".trimIndent()

class SketchFinder(
    private val options: Map<String, String>,
    private val logger: KSPLogger,
    private val codeGenerator: CodeGenerator
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val sketches = resolver.findClassesWithAnnotation(SKETCH)
        val defaults = resolver.findClassesWithAnnotation(DEFAULT)

        if (sketches.isEmpty()) {
            return emptyList()
        }

        if (defaults.size > 1) {
            error("There can be only one @Default.")
        }

        val defaultSketch = defaults.firstOrNull()
        val defaultSketchString: String = if (defaultSketch != null) {
            "\"${defaultSketch.substring(defaultSketch.lastIndexOf('.') + 1)}\""
        } else {
            "null"
        }

        val stream = codeGenerator.createNewFile(
            // Aggregating, because the registry lists every @Sketch class. As an isolating
            // output, an incremental build that adds a new source file would regenerate it
            // from that file alone, dropping every other sketch and the @Default.
            dependencies = Dependencies(true, *resolver.getAllFiles().toList().toTypedArray()),
            packageName = PACKAGE,
            fileName = CLASS,
            extensionName = "kt"
        )
        val writer = OutputStreamWriter(stream)

        val mapper = ValueMapper()
        mapper.add("PACKAGE", PACKAGE)
        mapper.add("CLASS", CLASS)
        mapper.add("DEFAULT", defaultSketchString)
        mapper.add("SKETCHES", sketches)
        writer.write(mapper.interpolate(TEMPLATE))
        writer.close()

        return emptyList()
    }
}

private fun Resolver.findClassesWithAnnotation(annotation: String) =
    getSymbolsWithAnnotation(annotation)
        .filterIsInstance<KSClassDeclaration>()
        .map(KSClassDeclaration::qualifiedName)
        .filterNotNull()
        .map(KSName::asString)
        .sorted()
        .toList()
