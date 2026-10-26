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

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Xml
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionManager
import androidx.appfunctions.AppFunctionSearchSpec
import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionReferenceTypeMetadata
import java.io.InputStream
import kotlinx.coroutines.runBlocking
import org.prbentley.scratchpad.BaseSketch
import org.prbentley.scratchpad.BooleanWidget
import org.prbentley.scratchpad.Sketch
import org.prbentley.scratchpad.Widget
import org.xmlpull.v1.XmlPullParser

/**
 * Lists installed apps which export AppFunctions, and the functions they export.
 *
 * Uses two methods:
 * 1. The Jetpack [AppFunctionManager] discovery API. This only sees other apps' functions if we
 *    hold EXECUTE_APP_FUNCTIONS (or DISCOVER_APP_FUNCTIONS), which is normally privileged.
 * 2. A PackageManager scan for AppFunctionService implementations, reading the function
 *    metadata XML each one declares from the owning app's assets. This needs no permissions.
 */
@Sketch
class AppFunctions : BaseSketch() {
    private val useApi = BooleanWidget("Use AppFunctionManager API", true)
    private val scan = BooleanWidget("Scan PackageManager", true)
    private val showDetails = BooleanWidget("Show parameters", true)

    override val widgets = listOf<Widget>(useApi, scan, showDetails)

    private val pm: PackageManager
        get() = applicationContext.packageManager

    override fun go() {
        if (useApi.checked) {
            searchWithApi()
        }
        if (scan.checked && shouldContinue) {
            scanPackages()
        }
    }

    private fun searchWithApi() {
        out("== AppFunctionManager API")
        val manager = AppFunctionManager.getInstance(applicationContext)
        if (manager == null) {
            out("AppFunctionManager unavailable (needs API 34+, not a work profile)")
            return
        }
        val functions =
            try {
                runBlocking { manager.searchAppFunctions(AppFunctionSearchSpec()) }
            } catch (e: SecurityException) {
                out("Search not permitted: ${e.message}")
                return
            }
        if (functions.isEmpty()) {
            out("No functions visible (EXECUTE_APP_FUNCTIONS is probably not granted)")
        }
        val byPackage = functions.groupBy { it.packageName }
        byPackage.forEach { (packageName, packageFunctions) ->
            out("${label(packageName)} ($packageName): ${packageFunctions.size} function(s)")
            packageFunctions.forEach { printFunction(it) }
        }
        out("API found ${functions.size} function(s) in ${byPackage.size} app(s)")
    }

    private fun printFunction(function: AppFunctionMetadata) {
        val schema = function.schema?.let { " [${it.category}/${it.name} v${it.version}]" } ?: ""
        out("  ${function.id}$schema")
        if (function.description.isNotEmpty()) {
            out("    ${function.description}")
        }
        if (showDetails.checked) {
            function.parameters.forEach {
                val optional = if (it.isRequired) "" else "?"
                out("    param ${it.name}$optional: ${typeName(it.dataType)}")
            }
            out("    returns ${typeName(function.response.valueType)}")
        }
    }

    private fun typeName(type: AppFunctionDataTypeMetadata): String {
        val name =
            when (type) {
                is AppFunctionArrayTypeMetadata -> "List<${typeName(type.itemType)}>"

                is AppFunctionReferenceTypeMetadata -> type.referenceDataType

                is AppFunctionObjectTypeMetadata -> type.qualifiedName ?: "Object"

                else ->
                    type.javaClass.simpleName
                        .removePrefix("AppFunction")
                        .removeSuffix("TypeMetadata")
            }
        return if (type.isNullable) "$name?" else name
    }

