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
import { GrinRule } from "../lib/grin-rule";
import { GrinPlayer } from "../lib/grin-player";
import { TestTickScheduler } from "../lib/tick-scheduler";
import { BaseOpcodeId } from "../lib/opcodes";
import { validateGrinFile } from "../lib/validation";
import { parseGrinBytes } from "../../tools/lib/grin.js";

const TIMING_SQUARE_PERIOD_2 = 0x01;

function createSampleFile(): GrinFile {
  const header = new GrinHeader({ width: 1, height: 1, tickMicros: 1, ruleCount: 1, opcodeSetId: 0 });
  const pixel = new GrinPixel(10, 20, 30, 255, 0);
  const rule = new GrinRule(0x0001, BaseOpcodeId.INVERT, TIMING_SQUARE_PERIOD_2);
  return new GrinFile(header, [pixel], [rule]);
}

test("full load -> validate -> play -> render cycle", () => {
  const scheduler = new TestTickScheduler();
  const player = new GrinPlayer(() => scheduler);
  const file = createSampleFile();

  const validation = validateGrinFile(file, "permissive");
  assert.equal(validation.ok, true);

  const bytes = file.toBytes();
  const loaded = GrinFile.load(bytes);
  player.load(loaded);
  player.play();
  scheduler.advance(1);

  const output = player.getCurrentFrame().rgbaData;
  assert.deepEqual(Array.from(output.slice(0, 4)), [245, 235, 225, 255]);
});

test("cross-platform compatibility with CLI parser", () => {
  const file = createSampleFile();
  const bytes = file.toBytes();
  const parsed = parseGrinBytes(bytes);
  assert.equal(parsed.header.width, 1);
  assert.equal(parsed.header.height, 1);
  assert.equal(parsed.rules.length, 1);
  assert.equal(parsed.pixels.length, 1);
});
