import { DisplayBuffer } from "./display-buffer";

export class GrinCanvasRenderer {
  private imageData: ImageData | null = null;

  render(displayBuffer: DisplayBuffer, canvas: HTMLCanvasElement | OffscreenCanvas): void {
    const width = displayBuffer.width;
    const height = displayBuffer.height;
    const context = canvas.getContext("2d");
    if (!context) {
      throw new Error("Unable to obtain 2D context");
    }
    if (!this.imageData || this.imageData.width !== width || this.imageData.height !== height) {
      this.imageData = new ImageData(width, height);
    }
    this.imageData.data.set(displayBuffer.rgbaData);
    context.putImageData(this.imageData, 0, 0);
  }
}
