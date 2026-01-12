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
import {
  validateDimensions,
  validateHeaderSize,
  validateMagic,
  validatePixelDataLength,
  validateReservedFields,
  validateRuleCount,
} from "../lib/validation";
import { HEADER_SIZE_BYTES, MAGIC_BYTES, MAX_RULE_COUNT, PIXEL_SIZE_BYTES } from "../lib/format";


test("validateHeaderSize accepts 128 and rejects other sizes", () => {
  assert.equal(validateHeaderSize(HEADER_SIZE_BYTES).ok, true);
  assert.equal(validateHeaderSize(127).ok, false);
});

test("validateMagic accepts correct magic and rejects mismatch", () => {
  assert.equal(validateMagic(MAGIC_BYTES).ok, true);
  assert.equal(validateMagic(new Uint8Array([0, 1, 2, 3])).ok, false);
});

test("validateDimensions handles boundary values", () => {
  assert.equal(validateDimensions(0, 0).ok, true);
  assert.equal(validateDimensions(1, 1).ok, true);
  assert.equal(validateDimensions(0xffffffff, 0xffffffff).ok, true);
  assert.equal(validateDimensions(0x1_0000_0000, 1).ok, false);
  assert.equal(validateDimensions(-1, 1).ok, false);
});

test("validateRuleCount enforces 0-16", () => {
  assert.equal(validateRuleCount(0).ok, true);
  assert.equal(validateRuleCount(MAX_RULE_COUNT).ok, true);
  assert.equal(validateRuleCount(MAX_RULE_COUNT + 1).ok, false);
});

test("validatePixelDataLength checks exact pixel byte length", () => {
  const expected = BigInt(PIXEL_SIZE_BYTES);
  assert.equal(validatePixelDataLength(expected, 1, 1).ok, true);
  assert.equal(validatePixelDataLength(expected - 1n, 1, 1).ok, false);
  assert.equal(validatePixelDataLength(expected + 1n, 1, 1).ok, false);
});

test("validateReservedFields warns when reserved fields are non-zero", () => {
  const clean = validateReservedFields(0n, 0n, 0);
  assert.equal(clean.ok, true);
  assert.equal(clean.warnings.length, 0);

  const warned = validateReservedFields(1n, 0n, 0);
  assert.equal(warned.ok, true);
  assert.equal(warned.warnings.length, 1);
});
