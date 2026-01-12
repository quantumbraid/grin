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
import { GrinImage } from "./grin-image";
import { GrinRule } from "./grin-rule";
import { TimingInterpreter } from "./timing";

export type ActiveRule = {
  index: number;
  rule: GrinRule;
};

const ACTIVE_THRESHOLD = 0.5;

export class RuleEngine {
  evaluateRules(image: GrinImage, tick: number): ActiveRule[] {
    const active: ActiveRule[] = [];
    image.rules.forEach((rule, index) => {
      const level = TimingInterpreter.evaluate(rule.timing, tick);
      if (level > ACTIVE_THRESHOLD) {
        active.push({ index, rule });
      }
    });
    return active;
  }
}
