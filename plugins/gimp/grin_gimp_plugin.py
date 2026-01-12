#!/usr/bin/env python3
# GRIN GIMP plugin providing group/lock overlays, export tooling, and validation hooks.

import json
import os
import subprocess
from gimpfu import (  # pylint: disable=unused-wildcard-import
    CLIP_TO_IMAGE,
    FOREGROUND_FILL,
    GRAY_IMAGE,
    MULTIPLY_MODE,
    NORMAL_MODE,
    PF_BOOL,
    PF_DIRNAME,
    PF_INT,
    PF_STRING,
    gimp,
    main,
    pdb,
    register,
)

GROUP_LAYER_NAMES = ["GRIN_GROUPS", "GRIN_GROUP_MAP"]
LOCK_LAYER_NAMES = ["GRIN_LOCK", "GRIN_LOCK_MAP"]


# --- Layer utilities ---


def find_layer_by_name(parent, target_names):
    """Locate a layer by name (case-insensitive), walking through layer groups."""
    # Normalize names once for comparison.
    normalized = [name.lower() for name in target_names]

    # Walk the layer stack recursively, including layer groups.
    for layer in parent.layers:
        if layer.name.lower() in normalized:
            return layer
        # Layer groups in GIMP expose child layers via `layers`.
        if hasattr(layer, "layers"):
            match = find_layer_by_name(layer, target_names)
            if match is not None:
                return match
    return None


def ensure_metadata_layer(image, name, opacity, mode, fill_value):
    """Ensure a grayscale metadata layer exists, creating and filling if needed."""
    layer = find_layer_by_name(image, [name])
    if layer is not None:
        return layer

    # Create a new grayscale layer sized to the image.
    layer = gimp.Layer(
        image,
        name,
        image.width,
        image.height,
        GRAY_IMAGE,
        opacity,
        mode,
    )
    image.add_layer(layer, 0)

    # Fill the layer with the requested grayscale value.
    gimp.context_push()
    try:
        gimp.set_foreground((fill_value, fill_value, fill_value))
        pdb.gimp_image_set_active_layer(image, layer)
        pdb.gimp_edit_fill(layer, FOREGROUND_FILL)
    finally:
        gimp.context_pop()
    return layer


# --- Group/lock editing helpers ---


def ensure_metadata_layers(image):
    """Create group + lock layers with overlay-friendly settings if missing."""
    group_layer = None
    lock_layer = None

    # Create or reuse the group map layer with a slight overlay opacity.
    group_layer = find_layer_by_name(image, GROUP_LAYER_NAMES)
    if group_layer is None:
        group_layer = ensure_metadata_layer(image, "GRIN_GROUPS", 55.0, MULTIPLY_MODE, 0)

    # Create or reuse the lock map layer with slightly stronger visibility.
    lock_layer = find_layer_by_name(image, LOCK_LAYER_NAMES)
    if lock_layer is None:
        lock_layer = ensure_metadata_layer(image, "GRIN_LOCK", 40.0, NORMAL_MODE, 0)

    return group_layer, lock_layer


def apply_group_lock_to_selection(image, group_layer, lock_layer, group_id, locked):
    """Paint the active selection into the group/lock metadata layers."""
    # Ensure a selection exists before painting metadata.
    if pdb.gimp_selection_is_empty(image):
        gimp.message("GRIN: No active selection to paint metadata.")
        return

    # Clamp the group value to the 0-15 range used by GRIN encoders.
    group_value = max(0, min(15, int(group_id)))
    lock_value = 255 if locked else 0

    gimp.context_push()
    try:
        # Fill the group map layer with the group value.
        gimp.set_foreground((group_value, group_value, group_value))
        pdb.gimp_image_set_active_layer(image, group_layer)
        pdb.gimp_edit_fill(group_layer, FOREGROUND_FILL)

        # Fill the lock map layer with the lock value.
        gimp.set_foreground((lock_value, lock_value, lock_value))
        pdb.gimp_image_set_active_layer(image, lock_layer)
        pdb.gimp_edit_fill(lock_layer, FOREGROUND_FILL)
    finally:
        gimp.context_pop()


