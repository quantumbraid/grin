import {
  HEADER_FIELDS,
  HEADER_SIZE_BYTES,
  MAGIC_BYTES,
  MAX_RULE_COUNT,
  PIXEL_SIZE_BYTES,
  RULES_BLOCK_SIZE,
  VERSION_MAJOR,
  VERSION_MINOR,
} from "./format";

export type GrinValidationResult = {
  ok: boolean;
  errors: string[];
  warnings: string[];
};

export type GrinHeaderInit = {
  width: number;
  height: number;
  tickMicros?: number;
  ruleCount?: number;
  opcodeSetId?: number;
  flags?: number;
  pixelDataLength?: bigint;
  fileLength?: bigint;
  pixelDataOffset?: bigint;
  reservedA?: bigint;
  reservedB?: bigint;
  rulesBlock?: Uint8Array;
  versionMajor?: number;
  versionMinor?: number;
  magic?: Uint8Array;
  headerSize?: number;
};

export class GrinHeader {
  magic: Uint8Array;
  versionMajor: number;
  versionMinor: number;
  headerSize: number;
  width: number;
  height: number;
  tickMicros: number;
  ruleCount: number;
  opcodeSetId: number;
  flags: number;
  pixelDataLength: bigint;
  fileLength: bigint;
  pixelDataOffset: bigint;
  reservedA: bigint;
  reservedB: bigint;
  rulesBlock: Uint8Array;

  constructor(init: GrinHeaderInit) {
    this.magic = copyBytes(init.magic ?? MAGIC_BYTES, HEADER_FIELDS.MAGIC.size);
    this.versionMajor = init.versionMajor ?? VERSION_MAJOR;
    this.versionMinor = init.versionMinor ?? VERSION_MINOR;
    this.headerSize = init.headerSize ?? HEADER_SIZE_BYTES;
    this.width = init.width;
    this.height = init.height;
    this.tickMicros = init.tickMicros ?? 0;
    this.ruleCount = init.ruleCount ?? 0;
    this.opcodeSetId = init.opcodeSetId ?? 0;
    this.flags = init.flags ?? 0;
    const expectedPixelLength =
      BigInt(this.width) * BigInt(this.height) * BigInt(PIXEL_SIZE_BYTES);
    this.pixelDataLength = init.pixelDataLength ?? expectedPixelLength;
    this.fileLength = init.fileLength ?? 0n;
    this.pixelDataOffset = init.pixelDataOffset ?? BigInt(HEADER_SIZE_BYTES);
    this.reservedA = init.reservedA ?? 0n;
    this.reservedB = init.reservedB ?? 0n;
    this.rulesBlock = copyBytes(
      init.rulesBlock ?? new Uint8Array(RULES_BLOCK_SIZE),
      RULES_BLOCK_SIZE
    );
  }

  static deserialize(bytes: Uint8Array): GrinHeader {
    if (bytes.length < HEADER_SIZE_BYTES) {
      throw new Error(`Header requires ${HEADER_SIZE_BYTES} bytes`);
    }
    const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
    const magic = copyBytes(
      bytes.subarray(HEADER_FIELDS.MAGIC.offset, HEADER_FIELDS.MAGIC.offset + HEADER_FIELDS.MAGIC.size),
      HEADER_FIELDS.MAGIC.size
    );
    const versionMajor = view.getUint8(HEADER_FIELDS.VERSION_MAJOR.offset);
    const versionMinor = view.getUint8(HEADER_FIELDS.VERSION_MINOR.offset);
    const headerSize = view.getUint16(HEADER_FIELDS.HEADER_SIZE.offset, true);
    const width = view.getUint32(HEADER_FIELDS.WIDTH.offset, true);
    const height = view.getUint32(HEADER_FIELDS.HEIGHT.offset, true);
    const tickMicros = view.getUint32(HEADER_FIELDS.TICK_MICROS.offset, true);
    const ruleCount = view.getUint8(HEADER_FIELDS.RULE_COUNT.offset);
    const opcodeSetId = view.getUint8(HEADER_FIELDS.OPCODE_SET_ID.offset);
    const flags = view.getUint16(HEADER_FIELDS.FLAGS.offset, true);
    const pixelDataLength = view.getBigUint64(HEADER_FIELDS.PIXEL_DATA_LENGTH.offset, true);
    const fileLength = view.getBigUint64(HEADER_FIELDS.FILE_LENGTH.offset, true);
    const pixelDataOffset = view.getBigUint64(HEADER_FIELDS.PIXEL_DATA_OFFSET.offset, true);
    const reservedA = view.getBigUint64(HEADER_FIELDS.RESERVED_A.offset, true);
    const reservedB = view.getBigUint64(HEADER_FIELDS.RESERVED_B.offset, true);
    const rulesBlock = copyBytes(
      bytes.subarray(
        HEADER_FIELDS.RULES_BLOCK.offset,
        HEADER_FIELDS.RULES_BLOCK.offset + HEADER_FIELDS.RULES_BLOCK.size
      ),
      RULES_BLOCK_SIZE
    );

    return new GrinHeader({
      magic,
      versionMajor,
      versionMinor,
      headerSize,
      width,
      height,
      tickMicros,
      ruleCount,
      opcodeSetId,
      flags,
      pixelDataLength,
      fileLength,
      pixelDataOffset,
      reservedA,
      reservedB,
      rulesBlock,
    });
  }

