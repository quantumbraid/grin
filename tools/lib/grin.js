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
import {
  CONTROL_BYTE_MASKS,
  HEADER_FIELDS,
  HEADER_SIZE_BYTES,
  MAGIC_BYTES,
  MAX_RULE_COUNT,
  PIXEL_SIZE_BYTES,
  RULE_ENTRY_SIZE,
  RULES_BLOCK_SIZE,
  VERSION_MAJOR,
  VERSION_MINOR,
} from "./format.js";
import {
  readUint16LE,
  readUint32LE,
  readUint64LE,
  writeUint16LE,
  writeUint32LE,
  writeUint64LE,
} from "./endianness.js";

export function readGrinFile(input) {
  const bytes = toUint8Array(input);
  return parseGrinBytes(bytes);
}

export function parseGrinBytes(bytes) {
  if (bytes.length < HEADER_SIZE_BYTES) {
    throw new Error("Buffer too small for GRIN header");
  }

  const headerBytes = bytes.subarray(0, HEADER_SIZE_BYTES);
  const header = parseHeader(headerBytes);
  const rules = parseRulesBlock(header.rulesBlock, header.ruleCount);

  const pixelOffset = safeNumber(header.pixelDataOffset, "PixelDataOffset64");
  const pixelLength = safeNumber(header.pixelDataLength, "PixelDataLength");
  if (bytes.length < pixelOffset + pixelLength) {
    throw new Error("Buffer does not contain full pixel data");
  }
  const pixelBytes = bytes.subarray(pixelOffset, pixelOffset + pixelLength);
  const pixels = parsePixels(pixelBytes, header.width, header.height);

  return { header, pixels, rules };
}

export function serializeGrinFile(file) {
  const header = normalizeHeader(file);
  const headerBytes = serializeHeader(header);
  const pixelBytes = serializePixels(file.pixels);
  const output = new Uint8Array(headerBytes.length + pixelBytes.length);
  output.set(headerBytes, 0);
  output.set(pixelBytes, headerBytes.length);
  return output;
}

export function writeGrinFile(path, file) {
  const bytes = serializeGrinFile(file);
  fs.writeFileSync(path, bytes);
}

export function createGrinFile({
  width,
  height,
  pixels,
  rules = [],
  tickMicros = 0,
  opcodeSetId = 0,
}) {
  if (!Array.isArray(pixels)) {
    throw new Error("Pixels array is required");
  }
  if (!Number.isInteger(width) || !Number.isInteger(height)) {
    throw new Error("Width and height must be integers");
  }
  const rulesBlock = buildRulesBlock(rules);
  const pixelDataLength = BigInt(pixels.length) * BigInt(PIXEL_SIZE_BYTES);
  const fileLength = BigInt(HEADER_SIZE_BYTES) + pixelDataLength;

  const header = {
    magic: cloneBytes(MAGIC_BYTES, MAGIC_BYTES.length),
    versionMajor: VERSION_MAJOR,
    versionMinor: VERSION_MINOR,
    headerSize: HEADER_SIZE_BYTES,
    width,
    height,
    tickMicros,
    ruleCount: rules.length,
    opcodeSetId,
    flags: 0,
    pixelDataLength,
    fileLength,
    pixelDataOffset: BigInt(HEADER_SIZE_BYTES),
    reservedA: 0n,
    reservedB: 0n,
    rulesBlock,
  };

  return { header, pixels, rules };
}

