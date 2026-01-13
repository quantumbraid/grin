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
  GrinCanvasRenderer,
  GrinLoader,
  GrinPlayer,
  OpcodeRegistry,
  RAFTickScheduler,
  RuleEngine,
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

const renderer = new GrinCanvasRenderer();
let player = null;

const schedulerFactory = (tickMicros) => new RAFTickScheduler(tickMicros);

const makePlayer = () =>
  new GrinPlayer(schedulerFactory, new RuleEngine(), OpcodeRegistry.getInstance(), () => {
    if (!player) return;
    renderer.render(player.getCurrentFrame(), canvas);
  });

async function loadFile(file) {
  const grinFile = await GrinLoader.fromFile(file);
  initializePlayer(grinFile);
}

async function loadFromUrl(url) {
  const grinFile = await GrinLoader.fromURL(url);
  initializePlayer(grinFile);
}

function initializePlayer(grinFile) {
  player = makePlayer();
  player.load(grinFile);
  canvas.width = grinFile.header.width;
  canvas.height = grinFile.header.height;
  renderer.render(player.getCurrentFrame(), canvas);
  metadata.textContent = [
    `Dimensions: ${grinFile.header.width} × ${grinFile.header.height}`,
    `Rules: ${grinFile.header.ruleCount}`,
    `Tick Rate: ${grinFile.header.tickMicros} µs`,
  ].join("\n");
  seekSlider.value = "0";
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
  renderer.render(player.getCurrentFrame(), canvas);
});

seekSlider.addEventListener("input", (event) => {
  if (!player) return;
  const value = Number(event.target.value);
  player.seek(value);
  renderer.render(player.getCurrentFrame(), canvas);
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

async function loadSamples() {
  sampleList.setAttribute("aria-busy", "true");
  try {
    const response = await fetch("./samples/samples.json");
    if (!response.ok) {
      sampleList.innerHTML = "<li>No samples available</li>";
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
    sampleList.innerHTML = "<li>No samples available</li>";
  } finally {
    sampleList.setAttribute("aria-busy", "false");
  }
}

loadSamples();
