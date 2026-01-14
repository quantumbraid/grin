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

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream
import kotlin.concurrent.thread

class GrinContentProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String = "application/octet-stream"

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        throw UnsupportedOperationException("Insert not supported")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        throw UnsupportedOperationException("Delete not supported")
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        throw UnsupportedOperationException("Update not supported")
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode.contains("w")) {
            throw FileNotFoundException("Write mode not supported")
        }
        val ctx = context ?: throw FileNotFoundException("Context unavailable")
        val path = uri.path?.trimStart('/') ?: throw FileNotFoundException("Missing path")

        return when {
            path.startsWith("asset/") -> {
                val assetPath = path.removePrefix("asset/")
                openPipe(ctx.assets.open(assetPath))
            }
            path.startsWith("raw/") -> {
                val resName = path.removePrefix("raw/")
                val resId = ctx.resources.getIdentifier(resName, "raw", ctx.packageName)
                if (resId == 0) {
                    throw FileNotFoundException("Resource not found: $resName")
                }
                openPipe(ctx.resources.openRawResource(resId))
            }
            else -> {
                val file = File(path)
                if (!file.exists()) {
                    throw FileNotFoundException("File not found: $path")
                }
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            }
        }
    }

    private fun openPipe(input: InputStream): ParcelFileDescriptor {
        val pipe = ParcelFileDescriptor.createPipe()
        val output = ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])
        thread(name = "grin-content-provider") {
            input.use { source ->
                output.use { target ->
                    source.copyTo(target)
                }
            }
        }
        return pipe[0]
    }
}
