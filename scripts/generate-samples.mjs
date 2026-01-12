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
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { BaseOpcodeId } from "../tools/lib/opcodes.js";
import { createGrinFile, serializeGrinFile } from "../tools/lib/grin.js";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const samplesDir = path.join(root, "samples");

fs.mkdirSync(samplesDir, { recursive: true });

const DEMO_TICK_MICROS = 16666;

function makePixel(r, g, b, a, groupId, locked = false) {
  const group = groupId & 0x0f;
  const lockBit = locked ? 0x80 : 0x00;
  return { r, g, b, a, c: group | lockBit };
}

function makePixelGrid(width, height, pixelFactory) {
  const pixels = [];
  for (let y = 0; y < height; y += 1) {
    for (let x = 0; x < width; x += 1) {
      pixels.push(pixelFactory(x, y));
    }
  }
  return pixels;
}

function writeSample(name, file) {
  const bytes = serializeGrinFile(file);
  fs.writeFileSync(path.join(samplesDir, name), bytes);
  return bytes;
}

function createSample({ name, width, height, pixels, rules, tickMicros }) {
  const file = createGrinFile({
    width,
    height,
    pixels,
    rules,
    tickMicros,
    opcodeSetId: 0,
  });
  return writeSample(name, file);
}

function clampByte(value) {
  return Math.max(0, Math.min(255, value));
}

const palette = [
  [255, 0, 0],
  [0, 255, 0],
  [0, 0, 255],
  [255, 255, 0],
  [255, 0, 255],
  [0, 255, 255],
  [255, 136, 0],
  [136, 255, 0],
  [0, 136, 255],
  [136, 0, 255],
  [255, 0, 136],
  [0, 255, 136],
  [136, 136, 136],
  [255, 255, 255],
  [68, 68, 68],
  [0, 0, 0],
];

const minimalPixels = [makePixel(0, 0, 0, 255, 0)];
const minimalBytes = createSample({
  name: "minimal.grin",
  width: 1,
  height: 1,
  pixels: minimalPixels,
  rules: [],
  tickMicros: 0,
});

const minimalLockedPixels = [makePixel(0, 0, 0, 255, 0, true)];
const minimalLockedBytes = createSample({
  name: "minimal_locked.grin",
  width: 1,
  height: 1,
  pixels: minimalLockedPixels,
  rules: [],
  tickMicros: 0,
});

createSample({
  name: "pulse_red.grin",
  width: 16,
  height: 16,
  pixels: makePixelGrid(16, 16, () => makePixel(255, 0, 0, 255, 0)),
  rules: [{ groupMask: 0x0001, opcode: BaseOpcodeId.PULSE, timing: 0x27 }],
  tickMicros: DEMO_TICK_MICROS,
});

createSample({
  name: "fade_gradient.grin",
  width: 64,
  height: 64,
  pixels: makePixelGrid(64, 64, (x) => {
    const value = clampByte(Math.round((x / 63) * 255));
    const groupId = Math.min(3, Math.floor(x / 16));
    return makePixel(value, value, value, 255, groupId);
  }),
  rules: [
    { groupMask: 0x0001, opcode: BaseOpcodeId.FADE_IN, timing: 0x27 },
    { groupMask: 0x0002, opcode: BaseOpcodeId.FADE_OUT, timing: 0x17 },
    { groupMask: 0x0004, opcode: BaseOpcodeId.FADE_IN, timing: 0x33 },
    { groupMask: 0x0008, opcode: BaseOpcodeId.FADE_OUT, timing: 0x07 },
  ],
  tickMicros: DEMO_TICK_MICROS,
});

createSample({
  name: "color_shift.grin",
  width: 32,
  height: 32,
  pixels: makePixelGrid(32, 32, (x) => {
    const stripeWidth = Math.ceil(32 / 3);
    const groupId = Math.min(2, Math.floor(x / stripeWidth));
    return makePixel(128, 128, 128, 255, groupId);
  }),
  rules: [
    { groupMask: 0x0001, opcode: BaseOpcodeId.SHIFT_R, timing: 0x27 },
    { groupMask: 0x0002, opcode: BaseOpcodeId.SHIFT_G, timing: 0x67 },
    { groupMask: 0x0004, opcode: BaseOpcodeId.SHIFT_B, timing: 0xa7 },
  ],
  tickMicros: DEMO_TICK_MICROS,
});

createSample({
  name: "groups_demo.grin",
  width: 16,
  height: 16,
  pixels: makePixelGrid(16, 16, (x) => {
    const [r, g, b] = palette[x];
    return makePixel(r, g, b, 255, x);
  }),
  rules: [],
  tickMicros: DEMO_TICK_MICROS,
});

createSample({
  name: "locking_demo.grin",
  width: 16,
  height: 16,
  pixels: makePixelGrid(16, 16, (x) => {
    const locked = x >= 8;
    const groupId = locked ? 1 : 0;
    return makePixel(24, 160, 200, 255, groupId, locked);
  }),
  rules: [
    { groupMask: 0x0001, opcode: BaseOpcodeId.PULSE, timing: 0x27 },
    { groupMask: 0x0001, opcode: BaseOpcodeId.LOCK, timing: 0x00 },
    { groupMask: 0x0002, opcode: BaseOpcodeId.UNLOCK, timing: 0x00 },
  ],
  tickMicros: DEMO_TICK_MICROS,
});

