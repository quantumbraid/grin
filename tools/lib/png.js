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
import fs from "node:fs";
import path from "node:path";
import { PNG } from "pngjs";
import jpeg from "jpeg-js";

export function readImage(filePath) {
  const buffer = fs.readFileSync(filePath);
  const ext = path.extname(filePath).toLowerCase();
  if (ext === ".png") {
    const png = PNG.sync.read(buffer);
    return { width: png.width, height: png.height, data: new Uint8Array(png.data) };
  }
  if (ext === ".jpg" || ext === ".jpeg") {
    const decoded = jpeg.decode(buffer, { useTArray: true });
    return { width: decoded.width, height: decoded.height, data: decoded.data };
  }
  throw new Error("Unsupported image format (expected .png or .jpg)");
}

export function writeImage(filePath, width, height, data, options = {}) {
  const ext = path.extname(filePath).toLowerCase();
  const expectedLength = width * height * 4;
  if (data.length !== expectedLength) {
    throw new Error("RGBA buffer length does not match width * height * 4");
  }

  if (ext === ".png") {
    const png = new PNG({ width, height });
    png.data = Buffer.from(data);
    const buffer = PNG.sync.write(png);
    fs.writeFileSync(filePath, buffer);
    return;
  }

  if (ext === ".jpg" || ext === ".jpeg") {
    const quality = Number.isInteger(options.quality) ? options.quality : 90;
    const encoded = jpeg.encode({ data: Buffer.from(data), width, height }, quality);
    fs.writeFileSync(filePath, encoded.data);
    return;
  }

  throw new Error("Unsupported output format (expected .png or .jpg)");
}
