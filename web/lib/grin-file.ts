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
import {
  HEADER_SIZE_BYTES,
  MAX_RULE_COUNT,
  PIXEL_SIZE_BYTES,
  RULE_ENTRY_SIZE,
  RULES_BLOCK_SIZE,
} from "./format";
import { GrinHeader } from "./grin-header";
import { GrinImage } from "./grin-image";
import { GrinPixel } from "./grin-pixel";
import { GrinRule } from "./grin-rule";
import { writePixelData } from "./grin-io";

export class GrinFile {
  header: GrinHeader;
  pixels: GrinPixel[];
  rules: GrinRule[];

  constructor(header: GrinHeader, pixels: GrinPixel[], rules: GrinRule[]) {
    this.header = header;
    this.pixels = pixels;
    this.rules = rules;
  }

  static load(input: ArrayBuffer | Uint8Array | string): GrinFile {
    if (typeof input === "string") {
      const fs = getNodeFs();
      if (!fs) {
        throw new Error("GrinFile.load(path) is only available in Node.js");
      }
      const bytes = fs.readFileSync(input) as Uint8Array;
      return GrinFile.loadBytes(bytes);
    }
    const bytes = input instanceof Uint8Array ? input : new Uint8Array(input);
    return GrinFile.loadBytes(bytes);
  }

  toBytes(): Uint8Array {
    const rulesBlock = buildRulesBlock(this.rules);
    const pixelDataLength = BigInt(this.pixels.length) * BigInt(PIXEL_SIZE_BYTES);
    const header = this.header.clone({
      ruleCount: this.rules.length,
      rulesBlock,
      pixelDataLength,
      pixelDataOffset: BigInt(HEADER_SIZE_BYTES),
    });

    const outputLength = HEADER_SIZE_BYTES + safeNumber(pixelDataLength, "PixelDataLength");
    const output = new Uint8Array(outputLength);
    output.set(header.serialize(), 0);

    const pixelBytes = writePixelData(this.pixels);
    output.set(pixelBytes, HEADER_SIZE_BYTES);

    return output;
  }

  save(path: string): void {
    const fs = getNodeFs();
    if (!fs) {
      throw new Error("GrinFile.save(path) is only available in Node.js");
    }
    fs.writeFileSync(path, this.toBytes());
  }

  private static loadBytes(bytes: Uint8Array): GrinFile {
    if (bytes.length < HEADER_SIZE_BYTES) {
      throw new Error("Buffer too small for GRIN header");
    }

    const headerBytes = bytes.subarray(0, HEADER_SIZE_BYTES);
    const header = GrinHeader.deserialize(headerBytes);
    const validation = header.validate();
    if (!validation.ok) {
      throw new Error(`Invalid GRIN header: ${validation.errors.join("; ")}`);
    }

    const rules = parseRules(header.rulesBlock, header.ruleCount);
    const pixelOffset = safeNumber(header.pixelDataOffset, "PixelDataOffset64");
    const pixelLength = safeNumber(header.pixelDataLength, "PixelDataLength");
    if (bytes.length < pixelOffset + pixelLength) {
      throw new Error("Buffer does not contain full pixel data");
    }
    const pixelBytes = bytes.subarray(pixelOffset, pixelOffset + pixelLength);
    const pixels = parsePixels(pixelBytes, header.width, header.height);
    const image = new GrinImage(header, pixels, rules);

    return new GrinFile(image.header, image.pixels, image.rules);
  }
}

function parseRules(rulesBlock: Uint8Array, ruleCount: number): GrinRule[] {
  if (ruleCount > MAX_RULE_COUNT) {
    throw new Error(`RuleCount exceeds ${MAX_RULE_COUNT}`);
  }
  const rules: GrinRule[] = [];
  for (let i = 0; i < ruleCount; i += 1) {
    const offset = i * RULE_ENTRY_SIZE;
    const slice = rulesBlock.subarray(offset, offset + RULE_ENTRY_SIZE);
    rules.push(GrinRule.deserialize(slice));
  }
  return rules;
}

function parsePixels(pixelBytes: Uint8Array, width: number, height: number): GrinPixel[] {
  const expectedLength = width * height * PIXEL_SIZE_BYTES;
  if (!Number.isSafeInteger(expectedLength)) {
    throw new Error("Pixel data length exceeds safe integer range");
  }
  if (pixelBytes.length !== expectedLength) {
    throw new Error("Pixel data length does not match header dimensions");
  }

  const pixels: GrinPixel[] = [];
  for (let offset = 0; offset < pixelBytes.length; offset += PIXEL_SIZE_BYTES) {
    pixels.push(GrinPixel.fromBytes(pixelBytes.subarray(offset, offset + PIXEL_SIZE_BYTES)));
  }
  return pixels;
}

function buildRulesBlock(rules: GrinRule[]): Uint8Array {
  if (rules.length > MAX_RULE_COUNT) {
    throw new Error(`Rule count exceeds ${MAX_RULE_COUNT}`);
  }
  const rulesBlock = new Uint8Array(RULES_BLOCK_SIZE);
  for (let i = 0; i < rules.length; i += 1) {
    const rule = rules[i];
    if (!rule) {
      continue;
    }
    const offset = i * RULE_ENTRY_SIZE;
    rulesBlock.set(rule.serialize(), offset);
  }
  return rulesBlock;
}

function safeNumber(value: bigint, label: string): number {
  if (value > BigInt(Number.MAX_SAFE_INTEGER)) {
    throw new Error(`${label} exceeds safe integer range`);
  }
  return Number(value);
}

function getNodeFs(): typeof import("fs") | null {
  const request = Function("return typeof require === 'function' ? require : null")() as
    | ((id: string) => typeof import("fs"))
    | null;
  if (!request) {
    return null;
  }
  return request("fs");
}
