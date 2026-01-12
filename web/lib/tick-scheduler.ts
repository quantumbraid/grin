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
export type TickCallback = (tick: number) => void;

export interface TickScheduler {
  start(): void;
  stop(): void;
  setTickCallback(cb: TickCallback): void;
  getCurrentTick(): number;
}

export class BrowserTickScheduler implements TickScheduler {
  private tickMicros: number;
  private tickCallback: TickCallback | null = null;
  private currentTick = 0;
  private accumulatorMicros = 0;
  private lastTimestamp = 0;
  private running = false;
  private requestId: number | null = null;

  constructor(tickMicros: number) {
    this.tickMicros = Math.max(1, Math.floor(tickMicros));
  }

  setTickCallback(cb: TickCallback): void {
    this.tickCallback = cb;
  }

  getCurrentTick(): number {
    return this.currentTick;
  }

  start(): void {
    if (this.running) {
      return;
    }
    this.running = true;
    this.lastTimestamp = 0;
    this.accumulatorMicros = 0;
    this.requestId = requestAnimationFrame(this.onFrame);
  }

  stop(): void {
    if (!this.running) {
      return;
    }
    this.running = false;
    if (this.requestId !== null) {
      cancelAnimationFrame(this.requestId);
      this.requestId = null;
    }
  }

  private onFrame = (timestamp: number): void => {
    if (!this.running) {
      return;
    }
    if (this.lastTimestamp === 0) {
      this.lastTimestamp = timestamp;
    }
    const deltaMicros = (timestamp - this.lastTimestamp) * 1000;
    this.lastTimestamp = timestamp;
    this.accumulatorMicros += deltaMicros;

    while (this.accumulatorMicros >= this.tickMicros) {
      this.accumulatorMicros -= this.tickMicros;
      this.currentTick = nextTick(this.currentTick);
      if (this.tickCallback) {
        this.tickCallback(this.currentTick);
      }
    }

    this.requestId = requestAnimationFrame(this.onFrame);
  };
}

export class RAFTickScheduler implements TickScheduler {
  private tickMicros: number;
  private tickCallback: TickCallback | null = null;
  private currentTick = 0;
  private accumulatorMicros = 0;
  private lastTimestamp = 0;
  private running = false;
  private requestId: number | null = null;
  private isHidden = false;

  constructor(tickMicros: number) {
    this.tickMicros = Math.max(1, Math.floor(tickMicros));
    if (typeof document !== "undefined") {
      this.isHidden = document.hidden;
      document.addEventListener("visibilitychange", () => {
        this.isHidden = document.hidden;
        if (!this.isHidden) {
          this.lastTimestamp = 0;
        }
      });
    }
  }

  setTickCallback(cb: TickCallback): void {
    this.tickCallback = cb;
  }

  getCurrentTick(): number {
    return this.currentTick;
  }

  start(): void {
    if (this.running) {
      return;
    }
    this.running = true;
    this.lastTimestamp = 0;
    this.accumulatorMicros = 0;
    this.requestId = requestAnimationFrame(this.onFrame);
  }

  stop(): void {
    if (!this.running) {
      return;
    }
    this.running = false;
    if (this.requestId !== null) {
      cancelAnimationFrame(this.requestId);
      this.requestId = null;
    }
  }

  private onFrame = (timestamp: number): void => {
    if (!this.running) {
      return;
    }
    if (this.isHidden) {
      this.requestId = requestAnimationFrame(this.onFrame);
      return;
    }
    if (this.lastTimestamp === 0) {
      this.lastTimestamp = timestamp;
    }
    const deltaMicros = (timestamp - this.lastTimestamp) * 1000;
    this.lastTimestamp = timestamp;
    this.accumulatorMicros += deltaMicros;

    while (this.accumulatorMicros >= this.tickMicros) {
      this.accumulatorMicros -= this.tickMicros;
      this.currentTick = nextTick(this.currentTick);
      if (this.tickCallback) {
        this.tickCallback(this.currentTick);
      }
    }

    this.requestId = requestAnimationFrame(this.onFrame);
  };
}

export class TestTickScheduler implements TickScheduler {
  private tickCallback: TickCallback | null = null;
  private currentTick = 0;
  private running = false;

  setTickCallback(cb: TickCallback): void {
    this.tickCallback = cb;
  }

  getCurrentTick(): number {
    return this.currentTick;
  }

  start(): void {
    this.running = true;
  }

  stop(): void {
    this.running = false;
  }

  advance(ticks = 1): void {
    if (!this.running) {
      return;
    }
    for (let i = 0; i < ticks; i += 1) {
      this.currentTick = nextTick(this.currentTick);
      if (this.tickCallback) {
        this.tickCallback(this.currentTick);
      }
    }
  }
}

function nextTick(currentTick: number): number {
  if (currentTick >= Number.MAX_SAFE_INTEGER) {
    return 0;
  }
  return currentTick + 1;
}
