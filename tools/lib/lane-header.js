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
import { CONTROL_GROUP_LABELS } from "./format.js";

export const LANE_LIMIT = 16;
export const HEADER_MAX_BYTES = 592;

const GROUP_LABEL_SET = new Set(CONTROL_GROUP_LABELS);
const REPEAT_PATTERN = /^(\d{3}):(min|sec|mil)$/i;
const ACTION_PATTERN = /^(sety|setz|[+-][0-9a-f]{1,2}(rr|gg|bb|aa))$/i;
const LANE_PATTERN = /\{(\d{2})\[([^\]|]+)\|([^\]|]+)\|([^\]|]+)\]\}/g;

/**
 * Parse a lane header string into a normalized lane array.
 * @param {string} header - Header string containing one or more lanes.
 * @returns {Array} Parsed lane descriptors.
 */
export function parseLaneHeader(header) {
  const raw = String(header ?? "");
  const trimmed = raw.trim();
  if (!trimmed) {
    return [];
  }

  validateHeaderLength(trimmed);

  const lanes = [];
  let lastLaneNumber = null;
  let lastIndex = 0;

  for (const match of trimmed.matchAll(LANE_PATTERN)) {
    const { index } = match;
    if (index !== lastIndex) {
      const gap = trimmed.slice(lastIndex, index).trim();
      if (gap) {
        throw new Error(`Unexpected characters between lanes: ${gap}`);
      }
    }
    lastIndex = index + match[0].length;

    const laneNumber = parseLaneNumber(match[1]);
    if (lastLaneNumber !== null && laneNumber !== lastLaneNumber + 1) {
      throw new Error(`Lane numbers must be sequential. Expected ${lastLaneNumber + 1} but got ${laneNumber}.`);
    }
    lastLaneNumber = laneNumber;

    const groups = normalizeGroups(match[2]);
    const action = parseAction(match[3]);
    const repeat = parseRepeat(match[4]);

    lanes.push({
      laneNumber,
      groups,
      action,
      repeat,
    });
  }

  const remainder = trimmed.slice(lastIndex).trim();
  if (remainder) {
    throw new Error(`Unexpected characters after lanes: ${remainder}`);
  }

  if (lanes.length > LANE_LIMIT) {
    throw new Error(`Lane count exceeds ${LANE_LIMIT}.`);
  }

  return lanes;
}

/**
 * Format lane descriptors into a canonical header string.
 * @param {Array} lanes - Lane descriptors to format.
 * @returns {string} Normalized header string.
 */
export function formatLaneHeader(lanes) {
  const normalized = normalizeLaneSpecs(lanes);
  if (normalized.length > LANE_LIMIT) {
    throw new Error(`Lane count exceeds ${LANE_LIMIT}.`);
  }

  const sorted = [...normalized].sort((a, b) => a.laneNumber - b.laneNumber);
  const parts = sorted.map((lane) => {
    const laneNumber = formatLaneNumber(lane.laneNumber);
    const groups = lane.groups.join("");
    const action = formatAction(lane.action);
    const repeat = formatRepeat(lane.repeat);
    return `{${laneNumber}[${groups}|${action}|${repeat}]}`;
  });

  const header = parts.join("");
  validateHeaderLength(header);
  return header;
}

/**
 * Normalize arbitrary lane payloads into validated lane descriptors.
 * @param {Array} lanes - Lane payloads (typically from JSON).
 * @returns {Array} Normalized lane descriptors.
 */
export function normalizeLaneSpecs(lanes) {
  if (!Array.isArray(lanes)) {
    throw new Error("Lane specs must be an array.");
  }

  return lanes.map((lane) => {
    if (!lane || typeof lane !== "object") {
      throw new Error("Lane specs must be objects.");
    }
    const laneNumber = parseLaneNumber(lane.laneNumber ?? lane.lane ?? lane.index ?? lane.id ?? lane.number);
    const groups = normalizeGroups(lane.groups ?? lane.group ?? "");
    const action = normalizeActionSpec(lane.action ?? lane.op ?? lane.operation ?? "");
    const repeat = normalizeRepeatSpec(lane.repeat ?? lane.frequency ?? lane.interval ?? "");

    return {
      laneNumber,
      groups,
      action,
      repeat,
    };
  });
}

function parseLaneNumber(value) {
  if (value === undefined || value === null || value === "") {
    throw new Error("Lane number is required.");
  }
  const parsed = Number.parseInt(String(value), 10);
  if (!Number.isInteger(parsed) || parsed < 0 || parsed >= LANE_LIMIT) {
    throw new Error(`Lane number must be between 0 and ${LANE_LIMIT - 1}.`);
  }
  return parsed;
}

