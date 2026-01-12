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
export type WaveformType = "square" | "triangle" | "sine" | "sawtooth";

export const TimingInterpreter = {
  getPeriod(timing: number): number {
    return (timing & 0x0f) + 1;
  },

  getWaveform(timing: number): WaveformType {
    const waveform = (timing >> 4) & 0x03;
    switch (waveform) {
      case 0:
        return "square";
      case 1:
        return "triangle";
      case 2:
        return "sine";
      default:
        return "sawtooth";
    }
  },

  getPhaseOffset(timing: number): number {
    return (timing >> 6) & 0x03;
  },

  evaluate(timing: number, tick: number): number {
    const period = this.getPeriod(timing);
    const phaseOffset = this.getPhaseOffset(timing) / 4;
    const position = ((tick / period) + phaseOffset) % 1;
    const waveform = this.getWaveform(timing);
    switch (waveform) {
      case "square":
        return position < 0.5 ? 0 : 1;
      case "triangle":
        return position < 0.5 ? position * 2 : 2 - position * 2;
      case "sine":
        return 0.5 - 0.5 * Math.cos(2 * Math.PI * position);
      case "sawtooth":
        return position;
    }
  },
} as const;
