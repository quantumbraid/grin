#!/usr/bin/env node
import { readGrinFile } from "../lib/grin.js";
import { validateGrinFile } from "../lib/validation.js";

const args = process.argv.slice(2);
const strict = args.includes("--strict");
const json = args.includes("--json");
const files = args.filter((arg) => !arg.startsWith("--"));

if (files.length === 0) {
  printUsage();
  process.exit(1);
}

const results = [];
let exitCode = 0;

for (const filePath of files) {
  try {
    const file = readGrinFile(filePath);
    const report = validateGrinFile(file, strict ? "strict" : "permissive");
    const result = { file: filePath, ...report };
    results.push(result);
    if (!report.ok) {
      exitCode = 1;
    }
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    const result = { file: filePath, ok: false, errors: [message], warnings: [], info: [] };
    results.push(result);
    exitCode = 1;
  }
}

if (json) {
  process.stdout.write(`${JSON.stringify(results, null, 2)}\n`);
} else {
  for (const result of results) {
    const status = result.ok ? "OK" : "FAIL";
    process.stdout.write(`${result.file}: ${status}\n`);
    for (const info of result.info) {
      process.stdout.write(`  info: ${info}\n`);
    }
    for (const warning of result.warnings) {
      process.stdout.write(`  warning: ${warning}\n`);
    }
    for (const error of result.errors) {
      process.stdout.write(`  error: ${error}\n`);
    }
  }
}

process.exit(exitCode);

function printUsage() {
  process.stdout.write("Usage: grin-validate <file.grin> [--strict] [--json]\n");
}
