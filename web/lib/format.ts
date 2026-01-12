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
// Spec references: §§4, 4.1, 7.2-7.4 (grin_technical_specification_v_2.md)

export const MAGIC_BYTES = new Uint8Array([0x47, 0x52, 0x49, 0x4e]); // "GRIN"
export const VERSION_MAJOR = 0x00;
export const VERSION_MINOR = 0x00;
export const HEADER_SIZE_BYTES = 128;
export const PIXEL_SIZE_BYTES = 5;
export const MAX_RULE_COUNT = 16;
export const RULES_BLOCK_SIZE = 64;
export const RULE_ENTRY_SIZE = 4;

export const HEADER_FIELDS = {
  MAGIC: { offset: 0, size: 4 },
  VERSION_MAJOR: { offset: 4, size: 1 },
  VERSION_MINOR: { offset: 5, size: 1 },
  HEADER_SIZE: { offset: 6, size: 2 },
  WIDTH: { offset: 8, size: 4 },
  HEIGHT: { offset: 12, size: 4 },
  TICK_MICROS: { offset: 16, size: 4 },
  RULE_COUNT: { offset: 20, size: 1 },
  OPCODE_SET_ID: { offset: 21, size: 1 },
  FLAGS: { offset: 22, size: 2 },
  PIXEL_DATA_LENGTH: { offset: 24, size: 8 },
  FILE_LENGTH: { offset: 32, size: 8 },
  PIXEL_DATA_OFFSET: { offset: 40, size: 8 },
  RESERVED_A: { offset: 48, size: 8 },
  RESERVED_B: { offset: 56, size: 8 },
  RULES_BLOCK: { offset: 64, size: 64 },
} as const;

export const RULE_ENTRY_FIELDS = {
  GROUP_MASK: { offset: 0, size: 2 },
  OPCODE: { offset: 2, size: 1 },
  TIMING: { offset: 3, size: 1 },
} as const;

export const TIMING_SEMANTICS = "reader-defined oscillator control";

export function groupMaskTargetsGroup(groupMask: number, groupId: number): boolean {
  const shift = groupId & 0x0f;
  return (groupMask & (1 << shift)) !== 0;
}

export const PIXEL_FIELDS = {
  R: { offset: 0, size: 1 },
  G: { offset: 1, size: 1 },
  B: { offset: 2, size: 1 },
  A: { offset: 3, size: 1 },
  C: { offset: 4, size: 1 },
} as const;

export const CONTROL_BYTE_MASKS = {
  GROUP_ID: 0x0f,
  RESERVED: 0x70,
  LOCK: 0x80,
} as const;

export function getGroupId(controlByte: number): number {
  return controlByte & CONTROL_BYTE_MASKS.GROUP_ID;
}

export function isLocked(controlByte: number): boolean {
  return (controlByte & CONTROL_BYTE_MASKS.LOCK) !== 0;
}

export function setGroupId(controlByte: number, groupId: number): number {
  const cleared = controlByte & ~CONTROL_BYTE_MASKS.GROUP_ID;
  return cleared | (groupId & CONTROL_BYTE_MASKS.GROUP_ID);
}

export function setLocked(controlByte: number, locked: boolean): number {
  if (locked) {
    return controlByte | CONTROL_BYTE_MASKS.LOCK;
  }
  return controlByte & ~CONTROL_BYTE_MASKS.LOCK;
}
