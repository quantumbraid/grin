export function writeUint16LE(value: number): Uint8Array {
  const buffer = new ArrayBuffer(2);
  const view = new DataView(buffer);
  view.setUint16(0, value & 0xffff, true);
  return new Uint8Array(buffer);
}

export function writeUint32LE(value: number): Uint8Array {
  const buffer = new ArrayBuffer(4);
  const view = new DataView(buffer);
  view.setUint32(0, value >>> 0, true);
  return new Uint8Array(buffer);
}

export function writeUint64LE(value: bigint): Uint8Array {
  const buffer = new ArrayBuffer(8);
  const view = new DataView(buffer);
  view.setBigUint64(0, value, true);
  return new Uint8Array(buffer);
}

export function readUint16LE(bytes: Uint8Array, offset: number): number {
  ensureRange(bytes, offset, 2);
  const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
  return view.getUint16(offset, true);
}

export function readUint32LE(bytes: Uint8Array, offset: number): number {
  ensureRange(bytes, offset, 4);
  const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
  return view.getUint32(offset, true);
}

export function readUint64LE(bytes: Uint8Array, offset: number): bigint {
  ensureRange(bytes, offset, 8);
  const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
  return view.getBigUint64(offset, true);
}

function ensureRange(bytes: Uint8Array, offset: number, length: number): void {
  if (offset < 0 || offset + length > bytes.length) {
    throw new RangeError("Read exceeds buffer length");
  }
}
