import {
  CONTROL_BYTE_MASKS,
  getGroupId,
  isLocked,
  setGroupId,
  setLocked,
} from "./format";

export class GrinPixel {
  r: number;
  g: number;
  b: number;
  a: number;
  c: number;

  constructor(r: number, g: number, b: number, a: number, c: number) {
    this.r = r & 0xff;
    this.g = g & 0xff;
    this.b = b & 0xff;
    this.a = a & 0xff;
    this.c = c & 0xff;
  }

  static fromBytes(bytes: Uint8Array): GrinPixel {
    if (bytes.length !== 5) {
      throw new Error("GrinPixel requires exactly 5 bytes");
    }
    return new GrinPixel(bytes[0] ?? 0, bytes[1] ?? 0, bytes[2] ?? 0, bytes[3] ?? 0, bytes[4] ?? 0);
  }

  getGroupId(): number {
    return getGroupId(this.c);
  }

  isLocked(): boolean {
    return isLocked(this.c);
  }

  setGroupId(groupId: number): void {
    this.c = setGroupId(this.c, groupId) & 0xff;
  }

  setLocked(locked: boolean): void {
    this.c = setLocked(this.c, locked) & 0xff;
  }

  toBytes(): Uint8Array {
    return new Uint8Array([this.r, this.g, this.b, this.a, this.c]);
  }

  reservedBits(): number {
    return this.c & CONTROL_BYTE_MASKS.RESERVED;
  }
}
