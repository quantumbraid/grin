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
import { HEADER_FIELDS, HEADER_SIZE_BYTES, RULES_BLOCK_SIZE } from "./format";
import { GrinHeader } from "./grin-header";
import { GrinFile } from "./grin-file";
import { readPixelData, readRulesBlock } from "./grin-io";

export class GrinReader {
  private bytes: Uint8Array;
  private view: DataView;

  constructor(buffer: ArrayBuffer | Uint8Array) {
    this.bytes = buffer instanceof Uint8Array ? buffer : new Uint8Array(buffer);
    this.view = new DataView(this.bytes.buffer, this.bytes.byteOffset, this.bytes.byteLength);
  }

  readHeader(): GrinHeader {
    if (this.bytes.length < HEADER_SIZE_BYTES) {
      throw new Error("Buffer too small for GRIN header");
    }
    const magic = this.bytes.subarray(0, 4);
    const versionMajor = this.view.getUint8(HEADER_FIELDS.VERSION_MAJOR.offset);
    const versionMinor = this.view.getUint8(HEADER_FIELDS.VERSION_MINOR.offset);
    const headerSize = this.view.getUint16(HEADER_FIELDS.HEADER_SIZE.offset, true);
    const width = this.view.getUint32(HEADER_FIELDS.WIDTH.offset, true);
    const height = this.view.getUint32(HEADER_FIELDS.HEIGHT.offset, true);
    const tickMicros = this.view.getUint32(HEADER_FIELDS.TICK_MICROS.offset, true);
    const ruleCount = this.view.getUint8(HEADER_FIELDS.RULE_COUNT.offset);
    const opcodeSetId = this.view.getUint8(HEADER_FIELDS.OPCODE_SET_ID.offset);
    const flags = this.view.getUint16(HEADER_FIELDS.FLAGS.offset, true);
    const pixelDataLength = this.view.getBigUint64(HEADER_FIELDS.PIXEL_DATA_LENGTH.offset, true);
    const fileLength = this.view.getBigUint64(HEADER_FIELDS.FILE_LENGTH.offset, true);
    const pixelDataOffset = this.view.getBigUint64(HEADER_FIELDS.PIXEL_DATA_OFFSET.offset, true);
    const reservedA = this.view.getBigUint64(HEADER_FIELDS.RESERVED_A.offset, true);
    const reservedB = this.view.getBigUint64(HEADER_FIELDS.RESERVED_B.offset, true);
    const rulesBlock = this.bytes.subarray(
      HEADER_FIELDS.RULES_BLOCK.offset,
      HEADER_FIELDS.RULES_BLOCK.offset + RULES_BLOCK_SIZE
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

  readFile(): GrinFile {
    const header = this.readHeader();
    const rules = readRulesBlock(header.rulesBlock, header.ruleCount, header.opcodeSetId).rules;
    const pixelOffset = Number(header.pixelDataOffset);
    const pixelLength = Number(header.pixelDataLength);
    const pixelBytes = this.bytes.subarray(pixelOffset, pixelOffset + pixelLength);
    const pixels = readPixelData(pixelBytes, header.width, header.height).pixels;
    return new GrinFile(header, pixels, rules);
  }
}
