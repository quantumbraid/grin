export class DisplayBuffer {
  width: number;
  height: number;
  rgbaData: Uint8ClampedArray;

  constructor(width: number, height: number) {
    if (!Number.isInteger(width) || !Number.isInteger(height) || width <= 0 || height <= 0) {
      throw new Error("DisplayBuffer dimensions must be positive integers");
    }
    this.width = width;
    this.height = height;
    this.rgbaData = new Uint8ClampedArray(width * height * 4);
  }

  clear(): void {
    this.rgbaData.fill(0);
  }

  setPixel(x: number, y: number, r: number, g: number, b: number, a: number): void {
    if (!Number.isInteger(x) || !Number.isInteger(y)) {
      throw new RangeError("Pixel coordinates must be integers");
    }
    if (x < 0 || y < 0 || x >= this.width || y >= this.height) {
      throw new RangeError("Pixel coordinates out of bounds");
    }
    const offset = (y * this.width + x) * 4;
    this.rgbaData[offset] = r & 0xff;
    this.rgbaData[offset + 1] = g & 0xff;
    this.rgbaData[offset + 2] = b & 0xff;
    this.rgbaData[offset + 3] = a & 0xff;
  }

  toImageData(): ImageData {
    if (typeof ImageData === "undefined") {
      throw new Error("ImageData is not available in this environment");
    }
    return new ImageData(this.rgbaData, this.width, this.height);
  }
}
