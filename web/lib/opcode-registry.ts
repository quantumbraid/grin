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
import {
  BaseOpcodeId,
  FadeInOpcode,
  FadeOutOpcode,
  InvertOpcode,
  LockOpcode,
  NopOpcode,
  Opcode,
  PulseOpcode,
  RotateHueOpcode,
  ShiftAOpcode,
  ShiftBOpcode,
  ShiftGOpcode,
  ShiftROpcode,
  ToggleLockOpcode,
  UnlockOpcode,
} from "./opcodes";

export class OpcodeRegistry {
  private static instance: OpcodeRegistry | null = null;
  private readonly baseOpcodes: Map<number, Opcode>;

  private constructor() {
    this.baseOpcodes = new Map<number, Opcode>([
      [BaseOpcodeId.NOP, new NopOpcode()],
      [BaseOpcodeId.FADE_IN, new FadeInOpcode()],
      [BaseOpcodeId.FADE_OUT, new FadeOutOpcode()],
      [BaseOpcodeId.PULSE, new PulseOpcode()],
      [BaseOpcodeId.SHIFT_R, new ShiftROpcode()],
      [BaseOpcodeId.SHIFT_G, new ShiftGOpcode()],
      [BaseOpcodeId.SHIFT_B, new ShiftBOpcode()],
      [BaseOpcodeId.SHIFT_A, new ShiftAOpcode()],
      [BaseOpcodeId.INVERT, new InvertOpcode()],
      [BaseOpcodeId.ROTATE_HUE, new RotateHueOpcode()],
      [BaseOpcodeId.LOCK, new LockOpcode()],
      [BaseOpcodeId.UNLOCK, new UnlockOpcode()],
      [BaseOpcodeId.TOGGLE_LOCK, new ToggleLockOpcode()],
    ]);
  }

  static getInstance(): OpcodeRegistry {
    if (!OpcodeRegistry.instance) {
      OpcodeRegistry.instance = new OpcodeRegistry();
    }
    return OpcodeRegistry.instance;
  }

  getOpcode(opcodeSetId: number, opcodeId: number): Opcode {
    if (opcodeSetId !== 0) {
      throw new Error(`Unknown OpcodeSetId ${opcodeSetId}`);
    }
    const opcode = this.baseOpcodes.get(opcodeId);
    if (!opcode) {
      throw new Error(`Unknown opcode ${opcodeId} for base set`);
    }
    return opcode;
  }

  isValidOpcode(opcodeSetId: number, opcodeId: number): boolean {
    if (opcodeSetId !== 0) {
      return false;
    }
    return this.baseOpcodes.has(opcodeId);
  }

  listOpcodes(opcodeSetId: number): Opcode[] {
    if (opcodeSetId !== 0) {
      return [];
    }
    return Array.from(this.baseOpcodes.values());
  }
}
