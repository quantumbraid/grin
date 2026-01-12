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
import path from "node:path";
import { fileURLToPath } from "node:url";
import { createGrinFile, serializeGrinFile } from "../tools/lib/grin.js";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const fixturesPath = path.join(root, "tests", "fixtures", "grin-fixtures.json");
const outputDir = path.join(root, "tests", "fixtures", "generated");

const fixtures = JSON.parse(fs.readFileSync(fixturesPath, "utf8"));

fs.mkdirSync(outputDir, { recursive: true });

for (const [name, fixture] of Object.entries(fixtures)) {
  const pixels = fixture.pixels.map((pixel) => ({
    r: pixel.r,
    g: pixel.g,
    b: pixel.b,
    a: pixel.a,
    c: pixel.c,
  }));
  const file = createGrinFile({
    width: fixture.width,
    height: fixture.height,
    pixels,
    rules: fixture.rules ?? [],
    tickMicros: fixture.tickMicros ?? 0,
    opcodeSetId: fixture.opcodeSetId ?? 0,
  });
  const bytes = serializeGrinFile(file);
  const outputPath = path.join(outputDir, `${name}.grin`);
  fs.writeFileSync(outputPath, bytes);
}

process.stdout.write(`Generated ${Object.keys(fixtures).length} fixture(s) in ${outputDir}\n`);