    private fun scanPackages() {
        out("== PackageManager scan")
        // AppFunctions need API 34+ in any case.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            out("Scan needs API 33+")
            return
        }
        val services =
            pm.queryIntentServices(
                Intent(SERVICE_ACTION),
                PackageManager.ResolveInfoFlags.of(0)
            )
        var functionCount = 0
        val packages = mutableSetOf<String>()
        for (info in services) {
            if (!shouldContinue) break
            val service = info.serviceInfo
            val component = ComponentName(service.packageName, service.name)
            packages += service.packageName
            out("${label(service.packageName)} (${service.packageName})")
            verbose("  service ${service.name}")

            val xmlName = metadataXmlName(component)
            if (xmlName == null) {
                out("  No AppFunction metadata property declared")
                continue
            }
            try {
                val root =
                    pm.getResourcesForApplication(service.packageName).assets.open(xmlName).use {
                        parseXml(it)
                    }
                val functions = root.children.filter { it.name == "appfunction" }
                out("  $xmlName: ${functions.size} function(s)")
                functions.forEach { printScannedFunction(it) }
                functionCount += functions.size
            } catch (e: Exception) {
                out("  Failed to read $xmlName: $e")
            }
        }
        out("Scan found $functionCount function(s) in ${packages.size} app(s)")
    }

    /**
     * Prints an `<appfunction>` element. Handles both the v2 format generated by the Jetpack
     * compiler (camelCase tags, full parameter and response metadata) and the legacy platform
     * format (`function_id`, `schema_name`, etc. and no parameter metadata).
     */
    private fun printScannedFunction(function: Element) {
        val id = function.value("id", "function_id") ?: "(no id)"
        val schema =
            function.value("schemaName", "schema_name")?.let {
                val category = function.value("schemaCategory", "schema_category")
                val version = function.value("schemaVersion", "schema_version")
                " [$category/$it v$version]"
            } ?: ""
        val disabled =
            if (function.value("enabledByDefault", "enabled_by_default") == "false") {
                " (disabled by default)"
            } else {
                ""
            }
        out("  $id$schema$disabled")
        function.value("description")?.let { out("    $it") }
        if (showDetails.checked) {
            // Each <parameters> element describes a single parameter.
            function.children.filter { it.name == "parameters" }.forEach {
                val optional = if (it.value("isRequired") == "false") "?" else ""
                out(
                    "    param ${it.value(
                        "name"
                    )}$optional: ${typeName(it.child("dataTypeMetadata"))}"
                )
            }
            function.child("response")?.child("valueType")?.let {
                out("    returns ${typeName(it)}")
            }
        }
    }

    private fun typeName(type: Element?): String {
        if (type == null) return "?"
        val code = type.value("type")?.toIntOrNull()
        val name =
            when (code) {
                TYPE_ARRAY -> "List<${typeName(type.child("itemType"))}>"
                TYPE_REFERENCE -> type.value("dataTypeReference") ?: "Reference"
                TYPE_OBJECT -> type.value("qualifiedName") ?: "Object"
                else -> TYPE_NAMES[code] ?: "type $code"
            }
        return if (type.value("isNullable") == "true") "$name?" else name
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun metadataXmlName(component: ComponentName): String? {
        for (property in METADATA_PROPERTIES) {
            try {
                return pm.getProperty(property, component).string
            } catch (_: PackageManager.NameNotFoundException) {
                // Try the next property name.
            }
        }
        return null
    }

    /** Minimal XML element tree, enough to navigate AppFunction metadata files. */
    private class Element(val name: String) {
        val children = mutableListOf<Element>()
        var text = ""

        fun child(vararg names: String): Element? = children.firstOrNull { it.name in names }

        fun value(vararg names: String): String? = child(*names)?.text?.trim()
    }

    private fun parseXml(input: InputStream): Element {
        val parser = Xml.newPullParser()
        parser.setInput(input, null)
        val root = Element("")
        val stack = ArrayDeque(listOf(root))
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    val element = Element(parser.name)
                    stack.last().children += element
                    stack.addLast(element)
                }

                XmlPullParser.TEXT -> stack.last().text += parser.text

                XmlPullParser.END_TAG -> stack.removeLast()
            }
        }
        // Return the document element, e.g. <appfunctions>.
        return root.children.firstOrNull() ?: root
    }

    private fun label(packageName: String): String = try {
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        packageName
    }

    private companion object {
        const val SERVICE_ACTION = "android.app.appfunctions.AppFunctionService"
        val METADATA_PROPERTIES =
            listOf("android.app.appfunctions.v2", "android.app.appfunctions")

        // Data type codes from AppFunctionDataTypeMetadata.
        const val TYPE_OBJECT = 3
        const val TYPE_ARRAY = 10
        const val TYPE_REFERENCE = 11
        val TYPE_NAMES =
            mapOf(
                0 to "Unit",
                1 to "Boolean",
                2 to "Bytes",
                4 to "Double",
                5 to "Float",
                6 to "Long",
                7 to "Int",
                8 to "String",
                12 to "AllOf",
                13 to "Parcelable",
                14 to "OneOf"
            )
    }
}
