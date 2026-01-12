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
import {
  CONTROL_BYTE_MASKS,
  getGroupId,
  isLocked,
  setGroupId,
  setLocked,
} from "./format";

export class GrinPixel {
  r: number;
  g: number;
  b: number;
  a: number;
  c: number;

  constructor(r: number, g: number, b: number, a: number, c: number) {
    this.r = r & 0xff;
    this.g = g & 0xff;
    this.b = b & 0xff;
    this.a = a & 0xff;
    this.c = c & 0xff;
  }

  static fromBytes(bytes: Uint8Array): GrinPixel {
    if (bytes.length !== 5) {
      throw new Error("GrinPixel requires exactly 5 bytes");
    }
    return new GrinPixel(bytes[0] ?? 0, bytes[1] ?? 0, bytes[2] ?? 0, bytes[3] ?? 0, bytes[4] ?? 0);
  }

  getGroupId(): number {
    return getGroupId(this.c);
  }

  isLocked(): boolean {
    return isLocked(this.c);
  }

  setGroupId(groupId: number): void {
    this.c = setGroupId(this.c, groupId) & 0xff;
  }

  setLocked(locked: boolean): void {
    this.c = setLocked(this.c, locked) & 0xff;
  }

  toBytes(): Uint8Array {
    return new Uint8Array([this.r, this.g, this.b, this.a, this.c]);
  }

  reservedBits(): number {
    return this.c & CONTROL_BYTE_MASKS.RESERVED;
  }
}
