import { CONTROL_BYTE_MASKS } from "./format";
import { GrinPixel } from "./grin-pixel";
import { TimingInterpreter } from "./timing";

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
} as const;

export interface Opcode {
  getId(): number;
  getName(): string;
  apply(pixel: GrinPixel, tick: number, timing: number): void;
  getMaxCpuCost(): number;
  requiresState(): boolean;
}

export class NopOpcode implements Opcode {
  getId(): number {
    return BaseOpcodeId.NOP;
  }
  getName(): string {
    return "NOP";
  }
  apply(_pixel: GrinPixel, _tick: number, _timing: number): void {}
  getMaxCpuCost(): number {
    return 1;
  }
  requiresState(): boolean {
    return false;
  }
}

export class FadeInOpcode implements Opcode {
  getId(): number {
    return BaseOpcodeId.FADE_IN;
  }
  getName(): string {
    return "FADE_IN";
  }
  apply(pixel: GrinPixel, tick: number, timing: number): void {
    const level = TimingInterpreter.evaluate(timing, tick);
    pixel.a = clampByte(Math.round(pixel.a * level));
  }
  getMaxCpuCost(): number {
    return 1;
  }
  requiresState(): boolean {
    return false;
  }
}

export class FadeOutOpcode implements Opcode {
  getId(): number {
    return BaseOpcodeId.FADE_OUT;
  }
  getName(): string {
    return "FADE_OUT";
  }
  apply(pixel: GrinPixel, tick: number, timing: number): void {
    const level = 1 - TimingInterpreter.evaluate(timing, tick);
    pixel.a = clampByte(Math.round(pixel.a * level));
  }
  getMaxCpuCost(): number {
    return 1;
  }
  requiresState(): boolean {
    return false;
  }
}

export class PulseOpcode implements Opcode {
  getId(): number {
    return BaseOpcodeId.PULSE;
  }
  getName(): string {
    return "PULSE";
  }
  apply(pixel: GrinPixel, tick: number, timing: number): void {
    const level = TimingInterpreter.evaluate(timing, tick);
    pixel.a = clampByte(Math.round(pixel.a * level));
  }
  getMaxCpuCost(): number {
    return 1;
  }
  requiresState(): boolean {
    return false;
  }
}

export class ShiftROpcode implements Opcode {
  getId(): number {
    return BaseOpcodeId.SHIFT_R;
  }
  getName(): string {
    return "SHIFT_R";
  }
  apply(pixel: GrinPixel, tick: number, timing: number): void {
    pixel.r = shiftChannel(pixel.r, tick, timing);
  }
  getMaxCpuCost(): number {
    return 1;
  }
  requiresState(): boolean {
    return false;
  }
}

export class ShiftGOpcode implements Opcode {
  getId(): number {
    return BaseOpcodeId.SHIFT_G;
  }
  getName(): string {
    return "SHIFT_G";
  }
  apply(pixel: GrinPixel, tick: number, timing: number): void {
    pixel.g = shiftChannel(pixel.g, tick, timing);
  }
  getMaxCpuCost(): number {
    return 1;
  }
  requiresState(): boolean {
    return false;
  }
}

export class ShiftBOpcode implements Opcode {
  getId(): number {
    return BaseOpcodeId.SHIFT_B;
  }
  getName(): string {
    return "SHIFT_B";
  }
  apply(pixel: GrinPixel, tick: number, timing: number): void {
    pixel.b = shiftChannel(pixel.b, tick, timing);
  }
  getMaxCpuCost(): number {
    return 1;
  }
  requiresState(): boolean {
    return false;
  }
}

export class ShiftAOpcode implements Opcode {
  getId(): number {
    return BaseOpcodeId.SHIFT_A;
  }
  getName(): string {
    return "SHIFT_A";
  }
  apply(pixel: GrinPixel, tick: number, timing: number): void {
    pixel.a = shiftChannel(pixel.a, tick, timing);
  }
  getMaxCpuCost(): number {
    return 1;
  }
  requiresState(): boolean {
    return false;
  }
}

export class InvertOpcode implements Opcode {
  getId(): number {
    return BaseOpcodeId.INVERT;
  }
  getName(): string {
    return "INVERT";
  }
  apply(pixel: GrinPixel, _tick: number, _timing: number): void {
    pixel.r = 255 - pixel.r;
    pixel.g = 255 - pixel.g;
    pixel.b = 255 - pixel.b;
  }
  getMaxCpuCost(): number {
    return 1;
  }
  requiresState(): boolean {
    return false;
  }
}

export class RotateHueOpcode implements Opcode {
  getId(): number {
    return BaseOpcodeId.ROTATE_HUE;
  }
  getName(): string {
    return "ROTATE_HUE";
  }
  apply(pixel: GrinPixel, tick: number, timing: number): void {
    const rotation = TimingInterpreter.evaluate(timing, tick) * 360;
    const [h, s, l] = rgbToHsl(pixel.r, pixel.g, pixel.b);
    const newHue = (h + rotation) % 360;
    const [r, g, b] = hslToRgb(newHue, s, l);
    pixel.r = r;
    pixel.g = g;
    pixel.b = b;
  }
  getMaxCpuCost(): number {
    return 3;
  }
  requiresState(): boolean {
    return false;
  }
}

export class LockOpcode implements Opcode {
  getId(): number {
    return BaseOpcodeId.LOCK;
  }
  getName(): string {
    return "LOCK";
  }
  apply(pixel: GrinPixel, _tick: number, _timing: number): void {
    pixel.c = pixel.c | CONTROL_BYTE_MASKS.LOCK;
  }
  getMaxCpuCost(): number {
    return 1;
  }
  requiresState(): boolean {
    return false;
  }
}

export class UnlockOpcode implements Opcode {
  getId(): number {
    return BaseOpcodeId.UNLOCK;
  }
  getName(): string {
    return "UNLOCK";
  }
  apply(pixel: GrinPixel, _tick: number, _timing: number): void {
    pixel.c = pixel.c & ~CONTROL_BYTE_MASKS.LOCK;
  }
  getMaxCpuCost(): number {
    return 1;
  }
  requiresState(): boolean {
    return false;
  }
}

export class ToggleLockOpcode implements Opcode {
  getId(): number {
    return BaseOpcodeId.TOGGLE_LOCK;
  }
  getName(): string {
    return "TOGGLE_LOCK";
  }
  apply(pixel: GrinPixel, _tick: number, _timing: number): void {
    pixel.c = pixel.c ^ CONTROL_BYTE_MASKS.LOCK;
  }
  getMaxCpuCost(): number {
    return 1;
  }
  requiresState(): boolean {
    return false;
  }
}

function shiftChannel(value: number, tick: number, timing: number): number {
  const wave = TimingInterpreter.evaluate(timing, tick);
  const delta = Math.round((wave * 2 - 1) * 255);
  return clampByte(value + delta);
}

function clampByte(value: number): number {
  if (value < 0) {
    return 0;
  }
  if (value > 255) {
    return 255;
  }
  return value;
}

function rgbToHsl(r: number, g: number, b: number): [number, number, number] {
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
    if (h < 0) {
      h += 360;
    }
  }

  return [h, s, l];
}

function hslToRgb(h: number, s: number, l: number): [number, number, number] {
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
