// GRIN Photoshop plugin logic for layer metadata, export, and validation.

const photoshop = require("photoshop");
const uxp = require("uxp");

const { app, action, core } = photoshop;
const { localFileSystem } = uxp.storage;

const statusElement = document.getElementById("status");
const groupSelect = document.getElementById("groupSelect");
const lockToggle = document.getElementById("lockToggle");
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
  return `Group ${groupId} • ${lockState ? "Locked" : "Unlocked"}`;
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
 * Parse a layer name into group/lock metadata based on tag conventions.
 * @param {string} layerName - Layer name to inspect.
 * @param {number} fallbackGroup - Fallback group ID.
 * @param {boolean} fallbackLock - Fallback lock state.
 * @returns {{groupId: number, locked: boolean, source: string}} Parsed metadata.
 */
function parseLayerNameMetadata(layerName, fallbackGroup, fallbackLock) {
  const bracketMatch = layerName.match(GROUP_BRACKET_REGEX);
  const inlineMatch = layerName.match(GROUP_TAG_REGEX);
  const rawGroup = bracketMatch ? bracketMatch[1] : inlineMatch ? inlineMatch[1] : null;
  const groupId = normalizeGroupId(rawGroup ? Number(rawGroup) : undefined, fallbackGroup);
  const hasLockTag = LOCK_TAG_REGEX.test(layerName);
  const hasUnlockTag = UNLOCK_TAG_REGEX.test(layerName);
  const locked = hasLockTag ? true : hasUnlockTag ? false : fallbackLock;

  return {
    groupId,
    locked,
    source: rawGroup ? "layer-name" : "default",
  };
}

/**
 * Recursively collect all layers in a document for traversal.
 * @param {import('photoshop').Document} document - Photoshop document.
 * @returns {import('photoshop').Layer[]} Flat list of layers.
 */
function collectLayers(document) {
  const allLayers = [];

  /**
   * Walk layers in depth-first order.
   * @param {import('photoshop').Layer[]} layers - Layers to walk.
   */
  function walk(layers) {
    layers.forEach((layer) => {
      allLayers.push(layer);
      if (layer.layers && layer.layers.length > 0) {
        walk(Array.from(layer.layers));
      }
    });
  }

  walk(Array.from(document.layers));
  return allLayers;
}

/**
 * Capture layer metadata for all layers in the active document.
 * @param {import('photoshop').Document} document - Photoshop document.
 * @returns {{layers: Array<{id: number, name: string, groupId: number, locked: boolean, source: string}>}}
 */
