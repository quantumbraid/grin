import { DisplayBuffer } from "./display-buffer";

export class PlaybackState {
  currentTick: number;
  isPlaying: boolean;
  tickAccumulatorMicros: number;
  displayBuffer: DisplayBuffer;

  constructor(displayBuffer: DisplayBuffer) {
    this.displayBuffer = displayBuffer;
    this.currentTick = 0;
    this.isPlaying = false;
    this.tickAccumulatorMicros = 0;
  }

  reset(): void {
    this.currentTick = 0;
    this.tickAccumulatorMicros = 0;
    this.isPlaying = false;
  }
}