function formatLaneNumber(value) {
  return String(value).padStart(2, "0");
}

function normalizeGroups(groups) {
  const raw = Array.isArray(groups) ? groups.join("") : String(groups ?? "");
  const trimmed = raw.trim();
  if (!trimmed) {
    throw new Error("Lane groups are required.");
  }

  const found = new Set();
  for (const char of trimmed) {
    const label = char.toUpperCase();
    if (!GROUP_LABEL_SET.has(label)) {
      throw new Error(`Invalid group label: ${char}`);
    }
    found.add(label);
  }

  if (found.size === 0) {
    throw new Error("Lane groups must include at least one valid label.");
  }

  return CONTROL_GROUP_LABELS.filter((label) => found.has(label));
}

function parseRepeat(repeat) {
  const trimmed = String(repeat ?? "").trim();
  const match = trimmed.match(REPEAT_PATTERN);
  if (!match) {
    throw new Error(`Invalid repeat spec: ${repeat}`);
  }
  const count = Number.parseInt(match[1], 10);
  return {
    count,
    unit: match[2].toLowerCase(),
  };
}

function formatRepeat(repeat) {
  const normalized = normalizeRepeatSpec(repeat);
  return `${String(normalized.count).padStart(3, "0")}:${normalized.unit}`;
}

function normalizeRepeatSpec(repeat) {
  if (typeof repeat === "string" || typeof repeat === "number") {
    return parseRepeat(repeat);
  }
  if (!repeat || typeof repeat !== "object") {
    throw new Error("Repeat spec is required.");
  }
  const count = Number.parseInt(String(repeat.count ?? repeat.value ?? repeat.amount ?? repeat.freq ?? ""), 10);
  if (!Number.isInteger(count) || count < 0 || count > 999) {
    throw new Error("Repeat count must be 000-999.");
  }
  const unit = String(repeat.unit ?? repeat.scale ?? repeat.unitName ?? "").toLowerCase();
  if (!REPEAT_PATTERN.test(`${String(count).padStart(3, "0")}:${unit}`)) {
    throw new Error("Repeat unit must be min, sec, or mil.");
  }
  return { count, unit };
}

function parseAction(action) {
  const trimmed = String(action ?? "").trim();
  if (!trimmed || !ACTION_PATTERN.test(trimmed)) {
    throw new Error(`Invalid action spec: ${action}`);
  }

  const normalized = trimmed.toLowerCase();
  if (normalized === "sety" || normalized === "setz") {
    return { type: normalized };
  }

  const match = normalized.match(/^([+-])([0-9a-f]{1,2})(rr|gg|bb|aa)$/i);
  if (!match) {
    throw new Error(`Invalid delta action: ${action}`);
  }

  return {
    type: "delta",
    direction: match[1],
    amount: Number.parseInt(match[2], 16),
    channel: match[3].toUpperCase(),
  };
}

function formatAction(action) {
  const normalized = normalizeActionSpec(action);
  if (normalized.type === "sety" || normalized.type === "setz") {
    return normalized.type;
  }
  const amount = normalized.amount.toString(16).padStart(2, "0");
  return `${normalized.direction}${amount}${normalized.channel}`.toLowerCase();
}

function normalizeActionSpec(action) {
  if (typeof action === "string") {
    return parseAction(action);
  }
  if (!action || typeof action !== "object") {
    throw new Error("Action spec is required.");
  }
  const type = String(action.type ?? action.kind ?? action.action ?? "").toLowerCase();
  if (type === "sety" || type === "setz") {
    return { type };
  }
  if (type === "delta") {
    const direction = String(action.direction ?? action.sign ?? "+").trim();
    if (direction !== "+" && direction !== "-") {
      throw new Error("Delta direction must be + or -.");
    }
    const amount = Number.parseInt(String(action.amount ?? action.value ?? action.delta ?? ""), 16);
    if (!Number.isInteger(amount) || amount < 0 || amount > 255) {
      throw new Error("Delta amount must be 00-ff.");
    }
    const channel = String(action.channel ?? action.target ?? "").toUpperCase();
    if (!["RR", "GG", "BB", "AA"].includes(channel)) {
      throw new Error("Delta channel must be RR, GG, BB, or AA.");
    }
    return {
      type: "delta",
      direction,
      amount,
      channel,
    };
  }
  throw new Error("Action spec must be sety, setz, or delta.");
}

function validateHeaderLength(header) {
  const bytes = Buffer.byteLength(header, "utf8");
  if (bytes > HEADER_MAX_BYTES) {
    throw new Error(`Header exceeds ${HEADER_MAX_BYTES} bytes.`);
  }
}
