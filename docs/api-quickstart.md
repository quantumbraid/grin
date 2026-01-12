# GRIN API Quick Start

This guide focuses on the current web, Android, and CLI implementations.

## CLI (Node >= 18)

```bash
node tools/bin/grin-validate.js samples/minimal.grin
node tools/bin/grin-inspect.js samples/pulse_red.grin --header --rules
node tools/bin/grin-encode.js input.png output.grin
node tools/bin/grin-decode.js samples/pulse_red.grin output.png --frame 0
```

## Web / TypeScript

Build the web package once so `web/dist` is available:

```bash
cd web
npm install
npm run build
```

Canvas playback with the core player:

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

const schedulerFactory = (tickMicros) => new RAFTickScheduler(tickMicros);

const file = await GrinLoader.fromURL("./samples/pulse_red.grin");
player = new GrinPlayer(schedulerFactory, new RuleEngine(), OpcodeRegistry.getInstance(), () => {
  renderer.render(player.getCurrentFrame(), canvas);
});
player.load(file);
player.play();
```

## Web Component

```html
<grin-player src="./samples/pulse_red.grin" autoplay loop playbackrate="1.5"></grin-player>
```

The custom element exposes `play()`, `pause()`, `currentTime`, and `getCurrentFrame()`.

## Android

Use `GrinView` for drop-in playback:

```kotlin
val grinView = findViewById<GrinView>(R.id.grinView)
val file = GrinUriLoader.fromAsset(this, "samples/minimal.grin")
grinView.load(file)
grinView.play()
```

Or manually manage playback:

```kotlin
val file = GrinFile.load(inputStream)
val player = GrinPlayer()
val renderer = GrinBitmapRenderer()
player.load(file)
player.play()

val frame = player.getCurrentFrame()
val bitmap = renderer.render(frame)
```
