import { CONTROL_BYTE_MASKS } from "./format.js";
import { TimingInterpreter } from "./timing.js";

export const BaseOpcodeId = {
  NOP: 0x00,
  FADE_IN: 0x01,
  FADE_OUT: 0x02,
  PULSE: 0x03,
  SHIFT_R: 0x04,
  SHIFT_G: 0x05,
  SHIFT_B: 0x06,
  SHIFT_A: 0x07,
  INVERT: 0x08,
  ROTATE_HUE: 0x09,
  LOCK: 0x0a,
  UNLOCK: 0x0b,
  TOGGLE_LOCK: 0x0c,
};

export function applyOpcode(opcodeId, pixel, tick, timing) {
  switch (opcodeId) {
    case BaseOpcodeId.NOP:
      return;
    case BaseOpcodeId.FADE_IN:
      fadeIn(pixel, tick, timing);
      return;
    case BaseOpcodeId.FADE_OUT:
      fadeOut(pixel, tick, timing);
      return;
    case BaseOpcodeId.PULSE:
      pulse(pixel, tick, timing);
      return;
    case BaseOpcodeId.SHIFT_R:
      pixel.r = shiftChannel(pixel.r, tick, timing);
      return;
    case BaseOpcodeId.SHIFT_G:
      pixel.g = shiftChannel(pixel.g, tick, timing);
      return;
    case BaseOpcodeId.SHIFT_B:
      pixel.b = shiftChannel(pixel.b, tick, timing);
      return;
    case BaseOpcodeId.SHIFT_A:
      pixel.a = shiftChannel(pixel.a, tick, timing);
      return;
    case BaseOpcodeId.INVERT:
      pixel.r = 255 - pixel.r;
      pixel.g = 255 - pixel.g;
      pixel.b = 255 - pixel.b;
      return;
    case BaseOpcodeId.ROTATE_HUE:
      rotateHue(pixel, tick, timing);
      return;
    case BaseOpcodeId.LOCK:
      pixel.c |= CONTROL_BYTE_MASKS.LOCK;
      return;
    case BaseOpcodeId.UNLOCK:
      pixel.c &= ~CONTROL_BYTE_MASKS.LOCK;
      return;
    case BaseOpcodeId.TOGGLE_LOCK:
      pixel.c ^= CONTROL_BYTE_MASKS.LOCK;
      return;
    default:
      throw new Error(`Unknown opcode ${opcodeId}`);
  }
}

function fadeIn(pixel, tick, timing) {
  const level = TimingInterpreter.evaluate(timing, tick);
  pixel.a = clampByte(Math.round(pixel.a * level));
}

function fadeOut(pixel, tick, timing) {
  const level = 1 - TimingInterpreter.evaluate(timing, tick);
  pixel.a = clampByte(Math.round(pixel.a * level));
}

function pulse(pixel, tick, timing) {
  const level = TimingInterpreter.evaluate(timing, tick);
  pixel.a = clampByte(Math.round(pixel.a * level));
}

function shiftChannel(value, tick, timing) {
  const wave = TimingInterpreter.evaluate(timing, tick);
  const delta = Math.round((wave * 2 - 1) * 255);
  return clampByte(value + delta);
}

function rotateHue(pixel, tick, timing) {
  const rotation = TimingInterpreter.evaluate(timing, tick) * 360;
  const [h, s, l] = rgbToHsl(pixel.r, pixel.g, pixel.b);
  const newHue = (h + rotation) % 360;
  const [r, g, b] = hslToRgb(newHue, s, l);
  pixel.r = r;
  pixel.g = g;
  pixel.b = b;
}

function clampByte(value) {
  if (value < 0) return 0;
  if (value > 255) return 255;
  return value;
}

function rgbToHsl(r, g, b) {
  const rNorm = r / 255;
  const gNorm = g / 255;
  const bNorm = b / 255;
  const max = Math.max(rNorm, gNorm, bNorm);
  const min = Math.min(rNorm, gNorm, bNorm);
  const delta = max - min;
  let h = 0;
  const l = (max + min) / 2;
  let s = 0;

  if (delta !== 0) {
    s = delta / (1 - Math.abs(2 * l - 1));
    if (max === rNorm) {
      h = ((gNorm - bNorm) / delta) % 6;
    } else if (max === gNorm) {
      h = (bNorm - rNorm) / delta + 2;
    } else {
      h = (rNorm - gNorm) / delta + 4;
    }
    h *= 60;
    if (h < 0) h += 360;
  }

  return [h, s, l];
}

function hslToRgb(h, s, l) {
  const c = (1 - Math.abs(2 * l - 1)) * s;
  const hPrime = h / 60;
  const x = c * (1 - Math.abs((hPrime % 2) - 1));
  let r1 = 0;
  let g1 = 0;
  let b1 = 0;

  if (hPrime >= 0 && hPrime < 1) {
    r1 = c;
    g1 = x;
  } else if (hPrime < 2) {
    r1 = x;
    g1 = c;
  } else if (hPrime < 3) {
    g1 = c;
    b1 = x;
  } else if (hPrime < 4) {
    g1 = x;
    b1 = c;
  } else if (hPrime < 5) {
    r1 = x;
    b1 = c;
  } else {
    r1 = c;
    b1 = x;
  }

  const m = l - c / 2;
  return [
    clampByte(Math.round((r1 + m) * 255)),
    clampByte(Math.round((g1 + m) * 255)),
    clampByte(Math.round((b1 + m) * 255)),
  ];
}
