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
import { TimingInterpreter } from "../lib/timing";

const TIMING_SQUARE_PERIOD_2 = 0x01;
const TIMING_TRIANGLE_PERIOD_2 = 0x11;
const TIMING_SINE_PERIOD_2 = 0x21;
const TIMING_SAW_PERIOD_2 = 0x31;
const TIMING_SQUARE_PHASE_2 = 0x41;
const TIMING_ONE_SHOT = 0x00;

test("TimingInterpreter exposes waveform metadata", () => {
  assert.equal(TimingInterpreter.getPeriod(TIMING_SQUARE_PERIOD_2), 2);
  assert.equal(TimingInterpreter.getWaveform(TIMING_SQUARE_PERIOD_2), "square");
  assert.equal(TimingInterpreter.getWaveform(TIMING_TRIANGLE_PERIOD_2), "triangle");
  assert.equal(TimingInterpreter.getWaveform(TIMING_SINE_PERIOD_2), "sine");
  assert.equal(TimingInterpreter.getWaveform(TIMING_SAW_PERIOD_2), "sawtooth");
  assert.equal(TimingInterpreter.getPhaseOffset(TIMING_SQUARE_PHASE_2), 1);
});

test("TimingInterpreter evaluates square waves on period boundaries", () => {
  assert.equal(TimingInterpreter.evaluate(TIMING_SQUARE_PERIOD_2, 0), 0);
  assert.equal(TimingInterpreter.evaluate(TIMING_SQUARE_PERIOD_2, 1), 1);
  assert.equal(TimingInterpreter.evaluate(TIMING_SQUARE_PERIOD_2, 2), 0);
});

test("TimingInterpreter evaluates triangle waves symmetrically", () => {
  const value = TimingInterpreter.evaluate(TIMING_TRIANGLE_PERIOD_2, 1);
  assert.equal(value, 1);
});

test("TimingInterpreter treats timing 0 as a one-shot activation", () => {
  // Timing zero should pulse once at the start, then remain inactive.
  assert.equal(TimingInterpreter.evaluate(TIMING_ONE_SHOT, 0), 1);
  assert.equal(TimingInterpreter.evaluate(TIMING_ONE_SHOT, 1), 0);
  assert.equal(TimingInterpreter.evaluate(TIMING_ONE_SHOT, 2), 0);
});
