export const TimingInterpreter = {
  getPeriod(timing) {
    return (timing & 0x0f) + 1;
  },

  getWaveform(timing) {
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

  getPhaseOffset(timing) {
    return (timing >> 6) & 0x03;
  },

  evaluate(timing, tick) {
    const period = this.getPeriod(timing);
    const phaseOffset = this.getPhaseOffset(timing) / 4;
    const position = ((tick / period) + phaseOffset) % 1;
    switch (this.getWaveform(timing)) {
      case "square":
        return position < 0.5 ? 0 : 1;
      case "triangle":
        return position < 0.5 ? position * 2 : 2 - position * 2;
      case "sine":
        return 0.5 - 0.5 * Math.cos(2 * Math.PI * position);
      case "sawtooth":
        return position;
      default:
        return 0;
    }
  },
};
