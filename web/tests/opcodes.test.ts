import { test } from "vitest";
import assert from "node:assert/strict";
import { GrinPixel } from "../lib/grin-pixel";
import {
  FadeInOpcode,
  FadeOutOpcode,
  InvertOpcode,
  LockOpcode,
  NopOpcode,
  PulseOpcode,
  RotateHueOpcode,
  ShiftAOpcode,
  ShiftBOpcode,
  ShiftGOpcode,
  ShiftROpcode,
  ToggleLockOpcode,
  UnlockOpcode,
} from "../lib/opcodes";
import { OpcodeRegistry } from "../lib/opcode-registry";

const TIMING_SQUARE_PERIOD_2 = 0x01;
const TIMING_SAW_PERIOD_2 = 0x31;
const BASE_OPCODES = [
  new NopOpcode(),
  new FadeInOpcode(),
  new FadeOutOpcode(),
  new PulseOpcode(),
  new ShiftROpcode(),
  new ShiftGOpcode(),
  new ShiftBOpcode(),
  new ShiftAOpcode(),
  new InvertOpcode(),
  new RotateHueOpcode(),
  new LockOpcode(),
  new UnlockOpcode(),
  new ToggleLockOpcode(),
];

test("NopOpcode leaves pixel unchanged", () => {
  const pixel = new GrinPixel(10, 20, 30, 40, 0);
  new NopOpcode().apply(pixel, 1, TIMING_SQUARE_PERIOD_2);
  assert.deepEqual(pixel.toBytes(), new Uint8Array([10, 20, 30, 40, 0]));
});

test("FadeInOpcode can zero alpha on low wave", () => {
  const pixel = new GrinPixel(10, 20, 30, 200, 0);
  new FadeInOpcode().apply(pixel, 0, TIMING_SQUARE_PERIOD_2);
  assert.equal(pixel.a, 0);
});

test("FadeOutOpcode can zero alpha on high wave", () => {
  const pixel = new GrinPixel(10, 20, 30, 200, 0);
  new FadeOutOpcode().apply(pixel, 1, TIMING_SQUARE_PERIOD_2);
  assert.equal(pixel.a, 0);
});

test("PulseOpcode scales alpha with waveform", () => {
  const pixel = new GrinPixel(10, 20, 30, 200, 0);
  new PulseOpcode().apply(pixel, 1, TIMING_SQUARE_PERIOD_2);
  assert.equal(pixel.a, 200);
});

test("Shift opcodes shift channels according to waveform", () => {
  const rPixel = new GrinPixel(10, 20, 30, 40, 0);
  const gPixel = new GrinPixel(10, 20, 30, 40, 0);
  const bPixel = new GrinPixel(10, 20, 30, 40, 0);
  const aPixel = new GrinPixel(10, 20, 30, 40, 0);

  new ShiftROpcode().apply(rPixel, 1, TIMING_SQUARE_PERIOD_2);
  new ShiftGOpcode().apply(gPixel, 0, TIMING_SQUARE_PERIOD_2);
  new ShiftBOpcode().apply(bPixel, 1, TIMING_SQUARE_PERIOD_2);
  new ShiftAOpcode().apply(aPixel, 0, TIMING_SQUARE_PERIOD_2);

  assert.equal(rPixel.r, 255);
  assert.equal(gPixel.g, 0);
  assert.equal(bPixel.b, 255);
  assert.equal(aPixel.a, 0);
});

test("InvertOpcode flips RGB channels", () => {
  const pixel = new GrinPixel(10, 20, 30, 200, 0);
  new InvertOpcode().apply(pixel, 0, 0);
  assert.deepEqual(pixel.toBytes(), new Uint8Array([245, 235, 225, 200, 0]));
});

test("RotateHueOpcode rotates hue by waveform", () => {
  const pixel = new GrinPixel(255, 0, 0, 255, 0);
  new RotateHueOpcode().apply(pixel, 1, TIMING_SAW_PERIOD_2);
  assert.deepEqual(pixel.toBytes(), new Uint8Array([0, 255, 255, 255, 0]));
});

test("Lock opcodes mutate lock bit", () => {
  const pixel = new GrinPixel(10, 20, 30, 40, 0);
  new LockOpcode().apply(pixel, 0, 0);
  assert.equal(pixel.c & 0x80, 0x80);

  new UnlockOpcode().apply(pixel, 0, 0);
  assert.equal(pixel.c & 0x80, 0);

  new ToggleLockOpcode().apply(pixel, 0, 0);
  assert.equal(pixel.c & 0x80, 0x80);
});

test("OpcodeRegistry returns base opcodes", () => {
  const registry = OpcodeRegistry.getInstance();
  assert.equal(registry.isValidOpcode(0, 0x00), true);
  assert.equal(registry.getOpcode(0, 0x00).getName(), "NOP");
});

test("Base opcodes report statelessness", () => {
  BASE_OPCODES.forEach((opcode) => {
    assert.equal(opcode.requiresState(), false);
  });
});

test("Base opcode CPU cost stays within declared bounds", () => {
  BASE_OPCODES.forEach((opcode) => {
    assert.equal(opcode.getMaxCpuCost() <= 3, true);
  });
});

test("Base opcodes are deterministic per call", () => {
  const tick = 3;
  const timing = TIMING_SAW_PERIOD_2;
  BASE_OPCODES.forEach((opcode) => {
    const pixelA = new GrinPixel(10, 20, 30, 200, 0);
    const pixelB = new GrinPixel(10, 20, 30, 200, 0);
    opcode.apply(pixelA, tick, timing);
    opcode.apply(pixelB, tick, timing);
    assert.deepEqual(pixelA.toBytes(), pixelB.toBytes());
  });
});
