# Header Lane Language (Authoring Standard)

GRIN authoring uses a compact header lane language to describe rule intent in a
fixed-size, deterministic string. Each lane represents a single script segment
(target + action + repeat cadence) and must be sequentially numbered.

This language is **authoring-only**; it is designed for tooling and review, and
can be normalized or validated before producing the binary rule block.

## Lane Grammar

```
{NN[groups|action|xxx:unit]}
```

- `NN` is the lane number (`00`–`15`) and must be sequential.
- `groups` is a concatenated list of group labels (`G`–`X`, case-insensitive).
  Use the 16 GRIN labels: `G H J K L M N P Q R S T U V W X`.
- `action` is one of:
  - `sety` (unlock)
  - `setz` (lock)
  - `+<hh><CC>` or `-<hh><CC>` (increment/decrement a channel by hex steps)
- `xxx:unit` is the repeat cadence:
  - `xxx` is `000`–`999` (`000` means no repeat)
  - `unit` is `min`, `sec`, or `mil`

### Action Details

`+`/`-` actions use base-16 counting, not color theory. Examples:

- `+03RR` increments red by 0x03 (with carry to green when applicable).
- `-0aGG` decrements green by 0x0A.
- `+ffAA` raises alpha to max; alpha clamps and does not carry.

Alpha clamps at `00`/`ff` without wrap. RGB channels carry to the next channel
when incrementing and borrow from the next channel when decrementing.

## Examples

Unlock all pixels (targets every group once):

```
{01[ghijklmnopqrstuvwx|sety|000:mil]}
```

Shift groups J, L, R by +3 on red every 500 milliseconds:

```
{02[jlr|+03rr|500:mil]}
```

## Limits

- Maximum of 16 lanes.
- Lane numbers must be sequential.
- The full header string is limited to 592 bytes.

Tooling should reject headers that violate these limits.
