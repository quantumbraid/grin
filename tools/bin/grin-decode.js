#!/usr/bin/env node
import path from "node:path";
import { readGrinFile } from "../lib/grin.js";
import { createRenderState, renderFrame, renderGroupMap, renderToTick } from "../lib/render.js";
import { writeGif } from "../lib/gif.js";
import { writeImage } from "../lib/png.js";

const args = process.argv.slice(2);
let inputPath = null;
let outputPath = null;
let frame = 0;
let groups = false;
let gif = false;
let frames = null;
let fps = null;

for (let i = 0; i < args.length; i += 1) {
  const arg = args[i];
  if (arg === "--frame") {
    frame = Number.parseInt(args[i + 1] ?? "0", 10);
    i += 1;
  } else if (arg === "--groups") {
    groups = true;
  } else if (arg === "--gif") {
    gif = true;
  } else if (arg === "--frames") {
    frames = Number.parseInt(args[i + 1] ?? "0", 10);
    i += 1;
  } else if (arg === "--fps") {
    fps = Number.parseInt(args[i + 1] ?? "0", 10);
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
  const file = readGrinFile(inputPath);
  const width = file.header.width;
  const height = file.header.height;
  const outputExt = path.extname(outputPath).toLowerCase();
  const isGif = gif || outputExt === ".gif";

  if (isGif && groups) {
    throw new Error("Cannot combine --groups with GIF output");
  }

  if (isGif) {
    const frameCount = Number.isInteger(frames) && frames > 0 ? frames : 60;
    const rate = Number.isInteger(fps) && fps > 0 ? fps : 30;
    const delayMs = Math.max(1, Math.round(1000 / rate));
    const state = createRenderState(file);
    const sequence = [];
    for (let i = 0; i < frameCount; i += 1) {
      sequence.push(renderFrame(file, frame + i, state));
    }
    writeGif(outputPath, width, height, sequence, { delayMs });
    process.stdout.write(`Wrote ${outputPath} (${frameCount} frames @ ${rate}fps)\n`);
    process.exit(0);
  }

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
    "Usage: grin-decode <input.grin> <output.png> [--frame <tick>] [--groups]\n" +
      "       grin-decode <input.grin> <output.gif> [--gif] [--frame <tick>] [--frames <count>] [--fps <rate>]\n"
  );
}