  serialize(): Uint8Array {
    const buffer = new ArrayBuffer(HEADER_SIZE_BYTES);
    const view = new DataView(buffer);
    const bytes = new Uint8Array(buffer);

    bytes.set(this.magic, HEADER_FIELDS.MAGIC.offset);
    view.setUint8(HEADER_FIELDS.VERSION_MAJOR.offset, this.versionMajor);
    view.setUint8(HEADER_FIELDS.VERSION_MINOR.offset, this.versionMinor);
    view.setUint16(HEADER_FIELDS.HEADER_SIZE.offset, this.headerSize, true);
    view.setUint32(HEADER_FIELDS.WIDTH.offset, this.width, true);
    view.setUint32(HEADER_FIELDS.HEIGHT.offset, this.height, true);
    view.setUint32(HEADER_FIELDS.TICK_MICROS.offset, this.tickMicros, true);
    view.setUint8(HEADER_FIELDS.RULE_COUNT.offset, this.ruleCount);
    view.setUint8(HEADER_FIELDS.OPCODE_SET_ID.offset, this.opcodeSetId);
    view.setUint16(HEADER_FIELDS.FLAGS.offset, this.flags, true);
    view.setBigUint64(HEADER_FIELDS.PIXEL_DATA_LENGTH.offset, this.pixelDataLength, true);
    view.setBigUint64(HEADER_FIELDS.FILE_LENGTH.offset, this.fileLength, true);
    view.setBigUint64(HEADER_FIELDS.PIXEL_DATA_OFFSET.offset, this.pixelDataOffset, true);
    view.setBigUint64(HEADER_FIELDS.RESERVED_A.offset, this.reservedA, true);
    view.setBigUint64(HEADER_FIELDS.RESERVED_B.offset, this.reservedB, true);
    bytes.set(this.rulesBlock, HEADER_FIELDS.RULES_BLOCK.offset);

    return bytes;
  }

  validate(): GrinValidationResult {
    const errors: string[] = [];
    const warnings: string[] = [];

    if (!matchesMagic(this.magic)) {
      errors.push("Magic bytes do not match GRIN");
    }

    if (this.headerSize !== HEADER_SIZE_BYTES) {
      errors.push(`HeaderSize must be ${HEADER_SIZE_BYTES}`);
    }

    if (this.ruleCount > MAX_RULE_COUNT) {
      errors.push(`RuleCount exceeds ${MAX_RULE_COUNT}`);
    }

    if (this.pixelDataOffset !== BigInt(HEADER_SIZE_BYTES)) {
      errors.push("PixelDataOffset64 must be 128");
    }

    const expectedPixelLength =
      BigInt(this.width) * BigInt(this.height) * BigInt(PIXEL_SIZE_BYTES);
    if (this.pixelDataLength !== expectedPixelLength) {
      errors.push("PixelDataLength does not match width * height * 5");
    }

    const minFileLength = BigInt(HEADER_SIZE_BYTES) + this.pixelDataLength;
    if (this.fileLength !== 0n && this.fileLength < minFileLength) {
      errors.push("FileLength is smaller than header + pixel data");
    }

    if (this.rulesBlock.length !== RULES_BLOCK_SIZE) {
      errors.push("RulesBlock must be 64 bytes");
    }

    if (this.flags !== 0) {
      warnings.push("Flags field is non-zero");
    }

    if (this.reservedA !== 0n || this.reservedB !== 0n) {
      warnings.push("Reserved header fields are non-zero");
    }

    if (this.versionMajor !== VERSION_MAJOR || this.versionMinor !== VERSION_MINOR) {
      warnings.push("Unexpected GRIN version");
    }

    return { ok: errors.length === 0, errors, warnings };
  }

  clone(overrides: Partial<GrinHeaderInit> = {}): GrinHeader {
    return new GrinHeader({
      magic: overrides.magic ?? this.magic,
      versionMajor: overrides.versionMajor ?? this.versionMajor,
      versionMinor: overrides.versionMinor ?? this.versionMinor,
      headerSize: overrides.headerSize ?? this.headerSize,
      width: overrides.width ?? this.width,
      height: overrides.height ?? this.height,
      tickMicros: overrides.tickMicros ?? this.tickMicros,
      ruleCount: overrides.ruleCount ?? this.ruleCount,
      opcodeSetId: overrides.opcodeSetId ?? this.opcodeSetId,
      flags: overrides.flags ?? this.flags,
      pixelDataLength: overrides.pixelDataLength ?? this.pixelDataLength,
      fileLength: overrides.fileLength ?? this.fileLength,
      pixelDataOffset: overrides.pixelDataOffset ?? this.pixelDataOffset,
      reservedA: overrides.reservedA ?? this.reservedA,
      reservedB: overrides.reservedB ?? this.reservedB,
      rulesBlock: overrides.rulesBlock ?? this.rulesBlock,
    });
  }
}

function matchesMagic(bytes: Uint8Array): boolean {
  if (bytes.length !== MAGIC_BYTES.length) {
    return false;
  }
  for (let i = 0; i < MAGIC_BYTES.length; i += 1) {
    if (bytes[i] !== MAGIC_BYTES[i]) {
      return false;
    }
  }
  return true;
}

function copyBytes(source: Uint8Array, size: number): Uint8Array {
  if (source.length !== size) {
    const copy = new Uint8Array(size);
    copy.set(source.subarray(0, size));
    return copy;
  }
  return new Uint8Array(source);
}
