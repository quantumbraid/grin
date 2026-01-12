export function readUint16LE(bytes, offset) {
  ensureRange(bytes, offset, 2);
  return bytes[offset] | (bytes[offset + 1] << 8);
}

export function readUint32LE(bytes, offset) {
  ensureRange(bytes, offset, 4);
  return (
    bytes[offset] |
    (bytes[offset + 1] << 8) |
    (bytes[offset + 2] << 16) |
    (bytes[offset + 3] << 24)
  ) >>> 0;
}

export function readUint64LE(bytes, offset) {
  ensureRange(bytes, offset, 8);
  const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
  return view.getBigUint64(offset, true);
}

export function writeUint16LE(value) {
  return new Uint8Array([value & 0xff, (value >> 8) & 0xff]);
}

export function writeUint32LE(value) {
  return new Uint8Array([
    value & 0xff,
    (value >> 8) & 0xff,
    (value >> 16) & 0xff,
    (value >> 24) & 0xff,
  ]);
}

export function writeUint64LE(value) {
  const buffer = new ArrayBuffer(8);
  const view = new DataView(buffer);
  view.setBigUint64(0, value, true);
  return new Uint8Array(buffer);
}

function ensureRange(bytes, offset, length) {
  if (offset < 0 || offset + length > bytes.length) {
    throw new RangeError("Read exceeds buffer length");
  }
}
