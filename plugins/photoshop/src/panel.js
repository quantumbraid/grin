// Panel UI logic for the GRIN export scaffold.
const GROUP_COUNT = 16;
const groupSelect = document.getElementById("groupSelect");
const lockToggle = document.getElementById("lockToggle");
const exportButton = document.getElementById("exportButton");
const validateButton = document.getElementById("validateButton");
const statusText = document.getElementById("statusText");

// Populate group IDs (0-15) for the selector.
for (let groupId = 0; groupId < GROUP_COUNT; groupId += 1) {
  const option = document.createElement("option");
  option.value = String(groupId);
  option.textContent = `Group ${groupId}`;
  groupSelect.appendChild(option);
}

function updateStatus(message) {
  // Surface status updates inside the panel.
  statusText.textContent = message;
}

function getPanelState() {
  // Capture the current UI selections for later export integration.
  return {
    groupId: Number(groupSelect.value),
    lockPixels: lockToggle.checked,
  };
}

exportButton.addEventListener("click", () => {
  const state = getPanelState();
  // Placeholder for export integration.
  updateStatus(
    `Export queued for Group ${state.groupId} (lock=${state.lockPixels}).`
  );
});

validateButton.addEventListener("click", () => {
  const state = getPanelState();
  // Placeholder for validation integration.
  updateStatus(
    `Validation pending for Group ${state.groupId} (lock=${state.lockPixels}).`
  );
});

updateStatus("Ready for export.");
