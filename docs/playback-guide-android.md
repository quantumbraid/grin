# Android Playback Integration Guide

The Android library provides both a `GrinView` for drop-in playback and the
lower-level `GrinPlayer` if you want custom rendering.

## Using GrinView

```kotlin
val grinView = findViewById<GrinView>(R.id.grinView)
val file = GrinUriLoader.fromAsset(this, "samples/pulse_red.grin")
grinView.load(file)
grinView.play()
```

`GrinView` handles:

- `GrinPlayer` lifecycle
- aspect-correct drawing
- pause/resume on attach/detach

## Using GrinPlayer + GrinBitmapRenderer

```kotlin
val file = GrinFile.load(inputStream)
val player = GrinPlayer()
val renderer = GrinBitmapRenderer()

player.load(file)
player.play()

val frame = player.getCurrentFrame()
val bitmap = renderer.render(frame)
imageView.setImageBitmap(bitmap)
```

The default scheduler uses `AndroidTickScheduler`, which is driven by
`Choreographer` and honors the `TickMicros` value in the file header.

## Loading Files

Use `GrinUriLoader` helpers for common sources:

```kotlin
GrinUriLoader.fromAsset(context, "samples/minimal.grin")
GrinUriLoader.fromRawResource(context, R.raw.my_sample)
GrinUriLoader.fromFile(File("/sdcard/sample.grin"))
```
