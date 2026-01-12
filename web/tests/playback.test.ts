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
import { CONTROL_BYTE_MASKS } from "../lib/format";
import { GrinFile } from "../lib/grin-file";
import { GrinHeader } from "../lib/grin-header";
import { GrinPixel } from "../lib/grin-pixel";
import { GrinRule } from "../lib/grin-rule";
import { GrinPlayer } from "../lib/grin-player";
import { TestTickScheduler } from "../lib/tick-scheduler";
import { BaseOpcodeId } from "../lib/opcodes";

const TIMING_SQUARE_PERIOD_2 = 0x01;

function buildFile(pixels: GrinPixel[], rules: GrinRule[], width: number, height: number): GrinFile {
  const header = new GrinHeader({
    width,
    height,
    tickMicros: 1,
    ruleCount: rules.length,
    opcodeSetId: 0,
  });
  return new GrinFile(header, pixels, rules);
}

test("tick advancement follows scheduler ticks", () => {
  const scheduler = new TestTickScheduler();
  const player = new GrinPlayer(() => scheduler);
  const file = buildFile([new GrinPixel(1, 2, 3, 4, 0)], [], 1, 1);

  player.load(file);
  player.play();
  scheduler.advance(3);

  assert.equal(player.getCurrentTick(), 3);
});

test("rule activation follows timing evaluation", () => {
  const scheduler = new TestTickScheduler();
  const player = new GrinPlayer(() => scheduler);
  const pixel = new GrinPixel(10, 20, 30, 255, 0);
  const rule = new GrinRule(0x0001, BaseOpcodeId.INVERT, TIMING_SQUARE_PERIOD_2);
  const file = buildFile([pixel], [rule], 1, 1);

  player.load(file);
  player.play();
  scheduler.advance(1);

  const output = player.getCurrentFrame().rgbaData;
  assert.deepEqual(Array.from(output.slice(0, 4)), [245, 235, 225, 255]);
});

test("locked pixels ignore active rules", () => {
  const scheduler = new TestTickScheduler();
  const player = new GrinPlayer(() => scheduler);
  const locked = CONTROL_BYTE_MASKS.LOCK;
  const pixel = new GrinPixel(10, 20, 30, 255, locked);
  const rule = new GrinRule(0x0001, BaseOpcodeId.INVERT, TIMING_SQUARE_PERIOD_2);
  const file = buildFile([pixel], [rule], 1, 1);

  player.load(file);
  player.play();
  scheduler.advance(1);

  const output = player.getCurrentFrame().rgbaData;
  assert.deepEqual(Array.from(output.slice(0, 4)), [10, 20, 30, 255]);
});

test("group targeting applies only to matching pixels", () => {
  const scheduler = new TestTickScheduler();
  const player = new GrinPlayer(() => scheduler);
  const pixelA = new GrinPixel(10, 20, 30, 255, 0); // group 0
  const pixelB = new GrinPixel(40, 50, 60, 255, 1); // group 1
  const rule = new GrinRule(0x0001, BaseOpcodeId.INVERT, TIMING_SQUARE_PERIOD_2);
  const file = buildFile([pixelA, pixelB], [rule], 2, 1);

  player.load(file);
  player.play();
  scheduler.advance(1);

  const output = player.getCurrentFrame().rgbaData;
  assert.deepEqual(Array.from(output.slice(0, 4)), [245, 235, 225, 255]);
  assert.deepEqual(Array.from(output.slice(4, 8)), [40, 50, 60, 255]);
});

test("display buffer updates do not mutate source pixels", () => {
  const scheduler = new TestTickScheduler();
  const player = new GrinPlayer(() => scheduler);
  const pixel = new GrinPixel(10, 20, 30, 255, 0);
  const file = buildFile([pixel], [], 1, 1);

  player.load(file);
  player.play();
  scheduler.advance(1);

  const output = player.getCurrentFrame().rgbaData;
  output[0] = 0;
  output[1] = 0;
  output[2] = 0;

  assert.equal(file.pixels[0]?.r, 10);
  assert.equal(file.pixels[0]?.g, 20);
  assert.equal(file.pixels[0]?.b, 30);
});