export function parseHeader(bytes) {
  return {
    magic: cloneBytes(
      bytes.subarray(HEADER_FIELDS.MAGIC.offset, HEADER_FIELDS.MAGIC.offset + HEADER_FIELDS.MAGIC.size),
      HEADER_FIELDS.MAGIC.size
    ),
    versionMajor: bytes[HEADER_FIELDS.VERSION_MAJOR.offset],
    versionMinor: bytes[HEADER_FIELDS.VERSION_MINOR.offset],
    headerSize: readUint16LE(bytes, HEADER_FIELDS.HEADER_SIZE.offset),
    width: readUint32LE(bytes, HEADER_FIELDS.WIDTH.offset),
    height: readUint32LE(bytes, HEADER_FIELDS.HEIGHT.offset),
    tickMicros: readUint32LE(bytes, HEADER_FIELDS.TICK_MICROS.offset),
    ruleCount: bytes[HEADER_FIELDS.RULE_COUNT.offset],
    opcodeSetId: bytes[HEADER_FIELDS.OPCODE_SET_ID.offset],
    flags: readUint16LE(bytes, HEADER_FIELDS.FLAGS.offset),
    pixelDataLength: readUint64LE(bytes, HEADER_FIELDS.PIXEL_DATA_LENGTH.offset),
    fileLength: readUint64LE(bytes, HEADER_FIELDS.FILE_LENGTH.offset),
    pixelDataOffset: readUint64LE(bytes, HEADER_FIELDS.PIXEL_DATA_OFFSET.offset),
    reservedA: readUint64LE(bytes, HEADER_FIELDS.RESERVED_A.offset),
    reservedB: readUint64LE(bytes, HEADER_FIELDS.RESERVED_B.offset),
    rulesBlock: cloneBytes(
      bytes.subarray(
        HEADER_FIELDS.RULES_BLOCK.offset,
        HEADER_FIELDS.RULES_BLOCK.offset + HEADER_FIELDS.RULES_BLOCK.size
      ),
      RULES_BLOCK_SIZE
    ),
  };
}

export function serializeHeader(header) {
  const output = new Uint8Array(HEADER_SIZE_BYTES);
  output.set(cloneBytes(header.magic ?? MAGIC_BYTES, HEADER_FIELDS.MAGIC.size), HEADER_FIELDS.MAGIC.offset);
  output[HEADER_FIELDS.VERSION_MAJOR.offset] = header.versionMajor ?? VERSION_MAJOR;
  output[HEADER_FIELDS.VERSION_MINOR.offset] = header.versionMinor ?? VERSION_MINOR;
  output.set(writeUint16LE(header.headerSize ?? HEADER_SIZE_BYTES), HEADER_FIELDS.HEADER_SIZE.offset);
  output.set(writeUint32LE(header.width ?? 0), HEADER_FIELDS.WIDTH.offset);
  output.set(writeUint32LE(header.height ?? 0), HEADER_FIELDS.HEIGHT.offset);
  output.set(writeUint32LE(header.tickMicros ?? 0), HEADER_FIELDS.TICK_MICROS.offset);
  output[HEADER_FIELDS.RULE_COUNT.offset] = header.ruleCount ?? 0;
  output[HEADER_FIELDS.OPCODE_SET_ID.offset] = header.opcodeSetId ?? 0;
  output.set(writeUint16LE(header.flags ?? 0), HEADER_FIELDS.FLAGS.offset);
  output.set(writeUint64LE(toBigInt(header.pixelDataLength)), HEADER_FIELDS.PIXEL_DATA_LENGTH.offset);
  output.set(writeUint64LE(toBigInt(header.fileLength)), HEADER_FIELDS.FILE_LENGTH.offset);
  output.set(writeUint64LE(toBigInt(header.pixelDataOffset)), HEADER_FIELDS.PIXEL_DATA_OFFSET.offset);
  output.set(writeUint64LE(toBigInt(header.reservedA)), HEADER_FIELDS.RESERVED_A.offset);
  output.set(writeUint64LE(toBigInt(header.reservedB)), HEADER_FIELDS.RESERVED_B.offset);
  const rulesBlock = header.rulesBlock ?? new Uint8Array(RULES_BLOCK_SIZE);
  output.set(cloneBytes(rulesBlock, RULES_BLOCK_SIZE), HEADER_FIELDS.RULES_BLOCK.offset);
  return output;
}

export function parseRulesBlock(block, ruleCount) {
  if (ruleCount > MAX_RULE_COUNT) {
    throw new Error(`RuleCount exceeds ${MAX_RULE_COUNT}`);
  }
  if (block.length !== RULES_BLOCK_SIZE) {
    throw new Error("Rules block must be 64 bytes");
  }
  const rules = [];
  for (let i = 0; i < ruleCount; i += 1) {
    const offset = i * RULE_ENTRY_SIZE;
    const groupMask = readUint16LE(block, offset);
    const opcode = block[offset + 2];
    const timing = block[offset + 3];
    rules.push({ groupMask, opcode, timing });
  }
  return rules;
}

