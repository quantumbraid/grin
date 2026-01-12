# Web Playback Integration Guide

This guide covers the core player and the `<grin-player>` custom element.

## Canvas Playback (GrinPlayer)

```ts
import {
  GrinCanvasRenderer,
  GrinLoader,
  GrinPlayer,
  OpcodeRegistry,
  RAFTickScheduler,
  RuleEngine,
} from "./dist/grin.js";

const canvas = document.querySelector("canvas");
const renderer = new GrinCanvasRenderer();
let player = null;

const file = await GrinLoader.fromURL("./samples/pulse_red.grin");
player = new GrinPlayer(
  (tickMicros) => new RAFTickScheduler(tickMicros),
  new RuleEngine(),
  OpcodeRegistry.getInstance(),
  () => renderer.render(player.getCurrentFrame(), canvas)
);

player.load(file);
player.play();
```

Notes:

- `GrinPlayer` expects a `GrinFile` and renders into a `DisplayBuffer`.
- Locked pixels are skipped; rule application is per-group per tick.
- `RAFTickScheduler` uses `requestAnimationFrame` and honors `TickMicros`.

## Web Component (`<grin-player>`)

```html
<grin-player src="./samples/pulse_red.grin" autoplay playbackrate="1.5"></grin-player>
```

Attributes:

- `src`: URL to a `.grin` file
- `autoplay`: start playback after load
- `playbackrate`: scales tick duration (default 1)

API:

- `play()`, `pause()`
- `currentTime` (tick)
- `getCurrentFrame()` (ImageData)
