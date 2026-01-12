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
export function writeUint16LE(value: number): Uint8Array {
  const buffer = new ArrayBuffer(2);
  const view = new DataView(buffer);
  view.setUint16(0, value & 0xffff, true);
  return new Uint8Array(buffer);
}

export function writeUint32LE(value: number): Uint8Array {
  const buffer = new ArrayBuffer(4);
  const view = new DataView(buffer);
  view.setUint32(0, value >>> 0, true);
  return new Uint8Array(buffer);
}

export function writeUint64LE(value: bigint): Uint8Array {
  const buffer = new ArrayBuffer(8);
  const view = new DataView(buffer);
  view.setBigUint64(0, value, true);
  return new Uint8Array(buffer);
}

export function readUint16LE(bytes: Uint8Array, offset: number): number {
  ensureRange(bytes, offset, 2);
  const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
  return view.getUint16(offset, true);
}

export function readUint32LE(bytes: Uint8Array, offset: number): number {
  ensureRange(bytes, offset, 4);
  const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
  return view.getUint32(offset, true);
}

export function readUint64LE(bytes: Uint8Array, offset: number): bigint {
  ensureRange(bytes, offset, 8);
  const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
  return view.getBigUint64(offset, true);
}

function ensureRange(bytes: Uint8Array, offset: number, length: number): void {
  if (offset < 0 || offset + length > bytes.length) {
    throw new RangeError("Read exceeds buffer length");
  }
}
