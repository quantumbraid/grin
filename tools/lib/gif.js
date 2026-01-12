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
import gifEncoderModule from "gif-encoder-2";

const GIFEncoder = gifEncoderModule?.default ?? gifEncoderModule;

export function writeGif(filePath, width, height, frames, options = {}) {
  const expectedLength = width * height * 4;
  const delayMs = Number.isInteger(options.delayMs) ? options.delayMs : 100;
  const repeat = Number.isInteger(options.repeat) ? options.repeat : 0;
  const quality = Number.isInteger(options.quality) ? options.quality : 10;

  for (const frame of frames) {
    if (frame.length !== expectedLength) {
      throw new Error("RGBA buffer length does not match width * height * 4");
    }
  }

  const encoder = new GIFEncoder(width, height);
  encoder.setRepeat(repeat);
  encoder.setDelay(delayMs);
  encoder.setQuality(quality);
  encoder.start();

  for (const frame of frames) {
    encoder.addFrame(frame);
  }

  encoder.finish();
  const buffer = encoder.out.getData();
  fs.writeFileSync(filePath, buffer);
}
