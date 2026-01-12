import { test } from "vitest";
import assert from "node:assert/strict";
import { TimingInterpreter } from "../lib/timing";

const TIMING_SQUARE_PERIOD_2 = 0x01;
const TIMING_TRIANGLE_PERIOD_2 = 0x11;
const TIMING_SINE_PERIOD_2 = 0x21;
const TIMING_SAW_PERIOD_2 = 0x31;
const TIMING_SQUARE_PHASE_2 = 0x41;

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
