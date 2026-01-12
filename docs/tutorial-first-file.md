# Tutorial: Creating Your First GRIN File

This tutorial walks through creating a simple GRIN file using the CLI tools.

## 1) Prepare an Image

Start with any PNG. A 16x16 or 32x32 image is enough for testing.

## 2) (Optional) Create a Rules File

Create `rules.json` to add a simple pulse:

```json
{
  "tickMicros": 16666,
  "opcodeSetId": 0,
  "rules": [
    { "groupMask": 1, "opcode": "0x03", "timing": "0x27" }
  ]
}
```

This targets group 0 and pulses alpha using a sine wave.

## 3) Encode the GRIN File

```bash
node tools/bin/grin-encode.js input.png output.grin --rules rules.json
```

If you want group control, also provide a group mask image:

```bash
node tools/bin/grin-encode.js input.png output.grin --groups group-mask.png --rules rules.json
```

## 4) Inspect the Result

```bash
node tools/bin/grin-inspect.js output.grin --header --rules --groups
```

## 5) Render a Frame

```bash
node tools/bin/grin-decode.js output.grin output.png --frame 0
```

## 6) Compare with Reference Samples

See `samples/README.md` for reference files such as `samples/pulse_red.grin`
and `samples/minimal.grin`.
