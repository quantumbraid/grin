#!/usr/bin/env node

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
import { readImage } from "../lib/png.js";
import { createGrinFile, serializeGrinFile } from "../lib/grin.js";
import { validateGrinFile } from "../lib/validation.js";
import { CONTROL_BYTE_MASKS } from "../lib/format.js";

const args = process.argv.slice(2);
let inputPath = null;
let outputPath = null;
let maskPath = null;
let rulesPath = null;

for (let i = 0; i < args.length; i += 1) {
  const arg = args[i];
  if (arg === "--groups") {
    maskPath = args[i + 1];
    i += 1;
  } else if (arg === "--rules") {
    rulesPath = args[i + 1];
    i += 1;
  } else if (!arg.startsWith("--")) {
    if (!inputPath) {
      inputPath = arg;
    } else if (!outputPath) {
      outputPath = arg;
    }
  }
}

if (!inputPath || !outputPath) {
  printUsage();
  process.exit(1);
}

try {
  const inputImage = readImage(inputPath);
  const maskImage = maskPath ? readImage(maskPath) : null;

  if (maskImage && (maskImage.width !== inputImage.width || maskImage.height !== inputImage.height)) {
    throw new Error("Group mask dimensions must match input image");
  }

  const rulesConfig = rulesPath ? loadRules(rulesPath) : { rules: [], tickMicros: 0, opcodeSetId: 0 };

  const pixels = buildPixels(inputImage, maskImage);
  const file = createGrinFile({
    width: inputImage.width,
    height: inputImage.height,
    pixels,
    rules: rulesConfig.rules,
    tickMicros: rulesConfig.tickMicros,
    opcodeSetId: rulesConfig.opcodeSetId,
  });

  const report = validateGrinFile(file, "permissive");
  if (!report.ok) {
    throw new Error(`Validation failed: ${report.errors.join("; ")}`);
  }

  const bytes = serializeGrinFile(file);
  fs.writeFileSync(outputPath, bytes);
  process.stdout.write(`Wrote ${outputPath}\n`);
} catch (error) {
  const message = error instanceof Error ? error.message : String(error);
  process.stderr.write(`grin-encode: ${message}\n`);
  process.exit(1);
}

function buildPixels(inputImage, maskImage) {
  const pixels = [];
  const data = inputImage.data;
  const mask = maskImage ? maskImage.data : null;

  for (let i = 0; i < data.length; i += 4) {
    const r = data[i];
    const g = data[i + 1];
    const b = data[i + 2];
    const a = data[i + 3];
    const groupId = mask ? mapGroupId(mask[i]) : 0;
    const control = (groupId & CONTROL_BYTE_MASKS.GROUP_ID) | 0;
    pixels.push({ r, g, b, a, c: control });
  }
  return pixels;
}

function mapGroupId(value) {
  if (value <= 15) {
    return value;
  }
  const mapped = Math.round(value / 17);
  if (mapped < 0) return 0;
  if (mapped > 15) return 15;
  return mapped;
}

function loadRules(rulesPath) {
  const raw = fs.readFileSync(rulesPath, "utf8");
  const parsed = JSON.parse(raw);
  const rules = Array.isArray(parsed) ? parsed : parsed.rules || [];
  const tickMicros = Array.isArray(parsed) ? 0 : parsed.tickMicros ?? 0;
  const opcodeSetId = Array.isArray(parsed) ? 0 : parsed.opcodeSetId ?? 0;

  return {
    rules: rules.map((rule) => ({
      groupMask: parseGroupMask(rule.groupMask ?? rule.groups ?? 0),
      opcode: parseNumber(rule.opcode ?? 0),
      timing: parseNumber(rule.timing ?? 0),
    })),
    tickMicros: parseNumber(tickMicros),
    opcodeSetId: parseNumber(opcodeSetId),
  };
}

function parseGroupMask(value) {
  if (Array.isArray(value)) {
    return value.reduce((mask, groupId) => mask | (1 << (parseNumber(groupId) & 0x0f)), 0);
  }
  return parseNumber(value);
}

function parseNumber(value) {
  if (typeof value === "string") {
    const parsed = Number.parseInt(value, 0);
    if (Number.isNaN(parsed)) {
      throw new Error(`Invalid numeric value: ${value}`);
    }
    return parsed;
  }
  if (typeof value === "number") {
    return value;
  }
  return 0;
}

function printUsage() {
  process.stdout.write(
    "Usage: grin-encode <input.png> <output.grin> [--groups <mask.png>] [--rules <rules.json>]\n"
  );
}
