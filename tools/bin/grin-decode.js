#!/usr/bin/env node
import { readGrinFile } from "../lib/grin.js";
import { renderGroupMap, renderToTick } from "../lib/render.js";
import { writeImage } from "../lib/png.js";

const args = process.argv.slice(2);
let inputPath = null;
let outputPath = null;
let frame = 0;
let groups = false;

for (let i = 0; i < args.length; i += 1) {
  const arg = args[i];
  if (arg === "--frame") {
    frame = Number.parseInt(args[i + 1] ?? "0", 10);
    i += 1;
  } else if (arg === "--groups") {
    groups = true;
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
  const file = readGrinFile(inputPath);
  const width = file.header.width;
  const height = file.header.height;
  const rgba = groups ? renderGroupMap(file) : renderToTick(file, frame);
  writeImage(outputPath, width, height, rgba);
  process.stdout.write(`Wrote ${outputPath}\n`);
} catch (error) {
  const message = error instanceof Error ? error.message : String(error);
  process.stderr.write(`grin-decode: ${message}\n`);
  process.exit(1);
}

function printUsage() {
  process.stdout.write(
    "Usage: grin-decode <input.grin> <output.png> [--frame <tick>] [--groups]\n"
  );
}
