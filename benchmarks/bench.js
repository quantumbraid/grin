import fs from "node:fs";
import path from "node:path";
import { performance } from "node:perf_hooks";
import { fileURLToPath } from "node:url";
import { createGrinFile, parseGrinBytes, serializeGrinFile } from "../tools/lib/grin.js";
import { validateGrinFile } from "../tools/lib/validation.js";
import { createRenderState, renderFrame, renderToTick } from "../tools/lib/render.js";

const args = process.argv.slice(2);
const writeBaseline = args.includes("--write-baseline");
const sizes = [
  { label: "1x1", width: 1, height: 1 },
  { label: "64x64", width: 64, height: 64 },
  { label: "256x256", width: 256, height: 256 },
];

const results = {};
for (const size of sizes) {
  const file = createSyntheticFile(size.width, size.height);
  const bytes = serializeGrinFile(file);

  const loadMs = measure(() => {
    parseGrinBytes(bytes);
  }, 50);

  const parsed = parseGrinBytes(bytes);
  const validateMs = measure(() => {
    validateGrinFile(parsed, "permissive");
  }, 50);

  const perTickMs = measure(() => {
    const state = createRenderState(parsed);
    for (let tick = 0; tick < 60; tick += 1) {
      renderFrame(parsed, tick, state);
    }
  }, 5);

  const renderMs = measure(() => {
    renderToTick(parsed, 60);
  }, 5);

  results[size.label] = {
    loadMs,
    validateMs,
    perTickMs,
    renderMs,
  };
}

const baselinePath = path.join(path.dirname(fileURLToPath(import.meta.url)), "baseline.json");
if (writeBaseline) {
  const baseline = {
    updatedAt: new Date().toISOString(),
    sizes: results,
  };
  fs.writeFileSync(baselinePath, `${JSON.stringify(baseline, null, 2)}\n`);
  process.stdout.write(`Baseline written to ${baselinePath}\n`);
} else {
  const baseline = safeReadBaseline(baselinePath);
  process.stdout.write(`${JSON.stringify({ baseline, results }, null, 2)}\n`);
}

function createSyntheticFile(width, height) {
  const pixels = [];
  for (let i = 0; i < width * height; i += 1) {
    const value = i % 256;
    pixels.push({ r: value, g: (value * 3) % 256, b: (value * 7) % 256, a: 255, c: 0 });
  }
  return createGrinFile({ width, height, pixels, rules: [], tickMicros: 1, opcodeSetId: 0 });
}

function measure(fn, iterations) {
  const start = performance.now();
  for (let i = 0; i < iterations; i += 1) {
    fn();
  }
  const end = performance.now();
  return (end - start) / iterations;
}

function safeReadBaseline(baselinePath) {
  try {
    const raw = fs.readFileSync(baselinePath, "utf8");
    return JSON.parse(raw);
  } catch (error) {
    return { updatedAt: null, sizes: {} };
  }
}
