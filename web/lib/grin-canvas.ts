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
import { DisplayBuffer } from "./display-buffer";

export class GrinCanvasRenderer {
  private imageData: ImageData | null = null;

  render(displayBuffer: DisplayBuffer, canvas: HTMLCanvasElement | OffscreenCanvas): void {
    const width = displayBuffer.width;
    const height = displayBuffer.height;
    const context = canvas.getContext("2d");
    if (!context) {
      throw new Error("Unable to obtain 2D context");
    }
    if (!this.imageData || this.imageData.width !== width || this.imageData.height !== height) {
      this.imageData = new ImageData(width, height);
    }
    this.imageData.data.set(displayBuffer.rgbaData);
    context.putImageData(this.imageData, 0, 0);
  }
}
