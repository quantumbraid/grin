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
import { GrinHeader } from "../lib/grin-header";
import { GrinPixel } from "../lib/grin-pixel";
import { GrinFile } from "../lib/grin-file";
import {
  validateControlByte,
  validateGrinFile,
  validateMagic,
  validateOpcode,
  validateVersion,
} from "../lib/validation";
import { MAGIC_BYTES } from "../lib/format";

function createMinimalFile(options: { flags?: number; controlByte?: number } = {}): GrinFile {
  const header = new GrinHeader({
    width: 1,
    height: 1,
    tickMicros: 0,
    ruleCount: 0,
    opcodeSetId: 0,
    flags: options.flags ?? 0,
  });
  const pixel = new GrinPixel(10, 20, 30, 200, options.controlByte ?? 0);
  return new GrinFile(header, [pixel], []);
}

test("validateMagic accepts GRIN and rejects invalid magic", () => {
  assert.equal(validateMagic(MAGIC_BYTES).ok, true);
  assert.equal(validateMagic(new Uint8Array([0, 0, 0, 0])).ok, false);
});

test("validateVersion warns on unexpected version", () => {
  const result = validateVersion(1, 0);
  assert.equal(result.ok, true);
  assert.equal(result.warnings.length, 1);
});

test("validateControlByte flags reserved bits", () => {
  const result = validateControlByte(0x70);
  assert.equal(result.ok, true);
  assert.equal(result.warnings.length, 1);
});

test("validateOpcode rejects unknown base opcode", () => {
  const result = validateOpcode(0xff, 0);
  assert.equal(result.ok, false);
});

test("validateGrinFile strict mode rejects warnings", () => {
  const file = createMinimalFile({ flags: 1 });
  const strictResult = validateGrinFile(file, "strict");
  assert.equal(strictResult.ok, false);

  const permissiveResult = validateGrinFile(file, "permissive");
  assert.equal(permissiveResult.ok, true);
});

test("validateGrinFile passes for minimal valid file", () => {
  const file = createMinimalFile();
  const result = validateGrinFile(file, "permissive");
  assert.equal(result.ok, true);
  assert.equal(result.errors.length, 0);
});
