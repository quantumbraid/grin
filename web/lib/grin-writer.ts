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
import { HEADER_SIZE_BYTES, PIXEL_SIZE_BYTES } from "./format";
import { GrinFile } from "./grin-file";
import { GrinHeader } from "./grin-header";
import { writePixelData, writeRulesBlock } from "./grin-io";

export class GrinWriter {
  static writeHeader(header: GrinHeader): Uint8Array {
    return header.serialize();
  }

  static writeFile(file: GrinFile): Uint8Array {
    const rulesBlock = writeRulesBlock(file.rules, file.rules.length);
    const pixelBytes = writePixelData(file.pixels);
    const pixelDataLength = BigInt(pixelBytes.length);

    const header = file.header.clone({
      ruleCount: file.rules.length,
      rulesBlock,
      pixelDataLength,
      pixelDataOffset: BigInt(HEADER_SIZE_BYTES),
      fileLength: BigInt(HEADER_SIZE_BYTES + pixelBytes.length),
    });

    const output = new Uint8Array(HEADER_SIZE_BYTES + pixelBytes.length);
    output.set(header.serialize(), 0);
    output.set(pixelBytes, HEADER_SIZE_BYTES);
    return output;
  }

  static writePixels(pixels: number[]): Uint8Array {
    if (pixels.length % PIXEL_SIZE_BYTES !== 0) {
      throw new Error("Pixel array length must be a multiple of 5");
    }
    return new Uint8Array(pixels);
  }
}
