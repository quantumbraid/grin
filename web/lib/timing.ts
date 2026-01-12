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
