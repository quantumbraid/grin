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