export function buildRulesBlock(rules) {
  if (rules.length > MAX_RULE_COUNT) {
    throw new Error(`RuleCount exceeds ${MAX_RULE_COUNT}`);
  }
  const block = new Uint8Array(RULES_BLOCK_SIZE);
  for (let i = 0; i < rules.length; i += 1) {
    const rule = rules[i];
    if (!rule) {
      continue;
    }
    const offset = i * RULE_ENTRY_SIZE;
    const groupMask = rule.groupMask ?? 0;
    block.set(writeUint16LE(groupMask), offset);
    block[offset + 2] = rule.opcode ?? 0;
    block[offset + 3] = rule.timing ?? 0;
  }
  return block;
}

export function parsePixels(pixelBytes, width, height) {
  const expectedLength = width * height * PIXEL_SIZE_BYTES;
  if (!Number.isSafeInteger(expectedLength)) {
    throw new Error("Pixel data length exceeds safe integer range");
  }
  if (pixelBytes.length !== expectedLength) {
    throw new Error("Pixel data length does not match header dimensions");
  }

  const pixels = [];
  for (let offset = 0; offset < pixelBytes.length; offset += PIXEL_SIZE_BYTES) {
    pixels.push({
      r: pixelBytes[offset],
      g: pixelBytes[offset + 1],
      b: pixelBytes[offset + 2],
      a: pixelBytes[offset + 3],
      c: pixelBytes[offset + 4],
    });
  }
  return pixels;
}

export function serializePixels(pixels) {
  const output = new Uint8Array(pixels.length * PIXEL_SIZE_BYTES);
  let offset = 0;
  for (const pixel of pixels) {
    output[offset] = pixel.r & 0xff;
    output[offset + 1] = pixel.g & 0xff;
    output[offset + 2] = pixel.b & 0xff;
    output[offset + 3] = pixel.a & 0xff;
    const control = pixel.c ?? 0;
    output[offset + 4] = control & ~CONTROL_BYTE_MASKS.RESERVED;
    offset += PIXEL_SIZE_BYTES;
  }
  return output;
}

function normalizeHeader(file) {
  if (!file.header) {
    throw new Error("File header is required");
  }
  const pixelDataLength = BigInt(file.pixels.length) * BigInt(PIXEL_SIZE_BYTES);
  const rulesBlock = buildRulesBlock(file.rules);
  const fileLength = BigInt(HEADER_SIZE_BYTES) + pixelDataLength;
  return {
    magic: cloneBytes(file.header.magic ?? MAGIC_BYTES, MAGIC_BYTES.length),
    versionMajor: file.header.versionMajor ?? VERSION_MAJOR,
    versionMinor: file.header.versionMinor ?? VERSION_MINOR,
    headerSize: HEADER_SIZE_BYTES,
    width: file.header.width,
    height: file.header.height,
    tickMicros: file.header.tickMicros ?? 0,
    ruleCount: file.rules.length,
    opcodeSetId: file.header.opcodeSetId ?? 0,
    flags: 0,
    pixelDataLength,
    fileLength,
    pixelDataOffset: BigInt(HEADER_SIZE_BYTES),
    reservedA: 0n,
    reservedB: 0n,
    rulesBlock,
  };
}

function toUint8Array(input) {
  if (typeof input === "string") {
    const buffer = fs.readFileSync(input);
    return new Uint8Array(buffer);
  }
  if (input instanceof Uint8Array) {
    return input;
  }
  if (input instanceof ArrayBuffer) {
    return new Uint8Array(input);
  }
  throw new Error("Unsupported input type for GRIN file");
}

function safeNumber(value, label) {
  if (value > BigInt(Number.MAX_SAFE_INTEGER)) {
    throw new Error(`${label} exceeds safe integer range`);
  }
  return Number(value);
}

function toBigInt(value) {
  if (typeof value === "bigint") {
    return value;
  }
  if (typeof value === "number") {
    return BigInt(value);
  }
  if (value === undefined || value === null) {
    return 0n;
  }
  throw new Error("Expected bigint-compatible value");
}

function cloneBytes(source, size) {
  const copy = new Uint8Array(size);
  copy.set(source.subarray(0, size));
  return copy;
}
