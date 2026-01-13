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
import {
  CONTROL_BYTE_MASKS,
  DisplayBuffer,
  GrinCanvasRenderer,
  GrinLoader,
  GrinPlayer,
  OpcodeRegistry,
  RAFTickScheduler,
  RuleEngine,
  validateGrinFile,
} from "../dist/grin.js";

const canvas = document.getElementById("previewCanvas");
const playButton = document.getElementById("playButton");
const pauseButton = document.getElementById("pauseButton");
const stopButton = document.getElementById("stopButton");
const seekSlider = document.getElementById("seekSlider");
const metadata = document.getElementById("metadata");
const dropZone = document.getElementById("dropZone");
const fileInput = document.getElementById("fileInput");
const fileButton = document.querySelector(".file-button");
const sampleList = document.getElementById("sampleList");
const statusPanel = document.getElementById("statusPanel");
const validationPanel = document.getElementById("validationPanel");
const validationStatus = document.getElementById("validationStatus");
const validationErrors = document.getElementById("validationErrors");
const validationWarnings = document.getElementById("validationWarnings");
const validationInvalidPixels = document.getElementById("validationInvalidPixels");
const validationRuleCount = document.getElementById("validationRuleCount");
const validationNotes = document.getElementById("validationNotes");
const groupFilterStatus = document.getElementById("groupFilterStatus");

const renderer = new GrinCanvasRenderer();
let player = null;
let currentFile = null;
let groupIds = null;
let activeGroup = null;
let filteredBuffer = null;

const schedulerFactory = (tickMicros) => new RAFTickScheduler(tickMicros);

const makePlayer = () =>
  new GrinPlayer(schedulerFactory, new RuleEngine(), OpcodeRegistry.getInstance(), () => {
    if (!player) return;
    renderPreview();
  });

setControlsEnabled(false);
resetValidationSummary();
updateGroupFilterStatus();

function setControlsEnabled(enabled) {
  [playButton, pauseButton, stopButton, seekSlider].forEach((element) => {
    element.disabled = !enabled;
    element.setAttribute("aria-disabled", String(!enabled));
  });
}

function setStatus(state, message) {
  statusPanel.textContent = message;
  if (state) {
    statusPanel.setAttribute("data-state", state);
  } else {
    statusPanel.removeAttribute("data-state");
  }
}

function handleLoadError(error, fallbackMessage) {
  const detail = error instanceof Error ? error.message : "";
  setStatus("error", detail ? `${fallbackMessage} (${detail})` : fallbackMessage);
  metadata.textContent = "Load a file to see metadata.";
  currentFile = null;
  groupIds = null;
  filteredBuffer = null;
  resetValidationSummary();
  setActiveGroup(null, false);
  setControlsEnabled(false);
}

function renderPreview() {
  if (!player) return;
  const frame = player.getCurrentFrame();
  if (activeGroup === null || !groupIds) {
    renderer.render(frame, canvas);
    return;
  }
  if (!filteredBuffer || filteredBuffer.width !== frame.width || filteredBuffer.height !== frame.height) {
    filteredBuffer = new DisplayBuffer(frame.width, frame.height);
  }
  filteredBuffer.rgbaData.set(frame.rgbaData);
  for (let i = 0; i < groupIds.length; i += 1) {
    if (groupIds[i] !== activeGroup) {
      filteredBuffer.rgbaData[i * 4 + 3] = 0;
    }
  }
  renderer.render(filteredBuffer, canvas);
}

function updateValidationSummary(grinFile) {
  const report = validateGrinFile(grinFile);
  const invalidPixels = grinFile.pixels.reduce((count, pixel) => {
    return count + ((pixel.c & CONTROL_BYTE_MASKS.RESERVED) !== 0 ? 1 : 0);
  }, 0);

  validationStatus.textContent = report.ok ? "OK" : "Issues found";
  validationErrors.textContent = String(report.errors.length);
  validationWarnings.textContent = String(report.warnings.length);
  validationInvalidPixels.textContent = String(invalidPixels);
  validationRuleCount.textContent = String(grinFile.header.ruleCount);

  let state = "ok";
  if (report.errors.length > 0) {
    state = "error";
  } else if (report.warnings.length > 0 || invalidPixels > 0) {
    state = "warn";
  }
  validationPanel.setAttribute("data-state", state);

  const notes = [];
  if (report.errors.length > 0) {
    notes.push(`Errors: ${report.errors.slice(0, 2).join(" | ")}`);
  }
  if (report.warnings.length > 0) {
    notes.push(`Warnings: ${report.warnings.slice(0, 2).join(" | ")}`);
  }
  if (invalidPixels > 0) {
    notes.push(`Invalid pixels with reserved bits: ${invalidPixels}`);
  }
  if (notes.length === 0) {
    notes.push("No validation issues detected.");
  }
  validationNotes.textContent = notes.join("\n");
}

function resetValidationSummary() {
  validationStatus.textContent = "No file loaded";
  validationErrors.textContent = "0";
  validationWarnings.textContent = "0";
  validationInvalidPixels.textContent = "0";
  validationRuleCount.textContent = "0";
  validationNotes.textContent = "No validation run yet.";
  validationPanel.removeAttribute("data-state");
}

function updateGroupFilterStatus() {
  if (activeGroup === null) {
    groupFilterStatus.textContent = "All";
    return;
  }
  groupFilterStatus.textContent = activeGroup.toString(16).toUpperCase();
}

function setActiveGroup(groupId, shouldRender = true) {
  activeGroup = Number.isInteger(groupId) ? groupId : null;
  updateGroupFilterStatus();
  if (shouldRender) {
    renderPreview();
  }
}

