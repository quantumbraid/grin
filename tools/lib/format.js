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

// Control group labels intentionally skip I/O to keep a 16-slot alphabet and avoid ambiguity.
export const CONTROL_GROUP_LABELS = [
  "G",
  "H",
  "J",
  "K",
  "L",
  "M",
  "N",
  "P",
  "Q",
  "R",
  "S",
  "T",
  "U",
  "V",
  "W",
  "X",
];

// Control lock suffixes are reserved for the final control channel indicator.
export const CONTROL_LOCK_LABELS = {
  UNLOCKED: "Y",
  LOCKED: "Z",
};

// Control suffixes append to rrggbbaa to represent the control channel in authoring strings.
export const CONTROL_SUFFIX_PATTERN = new RegExp(
  `^[${CONTROL_GROUP_LABELS.join("")}][${CONTROL_LOCK_LABELS.UNLOCKED}${CONTROL_LOCK_LABELS.LOCKED}]$`,
  "i"
);

/**
 * Format a numeric group ID using the control-label alphabet.
 * @param {number} groupId - Group index (0-15).
 * @returns {string} Group label or "?" when out of range.
 */
export function formatControlGroupLabel(groupId) {
  if (!Number.isInteger(groupId)) {
    return "?";
  }
  return CONTROL_GROUP_LABELS[groupId] ?? "?";
}

/**
 * Parse a control-label character into a numeric group ID.
 * @param {string} label - Single-letter group label.
 * @returns {number | null} Group ID or null when the label is not recognized.
 */
export function parseControlGroupLabel(label) {
  if (!label) {
    return null;
  }
  const normalized = String(label).trim().toUpperCase();
  const index = CONTROL_GROUP_LABELS.indexOf(normalized);
  return index >= 0 ? index : null;
}

/**
 * Format the lock suffix for a control channel.
 * @param {boolean} locked - Whether the control byte is locked.
 * @returns {string} "Y" for unlocked or "Z" for locked.
 */
export function formatControlLockLabel(locked) {
  return locked ? CONTROL_LOCK_LABELS.LOCKED : CONTROL_LOCK_LABELS.UNLOCKED;
}

/**
 * Format the authoring control suffix that follows rrggbbaa.
 * @param {number} groupId - Group index (0-15).
 * @param {boolean} locked - Whether the control byte is locked.
 * @returns {string} Two-character suffix like "GY" or "GZ".
 */
export function formatControlSuffix(groupId, locked) {
  return `${formatControlGroupLabel(groupId)}${formatControlLockLabel(locked)}`;
}

/**
 * Validate a control suffix to detect corruption in authoring strings.
 * @param {string} suffix - Two-character suffix to validate.
 * @returns {boolean} True when the suffix matches a G-X + Y/Z pattern.
 */
export function isValidControlSuffix(suffix) {
  return CONTROL_SUFFIX_PATTERN.test(String(suffix).trim());
}

/**
 * Parse a control suffix into group/lock metadata when valid.
 * @param {string} suffix - Two-character suffix like "GY" or "GZ".
 * @returns {{groupId: number, locked: boolean} | null} Parsed payload or null when invalid.
 */
export function parseControlSuffix(suffix) {
  if (!isValidControlSuffix(suffix)) {
    return null;
  }
  const normalized = String(suffix).trim().toUpperCase();
  const groupId = parseControlGroupLabel(normalized[0]);
  if (groupId === null) {
    return null;
  }
  const locked = normalized[1] === CONTROL_LOCK_LABELS.LOCKED;
  return { groupId, locked };
}

export function groupMaskTargetsGroup(groupMask, groupId) {
  const shift = groupId & 0x0f;
  return (groupMask & (1 << shift)) !== 0;
}
