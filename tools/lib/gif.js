import fs from "node:fs";
import gifEncoderModule from "gif-encoder-2";

const GIFEncoder = gifEncoderModule?.default ?? gifEncoderModule;

export function writeGif(filePath, width, height, frames, options = {}) {
  const expectedLength = width * height * 4;
  const delayMs = Number.isInteger(options.delayMs) ? options.delayMs : 100;
  const repeat = Number.isInteger(options.repeat) ? options.repeat : 0;
  const quality = Number.isInteger(options.quality) ? options.quality : 10;

  for (const frame of frames) {
    if (frame.length !== expectedLength) {
      throw new Error("RGBA buffer length does not match width * height * 4");
    }
  }

  const encoder = new GIFEncoder(width, height);
  encoder.setRepeat(repeat);
  encoder.setDelay(delayMs);
  encoder.setQuality(quality);
  encoder.start();

  for (const frame of frames) {
    encoder.addFrame(frame);
  }

  encoder.finish();
  const buffer = encoder.out.getData();
  fs.writeFileSync(filePath, buffer);
}
