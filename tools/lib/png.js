import fs from "node:fs";
import path from "node:path";
import { PNG } from "pngjs";
import jpeg from "jpeg-js";

export function readImage(filePath) {
  const buffer = fs.readFileSync(filePath);
  const ext = path.extname(filePath).toLowerCase();
  if (ext === ".png") {
    const png = PNG.sync.read(buffer);
    return { width: png.width, height: png.height, data: new Uint8Array(png.data) };
  }
  if (ext === ".jpg" || ext === ".jpeg") {
    const decoded = jpeg.decode(buffer, { useTArray: true });
    return { width: decoded.width, height: decoded.height, data: decoded.data };
  }
  throw new Error("Unsupported image format (expected .png or .jpg)");
}

export function writeImage(filePath, width, height, data, options = {}) {
  const ext = path.extname(filePath).toLowerCase();
  const expectedLength = width * height * 4;
  if (data.length !== expectedLength) {
    throw new Error("RGBA buffer length does not match width * height * 4");
  }

  if (ext === ".png") {
    const png = new PNG({ width, height });
    png.data = Buffer.from(data);
    const buffer = PNG.sync.write(png);
    fs.writeFileSync(filePath, buffer);
    return;
  }

  if (ext === ".jpg" || ext === ".jpeg") {
    const quality = Number.isInteger(options.quality) ? options.quality : 90;
    const encoded = jpeg.encode({ data: Buffer.from(data), width, height }, quality);
    fs.writeFileSync(filePath, encoded.data);
    return;
  }

  throw new Error("Unsupported output format (expected .png or .jpg)");
}
