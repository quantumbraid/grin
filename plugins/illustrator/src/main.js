// GRIN Illustrator plugin logic for vector flattening, metadata tagging, and export.

const illustrator = require("illustrator");
const uxp = require("uxp");

const { app, core } = illustrator;
const { localFileSystem } = uxp.storage;

const statusElement = document.getElementById("status");
const groupSelect = document.getElementById("groupSelect");
const lockToggle = document.getElementById("lockToggle");
const resolutionInput = document.getElementById("resolutionInput");
const previewButton = document.getElementById("previewButton");
const exportButton = document.getElementById("exportButton");

const GROUP_TAG_REGEX = /\bG(1[0-5]|[0-9])\b/i;
const GROUP_BRACKET_REGEX = /\[G(1[0-5]|[0-9])\]/i;
const LOCK_TAG_REGEX = /\bLOCK\b|\[L\]/i;
const UNLOCK_TAG_REGEX = /\bUNLOCK\b|\[U\]/i;

/**
 * Update the status footer with a short message.
 * @param {string} message - Status message to display.
 */
function setStatus(message) {
  if (!statusElement) {
    return;
  }
  statusElement.textContent = message;
}

/**
 * Build a friendly summary of the current selection state.
 * @returns {string} summary text
 */
function describeSelection() {
  const groupId = groupSelect ? Number(groupSelect.value) : 0;
  const lockState = lockToggle ? lockToggle.checked : false;
  const resolution = resolutionInput ? Number(resolutionInput.value) : 1024;
  return `Group ${groupId} • ${lockState ? "Locked" : "Unlocked"} • ${resolution}px`;
}

/**
 * Normalize a group ID, clamping to 0-15 and defaulting when undefined.
 * @param {number | undefined | null} groupId - Candidate group ID.
 * @param {number} fallback - Fallback group ID when undefined.
 * @returns {number} Normalized group ID.
 */
function normalizeGroupId(groupId, fallback) {
  if (typeof groupId !== "number" || Number.isNaN(groupId)) {
    return fallback;
  }
  if (groupId < 0) {
    return 0;
  }
  if (groupId > 15) {
    return 15;
  }
  return groupId;
}

/**
 * Parse a metadata source string into group/lock hints.
 * @param {string} sourceText - Text to inspect (name or note).
 * @param {number} fallbackGroup - Fallback group ID.
 * @param {boolean} fallbackLock - Fallback lock state.
 * @returns {{groupId: number, locked: boolean, source: string}} Parsed metadata.
 */
function parseMetadataTags(sourceText, fallbackGroup, fallbackLock) {
  const bracketMatch = sourceText.match(GROUP_BRACKET_REGEX);
  const inlineMatch = sourceText.match(GROUP_TAG_REGEX);
  const rawGroup = bracketMatch ? bracketMatch[1] : inlineMatch ? inlineMatch[1] : null;
  const groupId = normalizeGroupId(rawGroup ? Number(rawGroup) : undefined, fallbackGroup);
  const hasLockTag = LOCK_TAG_REGEX.test(sourceText);
  const hasUnlockTag = UNLOCK_TAG_REGEX.test(sourceText);
  const locked = hasLockTag ? true : hasUnlockTag ? false : fallbackLock;

  return {
    groupId,
    locked,
    source: rawGroup ? "tag" : "default",
  };
}

/**
 * Resolve group/lock metadata for a page item using name/note tags.
 * @param {import('illustrator').PageItem} item - Page item to inspect.
 * @param {number} fallbackGroup - Fallback group ID.
 * @param {boolean} fallbackLock - Fallback lock state.
 * @returns {{groupId: number, locked: boolean, source: string}} Metadata payload.
 */
function resolveItemMetadata(item, fallbackGroup, fallbackLock) {
  const nameSource = item.name || "";
  const noteSource = item.note || "";
  const nameMetadata = parseMetadataTags(nameSource, fallbackGroup, fallbackLock);
  const noteMetadata = parseMetadataTags(noteSource, fallbackGroup, fallbackLock);

  if (noteMetadata.source === "tag") {
    return { ...noteMetadata, source: "note" };
  }

  return { ...nameMetadata, source: nameMetadata.source === "tag" ? "name" : "default" };
}

/**
 * Collect all page items in the document for metadata tagging.
 * @param {import('illustrator').Document} document - Illustrator document.
 * @returns {import('illustrator').PageItem[]} Flat list of items.
 */
function collectPageItems(document) {
  return Array.from(document.pageItems || []);
}

/**
 * Capture metadata for page items in the active document.
 * @param {import('illustrator').Document} document - Illustrator document.
 * @returns {{items: Array<{name: string, groupId: number, locked: boolean, source: string}>}}
 */
