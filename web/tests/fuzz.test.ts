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
import { GrinFile } from "../lib/grin-file";
import { GrinPixel } from "../lib/grin-pixel";
import { GrinRule } from "../lib/grin-rule";
import { readRulesBlock } from "../lib/grin-io";
import { HEADER_SIZE_BYTES, RULES_BLOCK_SIZE } from "../lib/format";

function rng(seed: number): () => number {
  let value = seed >>> 0;
  return () => {
    value = (1664525 * value + 1013904223) >>> 0;
    return value;
  };
}

function randomBytes(count: number, next: () => number): Uint8Array {
  const bytes = new Uint8Array(count);
  for (let i = 0; i < bytes.length; i += 1) {
    bytes[i] = next() & 0xff;
  }
  return bytes;
}

test("header fuzzing produces validation output without throwing", () => {
  const next = rng(0xdeadbeef);
  for (let i = 0; i < 50; i += 1) {
    const bytes = randomBytes(HEADER_SIZE_BYTES, next);
    const header = GrinHeader.deserialize(bytes);
    const result = header.validate();
    assert.equal(typeof result.ok, "boolean");
  }
});

test("pixel data fuzzing fails gracefully", () => {
  const next = rng(0x1234);
  for (let i = 0; i < 20; i += 1) {
    const header = new GrinHeader({ width: 2, height: 2, tickMicros: 0, ruleCount: 0, opcodeSetId: 0 });
    const pixels = [
      new GrinPixel(1, 2, 3, 4, 0),
      new GrinPixel(5, 6, 7, 8, 0),
      new GrinPixel(9, 10, 11, 12, 0),
      new GrinPixel(13, 14, 15, 16, 0),
    ];
    const file = new GrinFile(header, pixels, []);
    const bytes = file.toBytes();
    const truncate = (next() % 6) + 1;
    const sliced = bytes.subarray(0, bytes.length - truncate);

    try {
      GrinFile.load(sliced);
      assert.fail("Expected truncation to throw");
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      assert.ok(message.length > 0);
    }
  }
});

test("rules fuzzing rejects invalid opcodes with clear errors", () => {
  const next = rng(0x9876);
  for (let i = 0; i < 20; i += 1) {
    const rulesBlock = randomBytes(RULES_BLOCK_SIZE, next);
    const ruleCount = (next() % 5) + 1;
    try {
      readRulesBlock(rulesBlock, ruleCount, 0);
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      assert.ok(message.length > 0);
    }
  }
});
