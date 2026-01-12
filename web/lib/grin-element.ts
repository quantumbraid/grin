import { GrinFile } from "./grin-file";
import { GrinCanvasRenderer } from "./grin-canvas";
import { GrinLoader } from "./grin-loader";
import { GrinPlayer } from "./grin-player";
import { OpcodeRegistry } from "./opcode-registry";
import { RAFTickScheduler, TickScheduler } from "./tick-scheduler";
import { RuleEngine } from "./rule-engine";

export class GrinPlayerElement extends HTMLElement {
  static get observedAttributes(): string[] {
    return ["src", "autoplay", "loop", "playbackrate"];
  }

  private canvas: HTMLCanvasElement;
  private renderer: GrinCanvasRenderer;
  private player: GrinPlayer | null = null;
  private file: GrinFile | null = null;
  private autoplay = false;
  private loop = false;
  private playbackRate = 1;

  constructor() {
    super();
    const shadow = this.attachShadow({ mode: "open" });
    const container = document.createElement("div");
    container.className = "grin-player";
    this.canvas = document.createElement("canvas");
    this.canvas.width = 1;
    this.canvas.height = 1;
    container.appendChild(this.canvas);

    const style = document.createElement("style");
    style.textContent = `
      :host {
        display: block;
        font-family: "Space Grotesk", system-ui, sans-serif;
        color: #101010;
      }
      .grin-player {
        position: relative;
        border-radius: 16px;
        overflow: hidden;
        background: radial-gradient(circle at 20% 20%, #f2f5ff, #e7f6ef 55%, #fef3e3 100%);
        border: 1px solid rgba(16, 16, 16, 0.08);
        box-shadow: 0 12px 32px rgba(16, 16, 16, 0.12);
        padding: 12px;
      }
      canvas {
        display: block;
        width: 100%;
        height: auto;
        border-radius: 12px;
        background: #000;
      }
    `;

    shadow.appendChild(style);
    shadow.appendChild(container);

    this.renderer = new GrinCanvasRenderer();
  }

  connectedCallback(): void {
    this.autoplay = this.hasAttribute("autoplay");
    this.loop = this.hasAttribute("loop");
    this.playbackRate = this.parsePlaybackRate(this.getAttribute("playbackrate"));

    const src = this.getAttribute("src");
    if (src) {
      void this.loadFromUrl(src);
    }
  }

  disconnectedCallback(): void {
    this.player?.pause();
  }

  attributeChangedCallback(name: string, _oldValue: string | null, newValue: string | null): void {
    switch (name) {
      case "src":
        if (newValue) {
          void this.loadFromUrl(newValue);
        }
        break;
      case "autoplay":
        this.autoplay = newValue !== null;
        if (this.autoplay) {
          this.player?.play();
        }
        break;
      case "loop":
        this.loop = newValue !== null;
        break;
      case "playbackrate":
        this.playbackRate = this.parsePlaybackRate(newValue);
        if (this.file) {
          this.loadFile(this.file);
        }
        break;
    }
  }

  play(): void {
    this.player?.play();
  }

  pause(): void {
    this.player?.pause();
  }

  get currentTime(): number {
    return this.player?.getCurrentTick() ?? 0;
  }

  set currentTime(tick: number) {
    if (!this.player) {
      return;
    }
    if (!Number.isInteger(tick) || tick < 0) {
      return;
    }
    this.player.seek(tick);
  }

  getCurrentFrame(): ImageData | null {
    if (!this.player) {
      return null;
    }
    const buffer = this.player.getCurrentFrame();
    return new ImageData(buffer.rgbaData, buffer.width, buffer.height);
  }

  private async loadFromUrl(url: string): Promise<void> {
    const file = await GrinLoader.fromURL(url);
    this.loadFile(file);
  }

  private loadFile(file: GrinFile): void {
    this.file = file;
    this.canvas.width = file.header.width;
    this.canvas.height = file.header.height;
    const schedulerFactory = (tickMicros: number): TickScheduler => {
      const scaled = Math.max(1, Math.floor(tickMicros / this.playbackRate));
      return new RAFTickScheduler(scaled);
    };
    this.player = new GrinPlayer(
      schedulerFactory,
      new RuleEngine(),
      OpcodeRegistry.getInstance(),
      () => this.render()
    );
    this.player.load(file);
    this.render();
    if (this.autoplay) {
      this.player.play();
    }
  }

  private render(): void {
    if (!this.player) {
      return;
    }
    const frame = this.player.getCurrentFrame();
    this.renderer.render(frame, this.canvas);
  }

  private parsePlaybackRate(value: string | null): number {
    if (!value) {
      return 1;
    }
    const parsed = Number(value);
    if (!Number.isFinite(parsed) || parsed <= 0) {
      return 1;
    }
    return parsed;
  }
}

if (typeof window !== "undefined" && !customElements.get("grin-player")) {
  customElements.define("grin-player", GrinPlayerElement);
}