function captureItemMetadata(document) {
  const defaultGroup = groupSelect ? Number(groupSelect.value) : 0;
  const defaultLocked = lockToggle ? lockToggle.checked : false;
  const items = collectPageItems(document);

  return {
    items: items.map((item) => {
      const metadata = resolveItemMetadata(item, defaultGroup, defaultLocked);
      return {
        name: item.name || "(unnamed)",
        groupId: metadata.groupId,
        locked: item.locked || metadata.locked,
        source: metadata.source,
      };
    }),
  };
}

/**
 * Build a flattening plan that maps vector artboards to raster pixels.
 * @param {import('illustrator').Document} document - Illustrator document.
 * @param {number} resolution - Target raster width in pixels.
 * @returns {{artboardName: string, width: number, height: number, scale: number}[]} Plan steps.
 */
function buildFlatteningPlan(document, resolution) {
  const artboards = Array.from(document.artboards || []);

  return artboards.map((artboard) => {
    const rect = artboard.artboardRect;
    const widthPoints = Math.abs(rect[2] - rect[0]);
    const heightPoints = Math.abs(rect[1] - rect[3]);
    const scale = resolution / widthPoints;
    const heightPixels = Math.round(heightPoints * scale);

    return {
      artboardName: artboard.name || "Artboard",
      width: Math.round(resolution),
      height: heightPixels,
      scale,
    };
  });
}

/**
 * Build the default export file names for a document.
 * @param {string} documentName - Document title.
 * @returns {{baseName: string, rgbaName: string, groupsName: string, lockName: string, rulesName: string, grinName: string}}
 */
function buildExportNames(documentName) {
  const baseName = documentName.replace(/\.[^/.]+$/, "");
  return {
    baseName,
    rgbaName: `${baseName}.png`,
    groupsName: `${baseName}.groups.png`,
    lockName: `${baseName}.lock.png`,
    rulesName: `${baseName}.rules.json`,
    grinName: `${baseName}.grin`,
  };
}

/**
 * Prompt the user for an export folder using the UXP file picker.
 * @returns {Promise<import('uxp').storage.Folder>} Export folder handle.
 */
async function pickExportFolder() {
  return localFileSystem.getFolder();
}

/**
 * Find a document layer by name.
 * @param {import('illustrator').Document} document - Illustrator document.
 * @param {string} layerName - Target layer name.
 * @returns {import('illustrator').Layer | null} Layer handle.
 */
function findLayerByName(document, layerName) {
  const layers = Array.from(document.layers || []);
  return layers.find((layer) => layer.name === layerName) || null;
}

/**
 * Temporarily override layer visibility while exporting an artboard.
 * @param {import('illustrator').Layer[]} layers - Layers to show.
 * @param {() => Promise<void>} exporter - Exporter callback.
 * @returns {Promise<void>} Completion promise.
 */
async function withLayerVisibilityOverride(layers, exporter) {
  const originalStates = layers.map((layer) => ({ layer, visible: layer.visible }));
  layers.forEach((layer) => {
    layer.visible = true;
  });

  await exporter();

  originalStates.forEach(({ layer, visible }) => {
    layer.visible = visible;
  });
}

/**
 * Export the active artboard as a PNG file at a given resolution.
 * @param {import('illustrator').Document} document - Illustrator document.
 * @param {import('uxp').storage.File} file - Destination file.
 * @param {number} resolution - Target width in pixels.
 * @returns {Promise<void>} Completion promise.
 */
async function exportArtboardAsPng(document, file, resolution) {
  const exportOptions = {
    type: "PNG",
    antiAliasing: true,
    transparency: true,
    resolution,
  };

  await document.exportFile(file, exportOptions);
}

/**
 * Export group and lock maps using dedicated metadata layers.
 * @param {import('illustrator').Document} document - Illustrator document.
 * @param {import('uxp').storage.File} groupsFile - Group map output.
 * @param {import('uxp').storage.File} lockFile - Lock map output.
 * @param {number} resolution - Raster width in pixels.
 * @returns {Promise<void>} Completion promise.
 */
async function exportMetadataMaps(document, groupsFile, lockFile, resolution) {
  const groupLayer = findLayerByName(document, "GRIN_GROUPS") || findLayerByName(document, "GRIN_GROUP_MAP");
  const lockLayer = findLayerByName(document, "GRIN_LOCK") || findLayerByName(document, "GRIN_LOCK_MAP");

  if (!groupLayer) {
    throw new Error("Missing GRIN_GROUPS layer for group map export.");
  }

  await withLayerVisibilityOverride([groupLayer], async () => {
    await exportArtboardAsPng(document, groupsFile, resolution);
  });

  if (lockFile) {
    if (!lockLayer) {
      throw new Error("Missing GRIN_LOCK layer for lock map export.");
    }

    await withLayerVisibilityOverride([lockLayer], async () => {
      await exportArtboardAsPng(document, lockFile, resolution);
    });
  }
}

