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
