import { RULE_ENTRY_SIZE, groupMaskTargetsGroup } from "./format";

export class GrinRule {
  groupMask: number;
  opcode: number;
  timing: number;

  constructor(groupMask: number, opcode: number, timing: number) {
    this.groupMask = groupMask & 0xffff;
    this.opcode = opcode & 0xff;
    this.timing = timing & 0xff;
  }

  targetsGroup(groupId: number): boolean {
    return groupMaskTargetsGroup(this.groupMask, groupId);
  }

  serialize(): Uint8Array {
    const buffer = new ArrayBuffer(RULE_ENTRY_SIZE);
    const view = new DataView(buffer);
    view.setUint16(0, this.groupMask, true);
    view.setUint8(2, this.opcode);
    view.setUint8(3, this.timing);
    return new Uint8Array(buffer);
  }

  static deserialize(bytes: Uint8Array): GrinRule {
    if (bytes.length !== RULE_ENTRY_SIZE) {
      throw new Error("GrinRule requires exactly 4 bytes");
    }
    const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
    const groupMask = view.getUint16(0, true);
    const opcode = view.getUint8(2);
    const timing = view.getUint8(3);
    return new GrinRule(groupMask, opcode, timing);
  }
}