# --- Pixel map extraction ---


def read_grayscale_values(layer):
    """Read grayscale pixel values from a layer into a flat list."""
    region = layer.get_pixel_rgn(0, 0, layer.width, layer.height, False, False)
    values = []

    # Iterate over rows to avoid allocating one massive buffer.
    for y in range(layer.height):
        row = region[0 : layer.width, y : y + 1]
        row_bytes = bytearray(row)
        # Extract the first channel for grayscale layers.
        for x in range(0, len(row_bytes), region.bpp):
            values.append(row_bytes[x])

    return values


def summarize_group_lock_maps(group_layer, lock_layer):
    """Summarize group and lock usage for preview output."""
    summary = {
        "totalPixels": 0,
        "groupCounts": {str(group_id): 0 for group_id in range(16)},
        "lockedPixels": 0,
        "unlockedPixels": 0,
    }

    if group_layer is None:
        return summary

    group_values = read_grayscale_values(group_layer)
    summary["totalPixels"] = len(group_values)

    # Count group IDs directly from the group map values.
    for value in group_values:
        group_id = value if value <= 15 else int(round(value / 17.0))
        group_id = max(0, min(15, group_id))
        summary["groupCounts"][str(group_id)] += 1

    if lock_layer is not None:
        lock_values = read_grayscale_values(lock_layer)
        # Treat values >= 128 as locked for preview purposes.
        for value in lock_values:
            if value >= 128:
                summary["lockedPixels"] += 1
            else:
                summary["unlockedPixels"] += 1

    return summary


# --- Export pipeline helpers ---


def export_layer_png(image, layer, output_path):
    """Export a specific layer to a PNG file using a temporary image."""
    # Build a temporary image to avoid mutating the working document.
    temp_image = pdb.gimp_image_new(layer.width, layer.height, image.base_type)
    temp_layer = pdb.gimp_layer_new_from_drawable(layer, temp_image)
    pdb.gimp_image_insert_layer(temp_image, temp_layer, None, 0)
    pdb.gimp_image_set_active_layer(temp_image, temp_layer)

    # Save the PNG with typical defaults.
    pdb.file_png_save2(
        temp_image,
        temp_layer,
        output_path,
        output_path,
        0,
        9,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
    )
    pdb.gimp_image_delete(temp_image)


def export_visible_png(image, output_path):
    """Export visible artwork to PNG by merging a temporary image copy."""
    temp_image = pdb.gimp_image_duplicate(image)
    merged_layer = pdb.gimp_image_merge_visible_layers(temp_image, CLIP_TO_IMAGE)
    pdb.file_png_save2(
        temp_image,
        merged_layer,
        output_path,
        output_path,
        0,
        9,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
    )
    pdb.gimp_image_delete(temp_image)


def write_rules_json(output_path):
    """Write a default rules JSON sidecar for GRIN encoding."""
    payload = {"tickMicros": 33333, "opcodeSetId": 0, "rules": []}
    with open(output_path, "w", encoding="utf-8") as handle:
        json.dump(payload, handle, indent=2)


# --- CLI invocation helpers ---


def run_command(command):
    """Run a CLI command and return stdout/stderr combined output."""
    result = subprocess.run(command, capture_output=True, text=True, check=False)
    output = result.stdout.strip() or result.stderr.strip()
    return output or "(no output)"


def build_encode_command(node_path, encode_script, rgba_path, grin_path, groups_path, rules_path):
    """Build the grin-encode CLI command for export guidance."""
    return [
        node_path,
        encode_script,
        rgba_path,
        grin_path,
        "--groups",
        groups_path,
        "--rules",
        rules_path,
    ]


def build_validate_command(node_path, validate_script, grin_path):
    """Build the grin-validate CLI command for validation guidance."""
    return [node_path, validate_script, grin_path]


# --- Main plugin entrypoint ---


