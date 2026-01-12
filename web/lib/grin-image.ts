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
import { GrinHeader } from "./grin-header";
import { GrinPixel } from "./grin-pixel";
import { GrinRule } from "./grin-rule";

export class GrinImage {
  header: GrinHeader;
  pixels: GrinPixel[];
  rules: GrinRule[];

  constructor(header: GrinHeader, pixels: GrinPixel[], rules: GrinRule[]) {
    this.header = header;
    this.pixels = pixels;
    this.rules = rules;

    const expectedPixelCount = this.header.width * this.header.height;
    if (!Number.isSafeInteger(expectedPixelCount)) {
      throw new Error("Pixel count exceeds safe integer range");
    }
    if (pixels.length !== expectedPixelCount) {
      throw new Error("Pixel array length does not match header dimensions");
    }
    if (rules.length !== this.header.ruleCount) {
      throw new Error("Rule array length does not match header ruleCount");
    }
  }

  getPixel(x: number, y: number): GrinPixel {
    const index = this.indexFor(x, y);
    const pixel = this.pixels[index];
    if (!pixel) {
      throw new Error("Pixel index out of range");
    }
    return pixel;
  }

  setPixel(x: number, y: number, pixel: GrinPixel): void {
    const index = this.indexFor(x, y);
    this.pixels[index] = pixel;
  }

  getPixelsByGroup(groupId: number): GrinPixel[] {
    return this.pixels.filter((pixel) => pixel.getGroupId() === (groupId & 0x0f));
  }

  getLockedPixels(): GrinPixel[] {
    return this.pixels.filter((pixel) => pixel.isLocked());
  }

  getUnlockedPixels(): GrinPixel[] {
    return this.pixels.filter((pixel) => !pixel.isLocked());
  }

  private indexFor(x: number, y: number): number {
    if (!Number.isInteger(x) || !Number.isInteger(y)) {
      throw new RangeError("Pixel coordinates must be integers");
    }
    if (x < 0 || y < 0 || x >= this.header.width || y >= this.header.height) {
      throw new RangeError("Pixel coordinates out of bounds");
    }
    return y * this.header.width + x;
  }
}
