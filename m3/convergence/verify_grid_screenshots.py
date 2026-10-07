# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
"""Automated persisted-pixel qualification for the native Grid capture lane."""
from pathlib import Path
import hashlib
import json
import sys
import xml.etree.ElementTree as ET

import cv2
import numpy as np


def require(condition, message):
    if not condition:
        raise AssertionError(message)


def properties(path):
    return dict(line.split('=', 1) for line in path.read_text().splitlines() if '=' in line)


def read_image(path, expected_hash):
    require(hashlib.sha256(path.read_bytes()).hexdigest() == expected_hash, 'PNG hash: ' + path.name)
    data = cv2.imread(str(path), cv2.IMREAD_COLOR)
    require(data is not None and data.size > 0, 'PNG decode: ' + path.name)
    return data


def marker_matches(data, color, box):
    x, y, width, height = box
    mask = np.all(data == color, axis=2).astype(np.uint8)
    count, labels, stats, centers = cv2.connectedComponentsWithStats(mask, connectivity=8)
    return any(tuple(row) == (x, y, width, height, width * height) for row in stats[1:])


def verify(directory, reports):
    expected_classes = {'GridFixedColumn_Test', 'GridVisibleRangeSupport_Test',
                        'GridViewportCoordinates_Test', 'GridGCProxy_Test'}
    cases = {}
    for xml in reports.glob('TEST-*.xml'):
        for case in ET.parse(xml).getroot().iter('testcase'):
            key = (case.get('classname', '').rsplit('.', 1)[-1], case.get('name'))
            if key[0] in expected_classes:
                require(not any(case.find(tag) is not None for tag in ('failure', 'error', 'skipped')),
                        'Unqualified test: ' + str(key))
                cases[key] = True
    require({key[0] for key in cases} == expected_classes, 'Missing required JUnit classes')
    require(('GridVisibleRangeSupport_Test', 'testScreenCaptureFreshnessBoundsAndGraphicsLifetime') in cases,
            'Missing capture-contract JUnit test')
    scenes = ['01-top', '02-middle', '03-horizontal', '04-resized',
              '05-virtual-million-top', '06-virtual-million-middle']
    images, metadata = {}, {}
    for scene in scenes:
        meta = properties(directory / (scene + '.txt'))
        require(meta['screen.captureMethod'] == 'DISPLAY_COPY_AREA', 'Must use native display pixels')
        require(meta['screen.backend'] == 'x11' and meta['platform'] == 'gtk', 'Unqualified backend')
        image = read_image(directory / meta['screen.screenshot'], meta['screen.sha256'])
        require(image.shape[:2] == (int(meta['screen.pixelHeight']), int(meta['screen.pixelWidth'])),
                'Persisted dimensions: ' + scene)
        # Window borders and scrollbars alone cannot qualify widget content.
        interior = image[8:-20, 8:-20]
        require(interior.size > 0 and np.count_nonzero(cv2.Canny(interior, 80, 160)) > 20,
                'Blank/unrendered scene: ' + scene)
        require(0 < int(meta['visibleRows']) < int(meta['logicalRows']), 'Unbounded row viewport')
        if scene.startswith(('05-', '06-')):
            require(int(meta['logicalRows']) == 1_000_000 and int(meta['materializedItems']) < 256,
                    'Sparse million-row gate')
        images[scene], metadata[scene] = image, meta
    for before, after in [('01-top', '02-middle'), ('02-middle', '03-horizontal'),
                          ('05-virtual-million-top', '06-virtual-million-middle')]:
        require(np.count_nonzero(cv2.absdiff(images[before], images[after])) > 20,
                'Stale scrolling: ' + after)
    require(images['04-resized'].shape[1] > images['01-top'].shape[1], 'Resize did not change width')
    contract = properties(directory / 'screen-contract.properties')
    for key, expected in {'captureMethod': 'DISPLAY_COPY_AREA', 'successfulCaptures': '48',
                          'writeFailures': '3', 'positiveLeakControls': '2',
                          'retainedGraphicsDelta': '0', 'manualQaRequired': 'false'}.items():
        require(contract[key] == expected, 'Capture ownership contract: ' + key)
    red = read_image(directory / 'capture-red.png', contract['red.sha256'])
    blue = read_image(directory / 'capture-blue.png', contract['blue.sha256'])
    box = tuple(int(contract['marker.' + key]) for key in ('x', 'y', 'width', 'height'))
    require(marker_matches(red, (0, 0, 255), box), 'Red geometry oracle')
    require(marker_matches(blue, (255, 0, 0), box), 'Blue repaint geometry oracle')
    x, y, width, height = box
    require(np.count_nonzero(cv2.absdiff(red[y:y+height, x:x+width], blue[y:y+height, x:x+width]))
            == width * height * 2, 'Exact red-to-blue repaint')
    # Defects are derived from the accepted frame, never accepted as replacements.
    missing = blue.copy(); missing[y:y+height, x:x+width] = 0
    leak = blue.copy(); leak[y:y+height, x+width:x+width+1] = (255, 0, 0)
    defects = {'blank': np.zeros_like(blue), 'stale': red,
               'shifted': np.roll(blue, 1, axis=1), 'cropped': blue[:, 1:],
               'missing-marker': missing, 'clip-leak': leak}
    for name, data in defects.items():
        require(not marker_matches(data, (255, 0, 0), box), 'Observer admitted ' + name)
    receipt = {'status': 'PASS', 'junitTests': len(cases), 'scenePngs': len(scenes),
               'canaryPngs': 2, 'rejectedControls': list(defects), 'graphicsLifetime': contract,
               'opencv': cv2.__version__, 'numpy': np.__version__, 'manualQaRequired': False,
               'scope': 'GTK3/X11 Grid capture and behavior; not full Tycho, JFace or platform qualification'}
    (directory / 'opencv-validation.json').write_text(json.dumps(receipt, indent=2) + '\n')
    print(json.dumps(receipt, indent=2))


if __name__ == '__main__':
    require(len(sys.argv) == 3, 'Usage: verify_grid_screenshots.py SCREENSHOT_DIR JUNIT_REPORT_DIR')
    verify(Path(sys.argv[1]), Path(sys.argv[2]))
