# GRIN Security Audit Checklist

Use this checklist to review GRIN readers, writers, and playback pipelines.

## File Parsing

- [ ] Magic bytes validated before any other parsing.
- [ ] Header size validated as 128 bytes.
- [ ] RuleCount limited to 0-16.
- [ ] PixelDataOffset64 validated as 128.
- [ ] PixelDataLength == Width * Height * 5.
- [ ] FileLength is 0 or >= 128 + PixelDataLength.
- [ ] Reserved header fields are zeroed on write.
- [ ] Reserved header fields are ignored on read (warn if non-zero).
- [ ] Control byte reserved bits (4-6) are zeroed on write.
- [ ] Control byte reserved bits are warned on read if non-zero.

## Resource Bounds

- [ ] Width/Height are non-negative and within implementation limits.
- [ ] Pixel count and pixel data length checked for overflow.
- [ ] Buffer lengths verified before allocation.

## Opcode and Rule Handling

- [ ] OpcodeSetId is recognized.
- [ ] Unknown opcodes are rejected.
- [ ] Rules block is fixed at 64 bytes.
- [ ] Unused rule entries are zero on write.
- [ ] Rules are applied only to matching group IDs.

## Playback

- [ ] Source pixels are never mutated.
- [ ] Locked pixels bypass rule application.
- [ ] Per-tick processing is O(1) per pixel and stateless.
