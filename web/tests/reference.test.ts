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
import { test } from "vitest";
import assert from "node:assert/strict";
import { GrinFile } from "../lib/grin-file";
import { GrinHeader } from "../lib/grin-header";
import { GrinPixel } from "../lib/grin-pixel";
import { HEADER_SIZE_BYTES } from "../lib/format";

function buildReferenceBytes(): Uint8Array {
  const bytes = new Uint8Array(HEADER_SIZE_BYTES + 5);
  bytes.set([0x47, 0x52, 0x49, 0x4e], 0); // MAGIC
  bytes[4] = 0x00;
  bytes[5] = 0x00;
  bytes[6] = 0x80;
  bytes[7] = 0x00;
  bytes[8] = 0x01;
  bytes[12] = 0x01;
  bytes[24] = 0x05;
  bytes[32] = 0x85;
  bytes[40] = 0x80;
  bytes[HEADER_SIZE_BYTES] = 1;
  bytes[HEADER_SIZE_BYTES + 1] = 2;
  bytes[HEADER_SIZE_BYTES + 2] = 3;
  bytes[HEADER_SIZE_BYTES + 3] = 4;
  bytes[HEADER_SIZE_BYTES + 4] = 0;
  return bytes;
}

test("JavaScript file serialization matches reference bytes", () => {
  const header = new GrinHeader({
    width: 1,
    height: 1,
    tickMicros: 0,
    ruleCount: 0,
    opcodeSetId: 0,
    fileLength: BigInt(HEADER_SIZE_BYTES + 5),
  });
  const pixel = new GrinPixel(1, 2, 3, 4, 0);
  const file = new GrinFile(header, [pixel], []);

  const bytes = file.toBytes();
  const expected = buildReferenceBytes();
  assert.deepEqual(bytes, expected);
});
