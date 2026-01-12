import { CONTROL_BYTE_MASKS } from "./format";
import { GrinFile } from "./grin-file";
import { GrinImage } from "./grin-image";
import { GrinPixel } from "./grin-pixel";
import { DisplayBuffer } from "./display-buffer";
import { OpcodeRegistry } from "./opcode-registry";
import { PlaybackState } from "./playback-state";
import { RuleEngine } from "./rule-engine";
import { RAFTickScheduler, TickScheduler } from "./tick-scheduler";

export class GrinPlayer {
  private image: GrinImage | null = null;
  private scheduler: TickScheduler | null = null;
  private readonly schedulerFactory: (tickMicros: number) => TickScheduler;
  private readonly ruleEngine: RuleEngine;
  private readonly registry: OpcodeRegistry;
  private controlBytes = new Uint8Array(0);
  private state: PlaybackState | null = null;
  private displayBuffer: DisplayBuffer | null = null;
  private readonly onFrameRendered: (() => void) | null;

  constructor(
    schedulerFactory: (tickMicros: number) => TickScheduler = (tickMicros) =>
      new RAFTickScheduler(tickMicros),
    ruleEngine: RuleEngine = new RuleEngine(),
    registry: OpcodeRegistry = OpcodeRegistry.getInstance(),
    onFrameRendered: (() => void) | null = null
  ) {
    this.schedulerFactory = schedulerFactory;
    this.ruleEngine = ruleEngine;
    this.registry = registry;
    this.onFrameRendered = onFrameRendered;
  }

  load(file: GrinFile): void {
    this.image = new GrinImage(file.header, file.pixels, file.rules);
    this.displayBuffer = new DisplayBuffer(this.image.header.width, this.image.header.height);
    this.state = new PlaybackState(this.displayBuffer);
    this.controlBytes = new Uint8Array(this.image.pixels.length);
    this.image.pixels.forEach((pixel, index) => {
      this.controlBytes[index] = pixel.c & ~CONTROL_BYTE_MASKS.RESERVED;
    });

    if (this.scheduler) {
      this.scheduler.stop();
    }
    this.scheduler = this.schedulerFactory(this.image.header.tickMicros);
    this.scheduler.setTickCallback((tick) => this.onTick(tick));
  }

  play(): void {
    if (!this.state || !this.scheduler) {
      throw new Error("GrinPlayer.load() must be called before play()");
    }
    if (this.state.isPlaying) {
      return;
    }
    this.state.isPlaying = true;
    this.scheduler.start();
  }

  pause(): void {
    if (!this.state || !this.scheduler) {
      return;
    }
    this.state.isPlaying = false;
    this.scheduler.stop();
  }

  stop(): void {
    if (!this.state) {
      return;
    }
    this.pause();
    this.state.reset();
  }

  seek(tick: number): void {
    if (!this.state || !this.image) {
      throw new Error("GrinPlayer.load() must be called before seek()");
    }
    if (!Number.isInteger(tick) || tick < 0) {
      throw new RangeError("Tick must be a non-negative integer");
    }
    this.state.currentTick = tick;
    this.renderFrame(tick);
  }

  getCurrentFrame(): DisplayBuffer {
    if (!this.displayBuffer) {
      throw new Error("GrinPlayer.load() must be called before getCurrentFrame()");
    }
    return this.displayBuffer;
  }

  isPlaying(): boolean {
    return this.state?.isPlaying ?? false;
  }

  getCurrentTick(): number {
    return this.state?.currentTick ?? 0;
  }

  private onTick(tick: number): void {
    if (!this.state) {
      return;
    }
    this.state.currentTick = tick;
    this.renderFrame(tick);
  }

  private renderFrame(tick: number): void {
    if (!this.image || !this.displayBuffer) {
      return;
    }
    const activeRules = this.ruleEngine.evaluateRules(this.image, tick);
    const output = this.displayBuffer.rgbaData;
    const opcodeSetId = this.image.header.opcodeSetId;

    for (let i = 0; i < this.image.pixels.length; i += 1) {
      const source = this.image.pixels[i];
      const control = this.controlBytes[i] ?? source.c;
      const outputIndex = i * 4;

      if ((control & CONTROL_BYTE_MASKS.LOCK) !== 0) {
        output[outputIndex] = source.r;
        output[outputIndex + 1] = source.g;
        output[outputIndex + 2] = source.b;
        output[outputIndex + 3] = source.a;
        continue;
      }

      const working = new GrinPixel(source.r, source.g, source.b, source.a, control);
      const groupId = control & CONTROL_BYTE_MASKS.GROUP_ID;

      for (const activeRule of activeRules) {
        if (activeRule.rule.targetsGroup(groupId)) {
          const opcode = this.registry.getOpcode(opcodeSetId, activeRule.rule.opcode);
          opcode.apply(working, tick, activeRule.rule.timing);
        }
      }

      this.controlBytes[i] = working.c & ~CONTROL_BYTE_MASKS.RESERVED;
      output[outputIndex] = working.r;
      output[outputIndex + 1] = working.g;
      output[outputIndex + 2] = working.b;
      output[outputIndex + 3] = working.a;
    }
    if (this.onFrameRendered) {
      this.onFrameRendered();
    }
  }
}