def grin_gimp_export(
    image,
    drawable,
    group_id,
    locked,
    apply_to_selection,
    ensure_layers,
    export_assets,
    export_dir,
    base_name,
    run_encode,
    run_validate,
    node_path,
    encode_script,
    validate_script,
):
    """Entry point for the GRIN GIMP export workflow."""
    # Ensure metadata layers exist before editing or exporting.
    group_layer = None
    lock_layer = None
    if ensure_layers:
        group_layer, lock_layer = ensure_metadata_layers(image)
    else:
        group_layer = find_layer_by_name(image, GROUP_LAYER_NAMES)
        lock_layer = find_layer_by_name(image, LOCK_LAYER_NAMES)

    # Apply group/lock values into the metadata layers if requested.
    if apply_to_selection and group_layer and lock_layer:
        apply_group_lock_to_selection(image, group_layer, lock_layer, group_id, locked)

    # Summarize metadata for preview feedback.
    summary = summarize_group_lock_maps(group_layer, lock_layer)
    gimp.message(
        "GRIN Preview:\n"
        f"Total Pixels: {summary['totalPixels']}\n"
        f"Locked Pixels: {summary['lockedPixels']}\n"
        f"Unlocked Pixels: {summary['unlockedPixels']}\n"
        f"Group Counts: {summary['groupCounts']}"
    )

    if not export_assets:
        return

    # Prepare export directory and file names.
    os.makedirs(export_dir, exist_ok=True)
    base = base_name or image.name.replace(os.path.splitext(image.name)[1], "")
    rgba_path = os.path.join(export_dir, f"{base}.png")
    groups_path = os.path.join(export_dir, f"{base}.groups.png")
    lock_path = os.path.join(export_dir, f"{base}.lock.png")
    rules_path = os.path.join(export_dir, f"{base}.rules.json")
    grin_path = os.path.join(export_dir, f"{base}.grin")

    # Export the visible art and metadata layers.
    export_visible_png(image, rgba_path)
    if group_layer is not None:
        export_layer_png(image, group_layer, groups_path)
    if lock_layer is not None:
        export_layer_png(image, lock_layer, lock_path)
    write_rules_json(rules_path)

    # Optionally run the encoding step.
    if run_encode:
        encode_command = build_encode_command(
            node_path, encode_script, rgba_path, grin_path, groups_path, rules_path
        )
        encode_output = run_command(encode_command)
        gimp.message(f"grin-encode output:\n{encode_output}")

    # Optionally run the validation step.
    if run_validate:
        validate_command = build_validate_command(node_path, validate_script, grin_path)
        validate_output = run_command(validate_command)
        gimp.message(f"grin-validate output:\n{validate_output}")


register(
    "python-fu-grin-export",
    "Export GRIN metadata and assets",
    "Prepare GRIN group/lock maps, export PNGs, and run validation.",
    "GRIN Project",
    "GRIN Project",
    "2025",
    "GRIN Export...",
    "*",
    [
        (PF_INT, "group_id", "Active Group ID (0-15)", 0),
        (PF_BOOL, "locked", "Lock pixels", False),
        (PF_BOOL, "apply_to_selection", "Apply to selection", True),
        (PF_BOOL, "ensure_layers", "Ensure metadata layers", True),
        (PF_BOOL, "export_assets", "Export assets", True),
        (PF_DIRNAME, "export_dir", "Export directory", os.path.expanduser("~/grin-exports")),
        (PF_STRING, "base_name", "Base filename (blank uses image name)", ""),
        (PF_BOOL, "run_encode", "Run grin-encode", False),
        (PF_BOOL, "run_validate", "Run grin-validate", False),
        (PF_STRING, "node_path", "Node executable", "node"),
        (PF_STRING, "encode_script", "Path to grin-encode.js", "tools/bin/grin-encode.js"),
        (PF_STRING, "validate_script", "Path to grin-validate.js", "tools/bin/grin-validate.js"),
    ],
    [],
    grin_gimp_export,
    menu="<Image>/Filters/GRIN",
)

main()
