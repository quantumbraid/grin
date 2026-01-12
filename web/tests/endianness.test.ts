import { test } from "vitest";
import assert from "node:assert/strict";
import {
  readUint16LE,
  readUint32LE,
  readUint64LE,
  writeUint16LE,
  writeUint32LE,
  writeUint64LE,
} from "../lib/endianness";

test("endianness helpers enforce little-endian interpretation", () => {
  const buffer = new ArrayBuffer(4);
  const view = new DataView(buffer);
  view.setUint32(0, 0x12345678, false); // big-endian encoding
  const bytes = new Uint8Array(buffer);
  assert.equal(readUint32LE(bytes, 0), 0x78563412);
});

test("writeUint16LE writes expected bytes", () => {
  assert.deepEqual(writeUint16LE(0x1234), new Uint8Array([0x34, 0x12]));
});

test("writeUint32LE writes expected bytes", () => {
  assert.deepEqual(writeUint32LE(0x78563412), new Uint8Array([0x12, 0x34, 0x56, 0x78]));
});

test("writeUint64LE writes expected bytes", () => {
  assert.deepEqual(
    writeUint64LE(0x0807060504030201n),
    new Uint8Array([0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08])
  );
});

test("readUint16LE reads expected value", () => {
  assert.equal(readUint16LE(new Uint8Array([0x34, 0x12]), 0), 0x1234);
});

test("readUint64LE reads expected value", () => {
  assert.equal(
    readUint64LE(new Uint8Array([0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08]), 0),
    0x0807060504030201n
  );
});
