// GRIN Photoshop plugin scaffold logic for panel UI wiring.

const statusElement = document.getElementById("status");
const groupSelect = document.getElementById("groupSelect");
const lockToggle = document.getElementById("lockToggle");
const previewButton = document.getElementById("previewButton");
const exportButton = document.getElementById("exportButton");

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
 * Handle placeholder preview action.
 */
function handlePreview() {
  setStatus(`Preview requested (${describeSelection()}).`);
}

/**
 * Handle placeholder export action.
 */
function handleExport() {
  setStatus(`Export requested (${describeSelection()}).`);
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
  exportButton.addEventListener("click", handleExport);
}

setStatus(`Ready (${describeSelection()}).`);