createSample({
  name: "timing_demo.grin",
  width: 16,
  height: 16,
  pixels: makePixelGrid(16, 16, (x, y) => {
    const groupId = x < 8 ? (y < 8 ? 0 : 2) : y < 8 ? 1 : 3;
    return makePixel(60, 120, 240, 255, groupId);
  }),
  rules: [
    { groupMask: 0x0001, opcode: BaseOpcodeId.PULSE, timing: 0x01 },
    { groupMask: 0x0002, opcode: BaseOpcodeId.PULSE, timing: 0x13 },
    { groupMask: 0x0004, opcode: BaseOpcodeId.PULSE, timing: 0x27 },
    { groupMask: 0x0008, opcode: BaseOpcodeId.PULSE, timing: 0x37 },
  ],
  tickMicros: DEMO_TICK_MICROS,
});

const maxSize = 512;
createSample({
  name: "max_size.grin",
  width: maxSize,
  height: maxSize,
  pixels: makePixelGrid(maxSize, maxSize, (x, y) => {
    const r = clampByte(Math.round((x / (maxSize - 1)) * 255));
    const g = clampByte(Math.round((y / (maxSize - 1)) * 255));
    return makePixel(r, g, 128, 255, 0);
  }),
  rules: [],
  tickMicros: DEMO_TICK_MICROS,
});

const maxRuleOpcodes = [
  BaseOpcodeId.NOP,
  BaseOpcodeId.FADE_IN,
  BaseOpcodeId.FADE_OUT,
  BaseOpcodeId.PULSE,
  BaseOpcodeId.SHIFT_R,
  BaseOpcodeId.SHIFT_G,
  BaseOpcodeId.SHIFT_B,
  BaseOpcodeId.SHIFT_A,
  BaseOpcodeId.INVERT,
  BaseOpcodeId.ROTATE_HUE,
  BaseOpcodeId.LOCK,
  BaseOpcodeId.UNLOCK,
  BaseOpcodeId.TOGGLE_LOCK,
  BaseOpcodeId.NOP,
  BaseOpcodeId.FADE_IN,
  BaseOpcodeId.FADE_OUT,
];

createSample({
  name: "max_rules.grin",
  width: 16,
  height: 16,
  pixels: makePixelGrid(16, 16, (x, y) => {
    const groupId = x & 0x0f;
    const r = clampByte(32 + groupId * 12);
    const g = clampByte(16 + y * 8);
    const b = clampByte(200 - groupId * 6);
    return makePixel(r, g, b, 255, groupId);
  }),
  rules: maxRuleOpcodes.map((opcode, index) => ({
    groupMask: 0xffff,
    opcode,
    timing: index & 0x0f,
  })),
  tickMicros: DEMO_TICK_MICROS,
});

const opcodeIds = [
  BaseOpcodeId.NOP,
  BaseOpcodeId.FADE_IN,
  BaseOpcodeId.FADE_OUT,
  BaseOpcodeId.PULSE,
  BaseOpcodeId.SHIFT_R,
  BaseOpcodeId.SHIFT_G,
  BaseOpcodeId.SHIFT_B,
  BaseOpcodeId.SHIFT_A,
  BaseOpcodeId.INVERT,
  BaseOpcodeId.ROTATE_HUE,
  BaseOpcodeId.LOCK,
  BaseOpcodeId.UNLOCK,
  BaseOpcodeId.TOGGLE_LOCK,
];

createSample({
  name: "all_opcodes.grin",
  width: opcodeIds.length,
  height: 1,
  pixels: opcodeIds.map((_, index) => {
    const r = clampByte(20 + index * 18);
    const g = clampByte(240 - index * 12);
    return makePixel(r, g, 100, 255, index);
  }),
  rules: opcodeIds.map((opcode, index) => ({
    groupMask: 1 << index,
    opcode,
    timing: 0x27,
  })),
  tickMicros: DEMO_TICK_MICROS,
});

const invalidMagic = Uint8Array.from(minimalBytes);
invalidMagic.set([0x4e, 0x4f, 0x50, 0x45], 0);
fs.writeFileSync(path.join(samplesDir, "invalid_magic.grin"), invalidMagic);

const invalidHeaderSize = Uint8Array.from(minimalBytes);
invalidHeaderSize[6] = 0x7f;
invalidHeaderSize[7] = 0x00;
fs.writeFileSync(path.join(samplesDir, "invalid_header_size.grin"), invalidHeaderSize);

fs.writeFileSync(path.join(samplesDir, "truncated.grin"), minimalBytes.subarray(0, 130));

createSample({
  name: "invalid_opcode.grin",
  width: 1,
  height: 1,
  pixels: minimalPixels,
  rules: [{ groupMask: 0x0001, opcode: 0xff, timing: 0x00 }],
  tickMicros: 0,
});

process.stdout.write(`Generated samples in ${samplesDir}\n`);
process.stdout.write(`minimal.grin bytes: ${minimalBytes.length}\n`);
process.stdout.write(`minimal_locked.grin bytes: ${minimalLockedBytes.length}\n`);
