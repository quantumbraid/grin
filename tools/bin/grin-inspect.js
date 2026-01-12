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
import { readGrinFile } from "../lib/grin.js";
import { BaseOpcodeId } from "../lib/opcodes.js";
import { CONTROL_BYTE_MASKS } from "../lib/format.js";

const args = process.argv.slice(2);
const json = args.includes("--json");
const showHeader = args.includes("--header");
const showRules = args.includes("--rules");
const showGroups = args.includes("--groups");
const showPixels = args.includes("--pixels");
const filePath = args.find((arg) => !arg.startsWith("--"));

if (!filePath) {
  printUsage();
  process.exit(1);
}

const sections = {
  header: showHeader,
  rules: showRules,
  groups: showGroups,
  pixels: showPixels,
};

if (!showHeader && !showRules && !showGroups && !showPixels) {
  sections.header = true;
  sections.rules = true;
  sections.groups = true;
}

try {
  const file = readGrinFile(filePath);
  const report = buildReport(file, sections);

  if (json) {
    process.stdout.write(`${JSON.stringify(report, null, 2)}\n`);
  } else {
    outputText(report, sections);
  }
} catch (error) {
  const message = error instanceof Error ? error.message : String(error);
  process.stderr.write(`grin-inspect: ${message}\n`);
  process.exit(1);
}

function buildReport(file, sections) {
  const header = sections.header ? formatHeader(file.header) : null;
  const rules = sections.rules ? formatRules(file.rules) : null;
  const groups = sections.groups ? formatGroups(file.pixels) : null;
  const pixels = sections.pixels ? formatPixels(file) : null;

  return {
    file: filePath,
    header,
    rules,
    groups,
    pixels,
  };
}

function formatHeader(header) {
  return {
    magic: Array.from(header.magic),
    versionMajor: header.versionMajor,
    versionMinor: header.versionMinor,
    headerSize: header.headerSize,
    width: header.width,
    height: header.height,
    tickMicros: header.tickMicros,
    ruleCount: header.ruleCount,
    opcodeSetId: header.opcodeSetId,
    flags: header.flags,
    pixelDataLength: header.pixelDataLength.toString(),
    fileLength: header.fileLength.toString(),
    pixelDataOffset: header.pixelDataOffset.toString(),
  };
}

function formatRules(rules) {
  return rules.map((rule, index) => ({
    index,
    groupMask: rule.groupMask,
    opcode: rule.opcode,
    opcodeName: opcodeName(rule.opcode),
    timing: rule.timing,
  }));
}

function formatGroups(pixels) {
  const counts = new Array(16).fill(0);
  let locked = 0;
  for (const pixel of pixels) {
    const groupId = pixel.c & CONTROL_BYTE_MASKS.GROUP_ID;
    counts[groupId] += 1;
    if ((pixel.c & CONTROL_BYTE_MASKS.LOCK) !== 0) {
      locked += 1;
    }
  }
  return { counts, locked };
}

function formatPixels(file) {
  const pixels = [];
  const width = file.header.width;
  for (let index = 0; index < file.pixels.length; index += 1) {
    const pixel = file.pixels[index];
    const x = index % width;
    const y = Math.floor(index / width);
    pixels.push({
      x,
      y,
      r: pixel.r,
      g: pixel.g,
      b: pixel.b,
      a: pixel.a,
      c: pixel.c,
      groupId: pixel.c & CONTROL_BYTE_MASKS.GROUP_ID,
      locked: (pixel.c & CONTROL_BYTE_MASKS.LOCK) !== 0,
    });
  }
  return pixels;
}

function outputText(report, sections) {
  process.stdout.write(`${report.file}\n`);

  if (sections.header && report.header) {
    const header = report.header;
    process.stdout.write("Header:\n");
    for (const [key, value] of Object.entries(header)) {
      process.stdout.write(`  ${key}: ${value}\n`);
    }
  }

  if (sections.rules && report.rules) {
    process.stdout.write("Rules:\n");
    if (report.rules.length === 0) {
      process.stdout.write("  (none)\n");
    } else {
      for (const rule of report.rules) {
        process.stdout.write(
          `  [${rule.index}] mask=0x${rule.groupMask.toString(16).padStart(4, "0")} opcode=${rule.opcode}` +
            ` (${rule.opcodeName}) timing=${rule.timing}\n`
        );
      }
    }
  }

  if (sections.groups && report.groups) {
    process.stdout.write("Groups:\n");
    report.groups.counts.forEach((count, groupId) => {
      process.stdout.write(`  ${groupId}: ${count}\n`);
    });
    process.stdout.write(`  locked: ${report.groups.locked}\n`);
  }

  if (sections.pixels && report.pixels) {
    process.stdout.write("Pixels:\n");
    for (const pixel of report.pixels) {
      process.stdout.write(
        `  (${pixel.x},${pixel.y}) rgba=(${pixel.r},${pixel.g},${pixel.b},${pixel.a}) c=${pixel.c}` +
          ` group=${pixel.groupId} locked=${pixel.locked}\n`
      );
    }
  }
}

function opcodeName(opcode) {
  const entry = Object.entries(BaseOpcodeId).find(([, value]) => value === opcode);
  return entry ? entry[0] : "UNKNOWN";
}

function printUsage() {
  process.stdout.write(
    "Usage: grin-inspect <file.grin> [--header] [--rules] [--groups] [--pixels] [--json]\n"
  );
}
