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
const FALLBACK_LIB_URL =
  "https://cdn.jsdelivr.net/gh/quantumbraid/grin@main/web/dist/grin-player.js";

function toBoolean(value) {
  if (value === undefined || value === null) {
    return false;
  }
  if (value === "" || value === "true" || value === "1") {
    return true;
  }
  return false;
}

function normalizeElements(list) {
  return list.filter((element) => element.tagName.toLowerCase() !== "script");
}

function getScriptElement() {
  if (document.currentScript) {
    return document.currentScript;
  }
  const scripts = document.querySelectorAll("script[type='module']");
  const currentUrl = new URL(import.meta.url, document.baseURI).href;
  for (const script of scripts) {
    if (!script.src) {
      continue;
    }
    const scriptUrl = new URL(script.src, document.baseURI).href;
    if (scriptUrl === currentUrl) {
      return script;
    }
  }
  return null;
}

function getScriptEmbed(script) {
  if (!script) {
    return null;
  }
  const src = script.dataset.grinSrc;
  if (!src) {
    return null;
  }
  const placeholder = document.createElement("div");
  placeholder.dataset.grinSrc = src;
  if (script.dataset.grinAutoplay !== undefined) {
    placeholder.dataset.grinAutoplay = script.dataset.grinAutoplay;
  }
  if (script.dataset.grinPlaybackrate !== undefined) {
    placeholder.dataset.grinPlaybackrate = script.dataset.grinPlaybackrate;
  }
  script.insertAdjacentElement("afterend", placeholder);
  return placeholder;
}

function mountPlayer(target) {
  const src = target.dataset.grinSrc;
  if (!src) {
    return;
  }

  const autoplay = toBoolean(target.dataset.grinAutoplay);
  const playbackrate = target.dataset.grinPlaybackrate;

  let player;
  if (target.tagName.toLowerCase() === "grin-player") {
    player = target;
  } else {
    player = document.createElement("grin-player");
    target.appendChild(player);
  }

  player.setAttribute("src", src);
  if (autoplay) {
    player.setAttribute("autoplay", "");
  }
  if (playbackrate) {
    player.setAttribute("playbackrate", playbackrate);
  }
}

function loadViewer(libUrl) {
  const hasPlayer =
    typeof customElements !== "undefined" && customElements.get("grin-player");
  if (hasPlayer) {
    return Promise.resolve();
  }
  return import(libUrl);
}

function resolveDefaultLibUrl(script) {
  const baseUrl = script?.src || import.meta.url;
  if (baseUrl) {
    try {
      return new URL("../dist/grin-player.js", baseUrl).toString();
    } catch (error) {
      return FALLBACK_LIB_URL;
    }
  }
  return FALLBACK_LIB_URL;
}

function init() {
  const script = getScriptElement();
  const libUrl = script?.dataset.grinLib || resolveDefaultLibUrl(script);

  const elements = normalizeElements(
    Array.from(document.querySelectorAll("[data-grin-src]"))
  );
  const scriptEmbed = getScriptEmbed(script);
  if (scriptEmbed) {
    elements.push(scriptEmbed);
  }
  if (elements.length === 0) {
    return;
  }

  loadViewer(libUrl)
    .then(() => {
      elements.forEach((element) => mountPlayer(element));
    })
    .catch((error) => {
      console.error("GRIN embed failed to load viewer:", error);
    });
}

if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", init);
} else {
  init();
}