function captureLayerMetadata(document) {
  const defaultGroup = groupSelect ? Number(groupSelect.value) : 0;
  const defaultLocked = lockToggle ? lockToggle.checked : false;
  const layers = collectLayers(document);

  return {
    layers: layers.map((layer) => {
      const parsed = parseLayerNameMetadata(layer.name, defaultGroup, defaultLocked);
      const layerLocked = layer.allLocked === true || layer.locked === true;

      return {
        id: layer.id,
        name: layer.name,
        groupId: parsed.groupId,
        locked: layerLocked || parsed.locked,
        source: parsed.source,
      };
    }),
  };
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
 * Save a document as a PNG file using Photoshop's batchPlay API.
 * @param {import('photoshop').Document} document - Document to save.
 * @param {import('uxp').storage.File} file - Destination file handle.
 * @returns {Promise<void>} Completion promise.
 */
async function saveDocumentAsPng(document, file) {
  await action.batchPlay(
    [
      {
        _obj: "save",
        as: {
          _obj: "PNGFormat",
          method: {
            _enum: "PNGMethod",
            _value: "quick",
          },
        },
        in: {
          _path: file.nativePath,
          _kind: "local",
        },
        documentID: document.id,
        copy: true,
        _options: {
          dialogOptions: "dontDisplay",
        },
      },
    ],
    {
      synchronousExecution: true,
      modalBehavior: "execute",
    }
  );
}

/**
 * Temporarily override layer visibility while exporting a PNG.
 * @param {import('photoshop').Document} document - Document to export.
 * @param {import('photoshop').Layer[]} visibleLayers - Layers to keep visible.
 * @param {() => Promise<void>} exporter - Exporter callback.
 * @returns {Promise<void>} Completion promise.
 */
async function withVisibilityOverride(document, visibleLayers, exporter) {
  const allLayers = collectLayers(document);
  const previousVisibility = new Map(allLayers.map((layer) => [layer.id, layer.visible]));
  const visibleIds = new Set(visibleLayers.map((layer) => layer.id));

  allLayers.forEach((layer) => {
    layer.visible = visibleIds.has(layer.id);
  });

  try {
    await exporter();
  } finally {
    allLayers.forEach((layer) => {
      layer.visible = previousVisibility.get(layer.id) ?? true;
    });
  }
}

/**
 * Find a metadata layer by matching allowed names.
 * @param {import('photoshop').Document} document - Document to search.
 * @param {string[]} names - Acceptable layer names.
 * @returns {import('photoshop').Layer | null} Matched layer or null.
 */
function findMetadataLayer(document, names) {
  const normalized = names.map((name) => name.toLowerCase());
  const layers = collectLayers(document);
  return layers.find((layer) => normalized.includes(layer.name.toLowerCase())) ?? null;
}

/**
 * Export metadata layers for group and lock maps.
 * @param {import('photoshop').Document} document - Document to export.
 * @param {import('uxp').storage.File} groupsFile - Output file for groups.
 * @param {import('uxp').storage.File | null} lockFile - Output file for locks.
 * @returns {Promise<void>} Completion promise.
 */
async function exportMetadataMaps(document, groupsFile, lockFile) {
  const groupLayer = findMetadataLayer(document, ["GRIN_GROUPS", "GRIN_GROUP_MAP"]);
  const lockLayer = lockFile ? findMetadataLayer(document, ["GRIN_LOCK", "GRIN_LOCK_MAP"]) : null;

  if (!groupLayer) {
    throw new Error("Missing GRIN_GROUPS layer for group map export.");
  }

  await withVisibilityOverride(document, [groupLayer], async () => {
    await saveDocumentAsPng(document, groupsFile);
  });

  if (lockFile) {
    if (!lockLayer) {
      throw new Error("Missing GRIN_LOCK layer for lock map export.");
    }

    await withVisibilityOverride(document, [lockLayer], async () => {
      await saveDocumentAsPng(document, lockFile);
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
 * Handle preview action by summarizing layer metadata.
 */
function handlePreview() {
  const document = app.activeDocument;
  if (!document) {
    setStatus("No active document to preview.");
    return;
  }

  const metadata = captureLayerMetadata(document);
  const taggedLayers = metadata.layers.filter((layer) => layer.source === "layer-name");
  setStatus(`Preview: ${taggedLayers.length} tagged layer(s) detected (${describeSelection()}).`);
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
    const names = buildExportNames(document.title);
    const rgbaFile = await exportFolder.createFile(names.rgbaName, { overwrite: true });
    const groupsFile = await exportFolder.createFile(names.groupsName, { overwrite: true });
    const lockFile = await exportFolder.createFile(names.lockName, { overwrite: true });
    const rulesFile = await exportFolder.createFile(names.rulesName, { overwrite: true });

    setStatus("Exporting RGBA art...");
    await saveDocumentAsPng(document, rgbaFile);

    setStatus("Exporting group/lock maps...");
    await exportMetadataMaps(document, groupsFile, lockFile);

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

if (previewButton) {
  previewButton.addEventListener("click", handlePreview);
}

if (exportButton) {
  exportButton.addEventListener("click", () => {
    core.executeAsModal(handleExport, { commandName: "GRIN Export" });
  });
}

setStatus(`Ready (${describeSelection()}).`);
