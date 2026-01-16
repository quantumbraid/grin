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
import {
  formatLaneHeader,
  normalizeLaneSpecs,
  parseLaneHeader,
} from "../lib/lane-header.js";

const args = process.argv.slice(2);
let inputPath = null;
let fromJsonPath = null;
let outputJson = false;
let outputNormalize = false;

// Basic CLI argument parsing for human-friendly usage.
for (let i = 0; i < args.length; i += 1) {
  const arg = args[i];
  if (arg === "--from-json") {
    fromJsonPath = args[i + 1];
    i += 1;
  } else if (arg === "--json") {
    outputJson = true;
  } else if (arg === "--normalize") {
    outputNormalize = true;
  } else if (arg === "--help" || arg === "-h") {
    printUsage();
    process.exit(0);
  } else if (!arg.startsWith("--") && !inputPath) {
    inputPath = arg;
  }
}

try {
  if (fromJsonPath) {
    const lanes = loadLaneJson(fromJsonPath);
    const header = formatLaneHeader(lanes);
    // Emit normalized header strings for authoring pipelines.
    process.stdout.write(`${header}\n`);
  } else if (inputPath) {
    const header = fs.readFileSync(inputPath, "utf8");
    const lanes = parseLaneHeader(header);
    // Always allow normalized output for review workflows.
    if (outputNormalize || !outputJson) {
      const normalized = formatLaneHeader(lanes);
      process.stdout.write(`${normalized}\n`);
    }
    if (outputJson) {
      process.stdout.write(`${JSON.stringify({ lanes }, null, 2)}\n`);
    }
  } else {
    printUsage();
    process.exit(1);
  }
} catch (error) {
  const message = error instanceof Error ? error.message : String(error);
  process.stderr.write(`grin-lanes: ${message}\n`);
  process.exit(1);
}

function loadLaneJson(path) {
  const raw = fs.readFileSync(path, "utf8");
  const parsed = JSON.parse(raw);
  const lanes = Array.isArray(parsed) ? parsed : parsed.lanes;
  return normalizeLaneSpecs(lanes);
}

function printUsage() {
  process.stdout.write(
    "Usage: grin-lanes <lanes.txt> [--normalize] [--json]\n" +
      "       grin-lanes --from-json <lanes.json>\n"
  );
}
