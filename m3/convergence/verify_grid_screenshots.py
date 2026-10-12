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


def qualify_control_surface(directory, meta, scene):
    """Qualify the independently captured SWT control pixels, not window chrome."""
    require(meta['captureMethod'] in ('CONTROL_PRINT', 'CONTROL_COPY_AREA'),
            'Control capture method: ' + scene)
    control = read_image(directory / meta['screenshot'], meta['screenshot.sha256'])
    require(control.shape[0] > 16 and control.shape[1] > 16,
            'Control capture dimensions: ' + scene)
    require(np.count_nonzero(cv2.Canny(control[8:-8, 8:-8], 80, 160)) > 20,
            'Blank/unrendered control surface: ' + scene)
    return control


def require_frame_change(previous, current, scene, surface):
    require(previous.shape == current.shape,
            surface + ' dimensions changed unexpectedly: ' + scene)
    require(np.count_nonzero(cv2.absdiff(previous, current)) > 20,
            'Stale ' + surface + ' scrolling: ' + scene)


def marker_matches(data, color, box):
    x, y, width, height = box
    mask = np.all(data == color, axis=2).astype(np.uint8)
    count, labels, stats, centers = cv2.connectedComponentsWithStats(mask, connectivity=8)
    return any(tuple(row) == (x, y, width, height, width * height) for row in stats[1:])


def verify(directory, reports, receiver=None):
    (directory / 'opencv-validation.json').unlink(missing_ok=True)
    expected_classes = {'GridFixedColumn_Test', 'GridVisibleRangeSupport_Test',
                        'GridViewportCoordinates_Test', 'GridGCProxy_Test'}
    cases = {}
    for xml in reports.glob('TEST-*.xml'):
        for case in ET.parse(xml).getroot().iter('testcase'):
            key = (case.get('classname', '').rsplit('.', 1)[-1], case.get('name'))
            if key[0] in expected_classes:
                require(key not in cases, 'Duplicate JUnit case: ' + str(key))
                require(not any(case.find(tag) is not None for tag in ('failure', 'error', 'skipped')),
                        'Unqualified test: ' + str(key))
                cases[key] = True
    require({key[0] for key in cases} == expected_classes, 'Missing required JUnit classes')
    require(('GridVisibleRangeSupport_Test', 'testScreenCaptureFreshnessBoundsAndGraphicsLifetime') in cases,
            'Missing capture-contract JUnit test')
    required_scenes = {'testVisibleRangeTracksRenderedViewportAndProducesScreenshots',
                       'testVirtualMillionRowScreenshotsStaySparseAcrossDistantViewport'}
    require(all(('GridVisibleRangeSupport_Test', name) in cases for name in required_scenes),
            'Missing scene-producing JUnit tests')
    scenes = ['01-top', '02-middle', '03-horizontal', '04-resized',
              '05-virtual-million-top', '06-virtual-million-middle',
              '07-virtual-million-bottom-horizontal', '08-virtual-million-return']
    images, control_images, metadata = {}, {}, {}
    for scene in scenes:
        meta = properties(directory / (scene + '.txt'))
        require(meta['screen.captureMethod'] == 'DISPLAY_COPY_AREA', 'Must use native display pixels')
        require(meta['screen.backend'] == 'x11' and meta['platform'] == 'gtk', 'Unqualified backend')
        require(meta['screen.screenshot'] == scene + '-screen.png', 'Unexpected screen artifact path')
        image = read_image(directory / meta['screen.screenshot'], meta['screen.sha256'])
        # Screen-only proof could accept static window chrome while the Grid
        # itself is blank or stale. Validate the second captured surface too.
        control = qualify_control_surface(directory, meta, scene)
        require(image.shape[:2] == (int(meta['screen.pixelHeight']), int(meta['screen.pixelWidth'])),
                'Persisted dimensions: ' + scene)
        # Window borders and scrollbars alone cannot qualify widget content.
        interior = image[8:-20, 8:-20]
        require(interior.size > 0 and np.count_nonzero(cv2.Canny(interior, 80, 160)) > 20,
                'Blank/unrendered scene: ' + scene)
        require(0 < int(meta['visibleRows']) < int(meta['logicalRows']), 'Unbounded row viewport')
        if scene.startswith(('05-', '06-', '07-', '08-')):
            require(int(meta['logicalRows']) == 1_000_000 and int(meta['materializedItems']) < 256,
                    'Sparse million-row gate')
        images[scene], control_images[scene], metadata[scene] = image, control, meta
    require(int(metadata['07-virtual-million-bottom-horizontal']['topIndex']) >= 999_400,
            'Distant lower viewport not reached')
    require(int(metadata['07-virtual-million-bottom-horizontal']['selectionCount']) >= 3,
            'Distant selection lost')
    require(int(metadata['08-virtual-million-return']['topIndex']) == 0,
            'Logical origin not restored')
    require(int(metadata['08-virtual-million-return']['selectionCount']) >= 3,
            'Returning selection lost')
    for before, after in [('01-top', '02-middle'), ('02-middle', '03-horizontal'),
                          ('05-virtual-million-top', '06-virtual-million-middle'),
                          ('06-virtual-million-middle', '07-virtual-million-bottom-horizontal'),
                          ('07-virtual-million-bottom-horizontal', '08-virtual-million-return')]:
        require_frame_change(images[before], images[after], after, 'screen')
        require_frame_change(control_images[before], control_images[after], after,
                             'control-surface')
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
    receiver_proof = {'status': 'NOT_REQUESTED'}
    if receiver is not None:
        expected = json.loads(receiver.read_text())
        actual = properties(directory / 'swt-receiver.properties')
        require(actual['gcClassSha256'] == expected['gcClassSha256'], 'Runtime SWT receiver mismatch')
        require(actual['resource'].startswith(('bundleresource:', 'bundleentry:')), 'Expected Tycho OSGi receiver')
        receiver_proof = {'status': 'PASS', 'sourceCommit': expected['sourceCommit'],
                          'gcClassSha256': actual['gcClassSha256'], 'resource': actual['resource']}
    receipt = {'status': 'PASS' , 'junitTests': len(cases), 'scenePngs': len(scenes),
               'controlPngs': len(control_images),
               'canaryPngs': 2, 'swtReceiver': receiver_proof, 'rejectedControls': list(defects), 'graphicsLifetime': contract,
               'opencv': cv2.__version__, 'numpy': np.__version__, 'manualQaRequired': False,
               'scope': 'GTK3/X11 Grid capture and behavior; not full Tycho, JFace or platform qualification'}
    (directory / 'opencv-validation.json').write_text(json.dumps(receipt, indent=2) + '\n')
    print(json.dumps(receipt, indent=2))


