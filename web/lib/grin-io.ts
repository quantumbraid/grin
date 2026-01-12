import {
  CONTROL_BYTE_MASKS,
  HEADER_SIZE_BYTES,
  MAX_RULE_COUNT,
  PIXEL_SIZE_BYTES,
  RULE_ENTRY_SIZE,
  RULES_BLOCK_SIZE,
} from "./format";
import { GrinHeader } from "./grin-header";
import { GrinPixel } from "./grin-pixel";
import { GrinRule } from "./grin-rule";

export type GrinHeaderReadResult = {
  header: GrinHeader;
  warnings: string[];
};

export type GrinPixelReadResult = {
  pixels: GrinPixel[];
  warnings: string[];
};

export type GrinRulesReadResult = {
  rules: GrinRule[];
  warnings: string[];
};

export function readHeader(bytes: Uint8Array): GrinHeaderReadResult {
  if (bytes.length < HEADER_SIZE_BYTES) {
    throw new Error("Buffer too small for GRIN header");
  }
  const headerBytes = bytes.subarray(0, HEADER_SIZE_BYTES);
  const header = GrinHeader.deserialize(headerBytes);
  const validation = header.validate();
  if (!validation.ok) {
    throw new Error(`Invalid GRIN header: ${validation.errors.join("; ")}`);
  }
  return { header, warnings: validation.warnings };
}

export function writeHeader(header: GrinHeader): Uint8Array {
  const width = toSafeBigInt(header.width, "Width");
  const height = toSafeBigInt(header.height, "Height");
  const pixelDataLength = width * height * BigInt(PIXEL_SIZE_BYTES);
  const fileLength =
    header.fileLength !== 0n
      ? header.fileLength
      : BigInt(HEADER_SIZE_BYTES) + pixelDataLength;

  const normalized = header.clone({
    headerSize: HEADER_SIZE_BYTES,
    pixelDataLength,
    fileLength,
    pixelDataOffset: BigInt(HEADER_SIZE_BYTES),
    flags: 0,
    reservedA: 0n,
    reservedB: 0n,
  });

  return normalized.serialize();
}

export function readPixelData(
  bytes: Uint8Array,
  width: number,
  height: number
): GrinPixelReadResult {
  const expectedLength = safePixelLength(width, height);
  if (bytes.length !== expectedLength) {
    throw new Error("Pixel data length does not match header dimensions");
  }

  const pixels: GrinPixel[] = [];
  let reservedCount = 0;
  for (let offset = 0; offset < bytes.length; offset += PIXEL_SIZE_BYTES) {
    const slice = bytes.subarray(offset, offset + PIXEL_SIZE_BYTES);
    const controlByte = slice[4] ?? 0;
    if ((controlByte & CONTROL_BYTE_MASKS.RESERVED) !== 0) {
      reservedCount += 1;
    }
    pixels.push(GrinPixel.fromBytes(slice));
  }

  const warnings: string[] = [];
  if (reservedCount > 0) {
    warnings.push(`Control byte reserved bits set on ${reservedCount} pixels`);
  }
  return { pixels, warnings };
}

export function writePixelData(pixels: GrinPixel[]): Uint8Array {
  const output = new Uint8Array(pixels.length * PIXEL_SIZE_BYTES);
  let offset = 0;
  for (const pixel of pixels) {
    output[offset] = pixel.r & 0xff;
    output[offset + 1] = pixel.g & 0xff;
    output[offset + 2] = pixel.b & 0xff;
    output[offset + 3] = pixel.a & 0xff;
    const control = pixel.c & 0xff;
    output[offset + 4] = control & ~CONTROL_BYTE_MASKS.RESERVED;
    offset += PIXEL_SIZE_BYTES;
  }
  return output;
}

export function readRulesBlock(
  bytes: Uint8Array,
  ruleCount: number,
  opcodeSetId: number
): GrinRulesReadResult {
  if (bytes.length !== RULES_BLOCK_SIZE) {
    throw new Error("Rules block must be exactly 64 bytes");
  }
  if (ruleCount > MAX_RULE_COUNT) {
    throw new Error(`RuleCount exceeds ${MAX_RULE_COUNT}`);
  }

  const rules: GrinRule[] = [];
  for (let i = 0; i < ruleCount; i += 1) {
    const offset = i * RULE_ENTRY_SIZE;
    const slice = bytes.subarray(offset, offset + RULE_ENTRY_SIZE);
    const rule = GrinRule.deserialize(slice);
    if (!isValidOpcode(opcodeSetId, rule.opcode)) {
      throw new Error(`Unknown opcode ${rule.opcode} for opcode set ${opcodeSetId}`);
    }
    rules.push(rule);
  }

  const warnings: string[] = [];
  for (let i = ruleCount * RULE_ENTRY_SIZE; i < bytes.length; i += 1) {
    if (bytes[i] !== 0) {
      warnings.push("Unused rules block entries are non-zero");
      break;
    }
  }

  return { rules, warnings };
}

export function writeRulesBlock(rules: GrinRule[], ruleCount: number): Uint8Array {
  if (ruleCount > MAX_RULE_COUNT) {
    throw new Error(`RuleCount exceeds ${MAX_RULE_COUNT}`);
  }
  if (rules.length < ruleCount) {
    throw new Error("Rule list is shorter than ruleCount");
  }
  const block = new Uint8Array(RULES_BLOCK_SIZE);
  for (let i = 0; i < ruleCount; i += 1) {
    const rule = rules[i];
    if (!rule) {
      continue;
    }
    if (rule.groupMask < 0 || rule.groupMask > 0xffff) {
      throw new Error("GroupMask must fit in 16 bits");
    }
    block.set(rule.serialize(), i * RULE_ENTRY_SIZE);
  }
  return block;
}

function safePixelLength(width: number, height: number): number {
  if (!Number.isInteger(width) || !Number.isInteger(height) || width < 0 || height < 0) {
    throw new Error("Width and height must be non-negative integers");
  }
  const pixelCount = width * height;
  if (!Number.isSafeInteger(pixelCount)) {
    throw new Error("Pixel count exceeds safe integer range");
  }
  const length = pixelCount * PIXEL_SIZE_BYTES;
  if (!Number.isSafeInteger(length)) {
    throw new Error("Pixel data length exceeds safe integer range");
  }
  return length;
}

function toSafeBigInt(value: number, label: string): bigint {
  if (!Number.isSafeInteger(value) || value < 0) {
    throw new Error(`${label} must be a non-negative safe integer`);
  }
  return BigInt(value);
}

function isValidOpcode(opcodeSetId: number, opcode: number): boolean {
  if (opcodeSetId !== 0) {
    return false;
  }
  return opcode >= 0x00 && opcode <= 0x0c;
}
