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

The `{aa[bbbbbbbbbbbbbbbb|cccc|ddd:eee]}` sketch clarifies the mapping: `a` is the lane number, `b` holds the concatenated `G`–`X` labels, `c` is the action (`sety`, `setz`, or a two-digit hex delta), `d` is the repeat count (`000` makes the lane a one-time event), and `eee` is the time unit (`min`,`sec`, or `mil`). Every lane number and delta amount consumes exactly two digits so the header stays canonical and, even with 16 fully populated lanes, remains under the 592-byte budget.

### Action Details

`+`/`-` actions use base-16 counting, not color theory. Examples:

- `+03RR` increments red by 0x03 (with carry to green when applicable).
- `-0aGG` decrements green by 0x0A.
- `+ffAA` raises alpha to max; alpha clamps and does not carry.

Hex math follows standard hexadecimal addition: `0x03 + 0x0ff = 0x103`, `0x003 + 0x00f = 0x013`, and adding `0xff` to `0x003` produces `0x103` because each digit spans 16 values. When RGB deltas overflow a channel, the carry moves to the next channel (`+ffRR` applied to `ff0003` becomes `ff0132`), and subtraction borrows from the next channel (`ff0003 - ff` gives `feff34`). Alpha never carries or borrows; it clamps between `00` and `ff`.

Alpha clamps at `00`/`ff` without wrap. RGB channels carry to the next channel
when incrementing and borrow from the next channel when decrementing.

### Pixel Control Suffix

Pixels are addressed as `rrggbbaa` plus a control suffix composed of a `G`–`X` group label and a `Y`/`Z` lock marker. For instance, `ff00eeffjy` targets group `J` and keeps the pixel unlocked (`y`). Switching the suffix to `z` would lock the same pixel. Tooling should rewrite any other suffix so the group letter is within `G`–`X` and the lock character is `Y` (unlocked) or `Z` (locked).

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