def selftest_control_oracles():
    """Synthetic negative controls; not a substitute for native GTK/JUnit proof."""
    from tempfile import TemporaryDirectory

    with TemporaryDirectory(prefix='grid-control-oracle-') as root:
        directory = Path(root)
        first = np.full((96, 128, 3), 255, dtype=np.uint8)
        second = first.copy()
        cv2.line(first, (16, 18), (98, 69), (0, 0, 0), 3)
        cv2.line(second, (22, 18), (113, 67), (0, 0, 0), 3)
        blank = np.zeros_like(first)
        for name, image in (('first', first), ('second', second), ('blank', blank)):
            require(cv2.imwrite(str(directory / (name + '.png')), image),
                    'Synthetic fixture write: ' + name)

        def evidence(name, method):
            path = directory / (name + '.png')
            return {
                'captureMethod': method,
                'screenshot': path.name,
                'screenshot.sha256': hashlib.sha256(path.read_bytes()).hexdigest(),
            }

        original = evidence('first', 'CONTROL_PRINT')
        updated = evidence('second', 'CONTROL_COPY_AREA')
        accepted_first = qualify_control_surface(directory, original, 'first')
        accepted_second = qualify_control_surface(directory, updated, 'second')
        require_frame_change(accepted_first, accepted_second, 'changed', 'control-surface')

        def must_reject(name, action):
            try:
                action()
            except AssertionError:
                return
            raise AssertionError('Synthetic defect admitted: ' + name)

        must_reject('wrong-control-method',
                    lambda: qualify_control_surface(directory,
                        dict(original, captureMethod='DISPLAY_COPY_AREA'), 'wrong-method'))
        must_reject('wrong-control-hash',
                    lambda: qualify_control_surface(directory,
                        dict(original, **{'screenshot.sha256': '0' * 64}), 'bad-hash'))
        must_reject('blank-control',
                    lambda: qualify_control_surface(directory,
                        evidence('blank', 'CONTROL_PRINT'), 'blank'))
        must_reject('stale-control',
                    lambda: require_frame_change(accepted_first, accepted_first,
                        'same', 'control-surface'))
        must_reject('mismatched-control-dimensions',
                    lambda: require_frame_change(accepted_first, accepted_second[:80],
                        'cropped', 'control-surface'))
    print('SYNTHETIC_GRID_CONTROL_ORACLE_PASS: 2 valid paths; 5 defects rejected')


if __name__ == '__main__':
    if len(sys.argv) == 2 and sys.argv[1] == '--selftest':
        selftest_control_oracles()
    else:
        require(len(sys.argv) in (3, 4), 'Usage: verify_grid_screenshots.py SCREENSHOT_DIR JUNIT_REPORT_DIR [RECEIVER_EXPECTED_JSON] | --selftest')
        verify(Path(sys.argv[1]), Path(sys.argv[2]), Path(sys.argv[3]) if len(sys.argv) == 4 else None)
