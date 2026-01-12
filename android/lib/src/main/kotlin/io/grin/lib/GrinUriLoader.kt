/*
 * MIT License
 *
 * Copyright (c) 2025 GRIN Project Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.grin.lib

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileInputStream

object GrinUriLoader {
    fun load(context: Context, uri: Uri): GrinFile {
        return when (uri.scheme) {
            ContentResolver.SCHEME_CONTENT -> {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    GrinFile.load(input)
                } ?: throw IllegalArgumentException("Unable to open content URI: $uri")
            }
            ContentResolver.SCHEME_FILE -> {
                val path = uri.path ?: throw IllegalArgumentException("Missing file path: $uri")
                if (path.startsWith("/android_asset/")) {
                    val assetPath = path.removePrefix("/android_asset/")
                    context.assets.open(assetPath).use { input -> GrinFile.load(input) }
                } else {
                    FileInputStream(File(path)).use { input -> GrinFile.load(input) }
                }
            }
            "android.resource" -> {
                val resId = resolveResourceId(context, uri)
                context.resources.openRawResource(resId).use { input -> GrinFile.load(input) }
            }
            else -> throw IllegalArgumentException("Unsupported URI scheme: ${uri.scheme}")
        }
    }

    fun fromAsset(context: Context, assetPath: String): GrinFile {
        context.assets.open(assetPath).use { input ->
            return GrinFile.load(input)
        }
    }

    fun fromRawResource(context: Context, resId: Int): GrinFile {
        context.resources.openRawResource(resId).use { input ->
            return GrinFile.load(input)
        }
    }

    fun fromFile(file: File): GrinFile {
        FileInputStream(file).use { input ->
            return GrinFile.load(input)
        }
    }

    private fun resolveResourceId(context: Context, uri: Uri): Int {
        val pathSegments = uri.pathSegments
        if (pathSegments.size < 2) {
            throw IllegalArgumentException("Invalid android.resource URI: $uri")
        }
        val resType = pathSegments[0]
        val resName = pathSegments[1]
        val resId = context.resources.getIdentifier(resName, resType, uri.authority)
        if (resId == 0) {
            throw IllegalArgumentException("Resource not found: $uri")
        }
        return resId
    }
}
