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
