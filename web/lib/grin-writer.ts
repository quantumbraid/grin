import { HEADER_SIZE_BYTES, PIXEL_SIZE_BYTES } from "./format";
import { GrinFile } from "./grin-file";
import { GrinHeader } from "./grin-header";
import { writePixelData, writeRulesBlock } from "./grin-io";

export class GrinWriter {
  static writeHeader(header: GrinHeader): Uint8Array {
    return header.serialize();
  }

  static writeFile(file: GrinFile): Uint8Array {
    const rulesBlock = writeRulesBlock(file.rules, file.rules.length);
    const pixelBytes = writePixelData(file.pixels);
    const pixelDataLength = BigInt(pixelBytes.length);

    const header = file.header.clone({
      ruleCount: file.rules.length,
      rulesBlock,
      pixelDataLength,
      pixelDataOffset: BigInt(HEADER_SIZE_BYTES),
      fileLength: BigInt(HEADER_SIZE_BYTES + pixelBytes.length),
    });

    const output = new Uint8Array(HEADER_SIZE_BYTES + pixelBytes.length);
    output.set(header.serialize(), 0);
    output.set(pixelBytes, HEADER_SIZE_BYTES);
    return output;
  }

  static writePixels(pixels: number[]): Uint8Array {
    if (pixels.length % PIXEL_SIZE_BYTES !== 0) {
      throw new Error("Pixel array length must be a multiple of 5");
    }
    return new Uint8Array(pixels);
  }
}
