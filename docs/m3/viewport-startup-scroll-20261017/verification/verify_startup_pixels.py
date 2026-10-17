#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Validate the focused startup frames with Nebula's existing OpenCV primitives.

This reuses the persisted PNG decoder/hash/change checks. It does not claim the
eight-scene Grid contract, retained graphics/resource gate, or other platforms.
"""
import hashlib
import importlib.util
import json
from pathlib import Path
import sys

evidence, existing_validator = map(Path, sys.argv[1:])
spec = importlib.util.spec_from_file_location("nebula_existing_pixels", existing_validator)
validator = importlib.util.module_from_spec(spec)
spec.loader.exec_module(validator)
sha = lambda path: hashlib.sha256(path.read_bytes()).hexdigest()
frames = {}
result = {"scope": "Focused full-control startup pairs; not the complete eight-scene/resource gate",
          "capture": "DISPLAY_COPY_AREA asserted by each source-bound native JUnit capture",
          "crop": None, "masks": [], "GTK_OVERLAY_SCROLLING": "0",
          "driver_sha256": sha(Path(__file__)), "reused_validator_sha256": sha(existing_validator),
          "opencv": validator.cv2.__version__, "numpy": validator.np.__version__, "frames": {}, "pairs": {}}
for lane in ("startup-before", "startup-default", "startup-original"):
    pair = []
    for filename in ("top-index-first-paint.png", "top-index-settled-request.png"):
        path = evidence / lane / filename
        expected_hash = sha(path)
        pixels = validator.read_image(path, expected_hash)
        validator.require(pixels.shape == (280, 390, 3), "Unexpected complete native Grid bounds")
        edges = int(validator.np.count_nonzero(validator.cv2.Canny(pixels[8:-20, 8:-20], 80, 160)))
        validator.require(edges > 20, "Blank/unrendered Grid interior")
        result["frames"][lane + "/" + filename] = {"sha256": expected_hash, "width": 390,
            "height": 280, "bytes": path.stat().st_size, "interior_edge_pixels": edges}
        pair.append(pixels)
    frames[lane] = pair
    changed = int(validator.np.count_nonzero(validator.np.any(pair[0] != pair[1], axis=2)))
    result["pairs"][lane] = {"different_pixels": changed, "total_pixels": 390 * 280}
    validator.require(changed > 20 if lane == "startup-before" else changed == 0,
                      "Unexpected before/after startup pixel oracle: " + lane)
validator.require_frame_change(frames["startup-before"][0], frames["startup-default"][0],
                               "startup repair", "complete native Grid")
validator.require(validator.np.array_equal(frames["startup-before"][1], frames["startup-default"][0]),
                  "Repaired initial viewport differs from the settled intended-scroll reference")
result["repaired_first_frame_matches_baseline_settled_reference"] = True
result["status"] = "PASSED_FOCUSED_PIXEL_GATE"
output = evidence / "startup-pixels.json"
with output.open("x") as target:
    target.write(json.dumps(result, indent=2) + "\n")
print(json.dumps({"status": result["status"], "pairs": result["pairs"], "output": str(output)}, sort_keys=True))