function stepTicks(delta) {
  if (!player) return;
  const maxTick = Number(seekSlider.max || "1000");
  const current = player.getCurrentTick();
  const next = Math.min(maxTick, Math.max(0, current + delta));
  player.pause();
  player.seek(next);
  seekSlider.value = String(next);
}

function isInteractiveTarget(target) {
  if (!(target instanceof HTMLElement)) return false;
  const tag = target.tagName;
  return (
    tag === "INPUT" ||
    tag === "TEXTAREA" ||
    tag === "SELECT" ||
    tag === "BUTTON" ||
    target.isContentEditable
  );
}

function handleKeyboardShortcuts(event) {
  if (event.defaultPrevented || isInteractiveTarget(event.target)) {
    return;
  }
  const key = event.key.toLowerCase();
  if (key === " ") {
    event.preventDefault();
    if (!player) return;
    if (player.isPlaying()) {
      player.pause();
    } else {
      player.play();
    }
    return;
  }
  if (key === "arrowright") {
    event.preventDefault();
    stepTicks(event.shiftKey ? 10 : 1);
    return;
  }
  if (key === "arrowleft") {
    event.preventDefault();
    stepTicks(event.shiftKey ? -10 : -1);
    return;
  }
  if (key === "escape") {
    event.preventDefault();
    setActiveGroup(null);
    return;
  }
  if (key.length === 1 && /^[0-9a-f]$/.test(key)) {
    event.preventDefault();
    setActiveGroup(parseInt(key, 16));
  }
}

async function loadFile(file) {
  setStatus("loading", "Loading file...");
  setControlsEnabled(false);
  try {
    const grinFile = await GrinLoader.fromFile(file);
    initializePlayer(grinFile);
    setStatus("success", "File loaded. Ready to play.");
  } catch (error) {
    handleLoadError(error, "Could not load that file. Ensure it is a valid .grin or .grn.");
  }
}

async function loadFromUrl(url) {
  setStatus("loading", "Loading sample...");
  setControlsEnabled(false);
  try {
    const grinFile = await GrinLoader.fromURL(url);
    initializePlayer(grinFile);
    setStatus("success", "Sample loaded. Ready to play.");
  } catch (error) {
    handleLoadError(error, "Sample failed to load. Check the sample path.");
  }
}

function initializePlayer(grinFile) {
  player = makePlayer();
  player.load(grinFile);
  currentFile = grinFile;
  groupIds = new Uint8Array(grinFile.pixels.length);
  grinFile.pixels.forEach((pixel, index) => {
    groupIds[index] = pixel.c & CONTROL_BYTE_MASKS.GROUP_ID;
  });
  filteredBuffer = null;
  setActiveGroup(null, false);
  updateValidationSummary(grinFile);
  canvas.width = grinFile.header.width;
  canvas.height = grinFile.header.height;
  renderPreview();
  metadata.textContent = [
    `Dimensions: ${grinFile.header.width} × ${grinFile.header.height}`,
    `Rules: ${grinFile.header.ruleCount}`,
    `Tick Rate: ${grinFile.header.tickMicros} µs`,
  ].join("\n");
  seekSlider.value = "0";
  setControlsEnabled(true);
}

playButton.addEventListener("click", () => {
  if (!player) return;
  player.play();
});

pauseButton.addEventListener("click", () => {
  player?.pause();
});

stopButton.addEventListener("click", () => {
  if (!player) return;
  player.stop();
  seekSlider.value = "0";
  renderPreview();
});

seekSlider.addEventListener("input", (event) => {
  if (!player) return;
  const value = Number(event.target.value);
  player.seek(value);
});

dropZone.addEventListener("dragover", (event) => {
  event.preventDefault();
  dropZone.classList.add("dragover");
});

dropZone.addEventListener("dragleave", () => {
  dropZone.classList.remove("dragover");
});

dropZone.addEventListener("drop", (event) => {
  event.preventDefault();
  dropZone.classList.remove("dragover");
  const file = event.dataTransfer.files[0];
  if (file) {
    loadFile(file);
  }
});

dropZone.addEventListener("keydown", (event) => {
  if (event.key === "Enter" || event.key === " ") {
    event.preventDefault();
    fileInput.click();
  }
});

fileButton.addEventListener("keydown", (event) => {
  if (event.key === "Enter" || event.key === " ") {
    event.preventDefault();
    fileInput.click();
  }
});

fileInput.addEventListener("change", (event) => {
  const file = event.target.files[0];
  if (file) {
    loadFile(file);
  }
});

document.addEventListener("keydown", handleKeyboardShortcuts);

async function loadSamples() {
  sampleList.setAttribute("aria-busy", "true");
  try {
    const response = await fetch("./samples/samples.json");
    if (!response.ok) {
      sampleList.innerHTML = "<li>No samples available. Check ./samples/samples.json.</li>";
      return;
    }
    const samples = await response.json();
    sampleList.innerHTML = "";
    samples.forEach((sample) => {
      const li = document.createElement("li");
      li.setAttribute("role", "button");
      li.setAttribute("tabindex", "0");
      li.setAttribute("aria-label", `Load sample ${sample.name}`);
      li.textContent = sample.name;
      li.addEventListener("click", () => loadFromUrl(sample.path));
      li.addEventListener("keydown", (event) => {
        if (event.key === "Enter" || event.key === " ") {
          event.preventDefault();
          loadFromUrl(sample.path);
        }
      });
      sampleList.appendChild(li);
    });
  } catch (error) {
    sampleList.innerHTML = "<li>No samples available. Check the samples folder.</li>";
  } finally {
    sampleList.setAttribute("aria-busy", "false");
  }
}

loadSamples();