/**
 * Write the rules JSON sidecar for the export pipeline.
 * @param {import('uxp').storage.File} rulesFile - Destination file.
 * @param {Array} rules - Rule definitions.
 * @returns {Promise<void>} Completion promise.
 */
async function writeRulesJson(rulesFile, rules) {
  const payload = {
    tickMicros: 33333,
    opcodeSetId: 0,
    rules,
  };
  await rulesFile.write(JSON.stringify(payload, null, 2));
}

/**
 * Build the CLI command for grin-encode.
 * @param {string} exportRoot - Export folder path.
 * @param {ReturnType<typeof buildExportNames>} names - Export file names.
 * @returns {string} CLI command.
 */
function buildEncodeCommand(exportRoot, names) {
  return `node tools/bin/grin-encode.js "${exportRoot}/${names.rgbaName}" "${exportRoot}/${names.grinName}" --groups "${exportRoot}/${names.groupsName}" --rules "${exportRoot}/${names.rulesName}"`;
}

/**
 * Build the CLI command for grin-validate.
 * @param {string} exportRoot - Export folder path.
 * @param {ReturnType<typeof buildExportNames>} names - Export file names.
 * @returns {string} CLI command.
 */
function buildValidateCommand(exportRoot, names) {
  return `node tools/bin/grin-validate.js "${exportRoot}/${names.grinName}"`;
}

/**
 * Handle preview action by summarizing vector metadata and flattening.
 */
function handlePreview() {
  const document = app.activeDocument;
  if (!document) {
    setStatus("No active document to preview.");
    return;
  }

  const resolution = resolutionInput ? Number(resolutionInput.value) : 1024;
  const metadata = captureItemMetadata(document);
  const taggedItems = metadata.items.filter((item) => item.source !== "default");
  const plan = buildFlatteningPlan(document, resolution);
  const artboardSummary = plan.map((entry) => `${entry.artboardName}: ${entry.width}x${entry.height}px`).join(" | ");

  setStatus(`Preview: ${taggedItems.length} tagged item(s). ${artboardSummary}`);
}

/**
 * Handle export action with metadata capture and export pipeline.
 */
async function handleExport() {
  const document = app.activeDocument;
  if (!document) {
    setStatus("No active document to export.");
    return;
  }

  try {
    setStatus("Selecting export folder...");
    const exportFolder = await pickExportFolder();
    const names = buildExportNames(document.title || "grin-export.ai");
    const rgbaFile = await exportFolder.createFile(names.rgbaName, { overwrite: true });
    const groupsFile = await exportFolder.createFile(names.groupsName, { overwrite: true });
    const lockFile = await exportFolder.createFile(names.lockName, { overwrite: true });
    const rulesFile = await exportFolder.createFile(names.rulesName, { overwrite: true });
    const resolution = resolutionInput ? Number(resolutionInput.value) : 1024;

    setStatus("Exporting flattened artboard...");
    await exportArtboardAsPng(document, rgbaFile, resolution);

    setStatus("Exporting group/lock maps...");
    await exportMetadataMaps(document, groupsFile, lockFile, resolution);

    setStatus("Writing rules sidecar...");
    await writeRulesJson(rulesFile, []);

    const exportRoot = exportFolder.nativePath.replace(/\\/g, "/");
    const encodeCommand = buildEncodeCommand(exportRoot, names);
    const validateCommand = buildValidateCommand(exportRoot, names);

    setStatus(`Export complete. Run: ${encodeCommand} then ${validateCommand}`);
  } catch (error) {
    setStatus(`Export failed: ${error.message}`);
  }
}

/**
 * Handle metadata changes and update the status.
 */
function handleSelectionChange() {
  setStatus(`Selection updated (${describeSelection()}).`);
}

if (groupSelect) {
  groupSelect.addEventListener("change", handleSelectionChange);
}

if (lockToggle) {
  lockToggle.addEventListener("change", handleSelectionChange);
}

if (resolutionInput) {
  resolutionInput.addEventListener("change", handleSelectionChange);
}

if (previewButton) {
  previewButton.addEventListener("click", handlePreview);
}

if (exportButton) {
  exportButton.addEventListener("click", () => {
    core.executeAsModal(handleExport, { commandName: "GRIN Export" });
  });
}

setStatus(`Ready (${describeSelection()}).`);
