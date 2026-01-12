export const MAGIC_BYTES = new Uint8Array([0x47, 0x52, 0x49, 0x4e]);
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
};

export const CONTROL_BYTE_MASKS = {
  GROUP_ID: 0x0f,
  RESERVED: 0x70,
  LOCK: 0x80,
};

export function groupMaskTargetsGroup(groupMask, groupId) {
  const shift = groupId & 0x0f;
  return (groupMask & (1 << shift)) !== 0;
}
