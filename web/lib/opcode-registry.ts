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
