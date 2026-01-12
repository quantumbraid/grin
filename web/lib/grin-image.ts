import { GrinHeader } from "./grin-header";
import { GrinPixel } from "./grin-pixel";
import { GrinRule } from "./grin-rule";

export class GrinImage {
  header: GrinHeader;
  pixels: GrinPixel[];
  rules: GrinRule[];

  constructor(header: GrinHeader, pixels: GrinPixel[], rules: GrinRule[]) {
    this.header = header;
    this.pixels = pixels;
    this.rules = rules;

    const expectedPixelCount = this.header.width * this.header.height;
    if (!Number.isSafeInteger(expectedPixelCount)) {
      throw new Error("Pixel count exceeds safe integer range");
    }
    if (pixels.length !== expectedPixelCount) {
      throw new Error("Pixel array length does not match header dimensions");
    }
    if (rules.length !== this.header.ruleCount) {
      throw new Error("Rule array length does not match header ruleCount");
    }
  }

  getPixel(x: number, y: number): GrinPixel {
    const index = this.indexFor(x, y);
    const pixel = this.pixels[index];
    if (!pixel) {
      throw new Error("Pixel index out of range");
    }
    return pixel;
  }

  setPixel(x: number, y: number, pixel: GrinPixel): void {
    const index = this.indexFor(x, y);
    this.pixels[index] = pixel;
  }

  getPixelsByGroup(groupId: number): GrinPixel[] {
    return this.pixels.filter((pixel) => pixel.getGroupId() === (groupId & 0x0f));
  }

  getLockedPixels(): GrinPixel[] {
    return this.pixels.filter((pixel) => pixel.isLocked());
  }

  getUnlockedPixels(): GrinPixel[] {
    return this.pixels.filter((pixel) => !pixel.isLocked());
  }

  private indexFor(x: number, y: number): number {
    if (!Number.isInteger(x) || !Number.isInteger(y)) {
      throw new RangeError("Pixel coordinates must be integers");
    }
    if (x < 0 || y < 0 || x >= this.header.width || y >= this.header.height) {
      throw new RangeError("Pixel coordinates out of bounds");
    }
    return y * this.header.width + x;
  }
}
