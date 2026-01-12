import { CONTROL_BYTE_MASKS, groupMaskTargetsGroup } from "./format.js";
import { TimingInterpreter } from "./timing.js";
import { applyOpcode } from "./opcodes.js";

const ACTIVE_THRESHOLD = 0.5;
const USE_PACKED_OUTPUT = isLittleEndian();

export function renderToTick(file, tick) {
  if (!Number.isInteger(tick) || tick < 0) {
    throw new Error("Tick must be a non-negative integer");
  }
  const state = createRenderState(file);
  let output = new Uint8Array(file.pixels.length * 4);
  for (let t = 0; t <= tick; t += 1) {
    output = renderFrame(file, t, state);
  }
  return output;
}

export function renderFrame(file, tick, state) {
  const output = new Uint8Array(file.pixels.length * 4);
  const output32 =
    USE_PACKED_OUTPUT && output.byteLength % 4 === 0 ? new Uint32Array(output.buffer) : null;
  const activeRules = evaluateActiveRules(file.rules, tick);

  for (let i = 0; i < file.pixels.length; i += 1) {
    const source = file.pixels[i];
    if (!source) {
      continue;
    }
    const control = state.controlBytes[i] ?? source.c;
    const outputIndex = i * 4;

    if ((control & CONTROL_BYTE_MASKS.LOCK) !== 0) {
      if (output32) {
        output32[i] = packRgba(source.r, source.g, source.b, source.a);
      } else {
        output[outputIndex] = source.r;
        output[outputIndex + 1] = source.g;
        output[outputIndex + 2] = source.b;
        output[outputIndex + 3] = source.a;
      }
      continue;
    }

    const working = {
      r: source.r,
      g: source.g,
      b: source.b,
      a: source.a,
      c: control,
    };
    const groupId = control & CONTROL_BYTE_MASKS.GROUP_ID;

    for (const rule of activeRules) {
      if (groupMaskTargetsGroup(rule.groupMask, groupId)) {
        applyOpcode(rule.opcode, working, tick, rule.timing);
      }
    }

    state.controlBytes[i] = working.c & ~CONTROL_BYTE_MASKS.RESERVED;
    if (output32) {
      output32[i] = packRgba(working.r, working.g, working.b, working.a);
    } else {
      output[outputIndex] = working.r;
      output[outputIndex + 1] = working.g;
      output[outputIndex + 2] = working.b;
      output[outputIndex + 3] = working.a;
    }
  }

  return output;
}

export function renderGroupMap(file) {
  const output = new Uint8Array(file.pixels.length * 4);
  const output32 =
    USE_PACKED_OUTPUT && output.byteLength % 4 === 0 ? new Uint32Array(output.buffer) : null;
  for (let i = 0; i < file.pixels.length; i += 1) {
    const source = file.pixels[i];
    if (!source) {
      continue;
    }
    const groupId = source.c & CONTROL_BYTE_MASKS.GROUP_ID;
    const color = GROUP_COLORS[groupId] ?? [0, 0, 0];
    const offset = i * 4;
    if (output32) {
      output32[i] = packRgba(color[0], color[1], color[2], 255);
    } else {
      output[offset] = color[0];
      output[offset + 1] = color[1];
      output[offset + 2] = color[2];
      output[offset + 3] = 255;
    }
  }
  return output;
}

export function createRenderState(file) {
  const controlBytes = new Uint8Array(file.pixels.length);
  file.pixels.forEach((pixel, index) => {
    controlBytes[index] = pixel.c & ~CONTROL_BYTE_MASKS.RESERVED;
  });
  return { controlBytes };
}

function evaluateActiveRules(rules, tick) {
  return rules.filter((rule) => TimingInterpreter.evaluate(rule.timing, tick) > ACTIVE_THRESHOLD);
}

function isLittleEndian() {
  const buffer = new ArrayBuffer(4);
  new DataView(buffer).setUint32(0, 0x0a0b0c0d, true);
  return new Uint8Array(buffer)[0] === 0x0d;
}

function packRgba(r, g, b, a) {
  return (r | (g << 8) | (b << 16) | (a << 24)) >>> 0;
}

const GROUP_COLORS = [
  [230, 57, 70],
  [29, 53, 87],
  [69, 123, 157],
  [168, 218, 220],
  [241, 250, 238],
  [255, 183, 3],
  [251, 133, 0],
  [33, 158, 188],
  [142, 202, 230],
  [0, 109, 119],
  [131, 56, 236],
  [255, 0, 110],
  [76, 201, 240],
  [6, 214, 160],
  [239, 71, 111],
  [17, 138, 178],
];
