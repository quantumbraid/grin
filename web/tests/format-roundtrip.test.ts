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
import { GrinRule } from "../lib/grin-rule";
import { HEADER_SIZE_BYTES, RULES_BLOCK_SIZE } from "../lib/format";

function buildRulesBlock(): Uint8Array {
  const block = new Uint8Array(RULES_BLOCK_SIZE);
  for (let i = 0; i < block.length; i += 1) {
    block[i] = (i * 7) & 0xff;
  }
  return block;
}

test("GrinHeader serialize/deserialize round-trip", () => {
  const rulesBlock = buildRulesBlock();
  const pixelDataLength = BigInt(2 * 3 * 5);
  const fileLength = BigInt(HEADER_SIZE_BYTES) + pixelDataLength;
  const header = new GrinHeader({
    width: 2,
    height: 3,
    tickMicros: 1234,
    ruleCount: 2,
    opcodeSetId: 0,
    flags: 0,
    pixelDataLength,
    fileLength,
    pixelDataOffset: BigInt(HEADER_SIZE_BYTES),
    reservedA: 0n,
    reservedB: 0n,
    rulesBlock,
  });

  const bytes = header.serialize();
  const parsed = GrinHeader.deserialize(bytes);

  assert.deepEqual(parsed.magic, header.magic);
  assert.equal(parsed.versionMajor, header.versionMajor);
  assert.equal(parsed.versionMinor, header.versionMinor);
  assert.equal(parsed.headerSize, header.headerSize);
  assert.equal(parsed.width, header.width);
  assert.equal(parsed.height, header.height);
  assert.equal(parsed.tickMicros, header.tickMicros);
  assert.equal(parsed.ruleCount, header.ruleCount);
  assert.equal(parsed.opcodeSetId, header.opcodeSetId);
  assert.equal(parsed.flags, header.flags);
  assert.equal(parsed.pixelDataLength, header.pixelDataLength);
  assert.equal(parsed.fileLength, header.fileLength);
  assert.equal(parsed.pixelDataOffset, header.pixelDataOffset);
  assert.equal(parsed.reservedA, header.reservedA);
  assert.equal(parsed.reservedB, header.reservedB);
  assert.deepEqual(parsed.rulesBlock, header.rulesBlock);
});

test("GrinPixel serialize/deserialize round-trip", () => {
  const pixel = new GrinPixel(12, 34, 56, 78, 0x8f);
  const bytes = pixel.toBytes();
  const parsed = GrinPixel.fromBytes(bytes);

  assert.equal(parsed.r, pixel.r);
  assert.equal(parsed.g, pixel.g);
  assert.equal(parsed.b, pixel.b);
  assert.equal(parsed.a, pixel.a);
  assert.equal(parsed.c, pixel.c);
});

test("GrinRule serialize/deserialize round-trip", () => {
  const rule = new GrinRule(0x00ff, 0x05, 0xaa);
  const bytes = rule.serialize();
  const parsed = GrinRule.deserialize(bytes);

  assert.equal(parsed.groupMask, rule.groupMask);
  assert.equal(parsed.opcode, rule.opcode);
  assert.equal(parsed.timing, rule.timing);
});
