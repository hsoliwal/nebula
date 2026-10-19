# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
"""Synthetic tests of the REAL OpenCV validator; never native screenshot evidence.
Run: python test_grid_screenshot_oracles.py verify_grid_screenshots.py [--baseline-witness]
All synthetic PNG/JUnit fixtures and the validator's temporary receipts are deleted.
"""
from contextlib import redirect_stdout
import hashlib
import importlib.util
import io
import json
from pathlib import Path
import sys
import tempfile
import unittest
import xml.etree.ElementTree as ET

import cv2
import numpy as np

SCENES = ['01-top', '02-middle', '03-horizontal', '04-resized',
          '05-virtual-million-top', '06-virtual-million-middle',
          '07-virtual-million-bottom-horizontal', '08-virtual-million-return']
BOX = (20, 22, 24, 18)
MODULE = None


def load_validator(path):
    spec = importlib.util.spec_from_file_location('grid_validator_under_test', path)
    if spec is None or spec.loader is None:
        raise ValueError('validator must be an emitted Python file')
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def props(path, values):
    path.write_text(''.join(str(k) + '=' + str(v) + '\n' for k, v in values.items()), encoding='utf-8')


class GridVisionTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory(prefix='SYNTHETIC-grid-vision-')
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.frames = self.root / 'synthetic-frames'
        self.reports = self.root / 'synthetic-junit'
        self.frames.mkdir()
        self.reports.mkdir()
        self.fixture()

    def png(self, path, data):
        self.assertTrue(cv2.imwrite(str(path), data))
        return digest(path)

    def fixture(self):
        for report in self.reports.glob('*.xml'):
            report.unlink()
        classes = {
            'GridFixedColumn_Test': ['syntheticFixedColumn'],
            'GridViewportCoordinates_Test': ['syntheticCoordinates'],
            'GridGCProxy_Test': ['syntheticGraphics'],
            'GridVisibleRangeSupport_Test': [
                'testScreenCaptureFreshnessBoundsAndGraphicsLifetime',
                'testVisibleRangeTracksRenderedViewportAndProducesScreenshots',
                'testVirtualMillionRowScreenshotsStaySparseAcrossDistantViewport'],
        }
        suite = ET.Element('testsuite', name='SYNTHETIC_VALIDATOR_INPUT_NOT_JUNIT_EXECUTION')
        for cls, names in classes.items():
            for name in names:
                ET.SubElement(suite, 'testcase', classname='org.eclipse.nebula.widgets.grid.' + cls, name=name)
        ET.ElementTree(suite).write(self.reports / 'TEST-synthetic.xml', encoding='utf-8')
        for i, scene in enumerate(SCENES):
            width = 192 if i == 3 else 160
            data = np.full((120, width, 3), 245, dtype=np.uint8)
            cv2.line(data, (15 + i * 3, 18), (108 + i * 2, 84), (0, 0, 0), 3)
            cv2.rectangle(data, (22 + i * 2, 34), (54 + i * 2, 69), (50, 90, 130), 1)
            screen = self.frames / (scene + '-screen.png')
            control = self.frames / (scene + '-control.png')
            meta = {
                'fixtureScope': 'SYNTHETIC_VALIDATOR_TEST_ONLY',
                'screen.captureMethod': 'DISPLAY_COPY_AREA', 'captureMethod': 'CONTROL_PRINT',
                'screen.backend': 'x11', 'platform': 'gtk',
                'screen.screenshot': screen.name, 'screen.sha256': self.png(screen, data),
                'screenshot': control.name, 'screenshot.sha256': self.png(control, data),
                'screen.pixelHeight': 120, 'screen.pixelWidth': width,
                'visibleRows': 12, 'logicalRows': 1_000_000 if i >= 4 else 100,
                'materializedItems': 32, 'selectionCount': 3,
                'topIndex': 999_500 if i == 6 else 0 if i in (0, 4, 7) else 100,
            }
            props(self.frames / (scene + '.txt'), meta)
        red = np.full((80, 100, 3), 245, np.uint8)
        blue = red.copy()
        x, y, w, h = BOX
        red[y:y+h, x:x+w] = (0, 0, 255)
        blue[y:y+h, x:x+w] = (255, 0, 0)
        props(self.frames / 'screen-contract.properties', {
            'fixtureScope': 'SYNTHETIC_VALIDATOR_TEST_ONLY',
            'captureMethod': 'DISPLAY_COPY_AREA', 'successfulCaptures': 48,
            'writeFailures': 3, 'positiveLeakControls': 2, 'retainedGraphicsDelta': 0,
            'manualQaRequired': 'false', 'red.sha256': self.png(self.frames / 'capture-red.png', red),
            'blue.sha256': self.png(self.frames / 'capture-blue.png', blue),
            **{'marker.' + k: v for k, v in zip(('x', 'y', 'width', 'height'), BOX)},
        })

    def verify(self):
        with redirect_stdout(io.StringIO()):
            MODULE.verify(self.frames, self.reports)

    def rejected(self, message):
        receipt = self.frames / 'opencv-validation.json'
        receipt.write_text('{"status":"PASS","scope":"STALE_SYNTHETIC_CONTROL"}')
        with self.assertRaisesRegex(AssertionError, message):
            self.verify()
        self.assertFalse(receipt.exists(), 'a failed run must remove a stale PASS receipt')

    def set_meta(self, scene, **changes):
        path = self.frames / (scene + '.txt')
        values = MODULE.properties(path)
        values.update(changes)
        props(path, values)

    def replace_scene(self, scene, data, surface='screen'):
        meta = MODULE.properties(self.frames / (scene + '.txt'))
        name_key = 'screen.screenshot' if surface == 'screen' else 'screenshot'
        hash_key = 'screen.sha256' if surface == 'screen' else 'screenshot.sha256'
        self.set_meta(scene, **{hash_key: self.png(self.frames / meta[name_key], data)})

    def replace_canary(self, data):
        path = self.frames / 'screen-contract.properties'
        meta = MODULE.properties(path)
        meta['blue.sha256'] = self.png(self.frames / 'capture-blue.png', data)
        props(path, meta)

    def test_01_valid_eight_scene_fixture_and_existing_selftest(self):
        self.verify()
        receipt = json.loads((self.frames / 'opencv-validation.json').read_text())
        self.assertEqual(8, receipt['scenePngs'])
        self.assertEqual(8, receipt['controlPngs'])
        self.assertEqual(2, receipt['canaryPngs'])
        self.assertFalse(receipt['manualQaRequired'])
        with redirect_stdout(io.StringIO()):
            MODULE.selftest_control_oracles()

    def test_02_disconnected_clip_leak_is_rejected(self):
        blue = cv2.imread(str(self.frames / 'capture-blue.png'))
        blue[4, 4] = (255, 0, 0)
        self.replace_canary(blue)  # Rehash deliberately: test pixels, not hash mismatch.
        self.rejected('Blue repaint geometry oracle')

    def test_03_extra_canary_frame_extent_is_rejected(self):
        blue = cv2.imread(str(self.frames / 'capture-blue.png'))
        self.replace_canary(cv2.copyMakeBorder(blue, 0, 0, 0, 7, cv2.BORDER_CONSTANT, value=(245, 245, 245)))
        self.rejected('Canary frame dimensions')

    def test_04_exact_geometry_hole_shift_blank_and_stale(self):
        blue = cv2.imread(str(self.frames / 'capture-blue.png'))
        x, y, w, h = BOX
        hole = blue.copy(); hole[y + 2, x + 2] = 245
        adjacent = blue.copy(); adjacent[y:y+h, x+w] = (255, 0, 0)
        duplicate = blue.copy(); duplicate[1:4, 1:4] = (255, 0, 0)
        cases = {'hole': hole, 'adjacent-leak': adjacent, 'disjoint-leak': duplicate,
                 'shifted': np.roll(blue, 1, axis=1), 'cropped': blue[:, 1:],
                 'blank': np.zeros_like(blue), 'stale': cv2.imread(str(self.frames / 'capture-red.png'))}
        for name, data in cases.items():
            with self.subTest(name=name):
                self.assertFalse(MODULE.marker_matches(data, (255, 0, 0), BOX))

    def test_05_seeded_raster_membership_and_edge_touching(self):
        rng = np.random.default_rng(20261019)
        for i in range(300):
            height, width = (int(v) for v in rng.integers(8, 65, 2))
            x, y = int(rng.integers(width)), int(rng.integers(height))
            w, h = int(rng.integers(1, width-x+1)), int(rng.integers(1, height-y+1))
            color = (255, 0, 0) if i % 2 else (0, 0, 255)
            data = np.zeros((height, width, 3), np.uint8)
            data[y:y+h, x:x+w] = color
            self.assertTrue(MODULE.marker_matches(data, color, (x, y, w, h)))
            # Independent oracle: enumerate raster membership, not component labels.
            outside = [(r, c) for r in range(height) for c in range(width)
                       if not (x <= c < x+w and y <= r < y+h)]
            if outside:
                r, c = outside[int(rng.integers(len(outside)))]
                data[r, c] = color
                self.assertFalse(MODULE.marker_matches(data, color, (x, y, w, h)))
        edge = np.zeros((8, 9, 3), np.uint8); edge[6:8, 6:9] = (255, 0, 0)
        self.assertTrue(MODULE.marker_matches(edge, (255, 0, 0), (6, 6, 3, 2)))
        for box in [(-1, 6, 3, 2), (6, 6, 0, 2), (6, 6, 4, 2), (6, 6, 3, 3)]:
            self.assertFalse(MODULE.marker_matches(edge, (255, 0, 0), box))

    def test_06_missing_failed_skipped_and_duplicate_junit(self):
        for flaw in ['missing-class', 'missing-capture', 'missing-scenes', 'failure', 'error', 'skipped', 'duplicate']:
            with self.subTest(flaw=flaw):
                self.fixture()
                path = self.reports / 'TEST-synthetic.xml'
                tree = ET.parse(path); root = tree.getroot()
                if flaw == 'missing-class':
                    root.remove(root[0]); message = 'Missing required JUnit'
                elif flaw == 'missing-capture':
                    root.remove(root[3]); message = 'Missing capture-contract'
                elif flaw == 'missing-scenes':
                    root.remove(root[-1]); message = 'Missing scene-producing'
                elif flaw == 'duplicate':
                    ET.SubElement(root, 'testcase', **root[0].attrib); message = 'Duplicate JUnit'
                else:
                    ET.SubElement(root[0], flaw); message = 'Unqualified test'
                tree.write(path, encoding='utf-8')
                self.rejected(message)

    def test_07_native_method_backend_and_dimensions(self):
        for change, message in [({'screen.captureMethod': 'CONTROL_PRINT'}, 'native display'),
                                ({'screen.backend': 'wayland'}, 'Unqualified backend'),
                                ({'platform': 'win32'}, 'Unqualified backend'),
                                ({'screen.pixelWidth': 161}, 'Persisted dimensions'),
                                ({'captureMethod': 'DISPLAY_COPY_AREA'}, 'Control capture method')]:
            with self.subTest(change=change):
                self.fixture(); self.set_meta('01-top', **change); self.rejected(message)

    def test_08_blank_stale_and_cropped_surfaces(self):
        for surface in ['screen', 'control']:
            for defect in ['blank', 'stale', 'cropped']:
                with self.subTest(surface=surface, defect=defect):
                    self.fixture()
                    image = cv2.imread(str(self.frames / ('01-top-' + surface + '.png')))
                    replacement = np.zeros_like(image) if defect == 'blank' else image[:, 1:] if defect == 'cropped' else image
                    self.replace_scene('02-middle', replacement, surface)
                    self.rejected('Blank/unrendered|Stale |dimensions')

    def test_09_hash_and_decoding_failures(self):
        self.set_meta('01-top', **{'screen.sha256': '0' * 64}); self.rejected('PNG hash')
        self.fixture()
        path = self.frames / '01-top-screen.png'; path.write_bytes(b'not a PNG')
        self.set_meta('01-top', **{'screen.sha256': digest(path)}); self.rejected('PNG decode')

    def test_10_sparse_selection_and_return_contracts(self):
        cases = [({'logicalRows': 1000}, 'Sparse million-row'),
                 ({'materializedItems': 256}, 'Sparse million-row'),
                 ({'visibleRows': 0}, 'Unbounded row viewport'),
                 ({'topIndex': 17}, 'Distant lower viewport'),
                 ({'selectionCount': 0}, 'Distant selection')]
        for changes, message in cases:
            with self.subTest(changes=changes):
                self.fixture(); self.set_meta(SCENES[6], **changes); self.rejected(message)
        self.fixture(); self.set_meta(SCENES[7], topIndex=1); self.rejected('Logical origin')
        self.fixture(); self.set_meta(SCENES[7], selectionCount=0); self.rejected('Returning selection')

    def test_11_graphics_lifetime_and_receiver_binding(self):
        path = self.frames / 'screen-contract.properties'
        meta = MODULE.properties(path); meta['retainedGraphicsDelta'] = 1; props(path, meta)
        self.rejected('Capture ownership contract')
        self.fixture()
        expected = self.root / 'expected.json'
        expected.write_text(json.dumps({'gcClassSha256': '1'*64, 'sourceCommit': '2'*40}))
        props(self.frames / 'swt-receiver.properties', {'gcClassSha256': '0'*64, 'resource': 'bundleentry:/test/GC.class'})
        with self.assertRaisesRegex(AssertionError, 'Runtime SWT receiver mismatch'), redirect_stdout(io.StringIO()):
            MODULE.verify(self.frames, self.reports, expected)
        self.assertFalse((self.frames / 'opencv-validation.json').exists())

    def test_12_missing_capture_never_leaves_pass(self):
        path = self.frames / 'opencv-validation.json'; path.write_text('{"status":"PASS"}')
        (self.frames / 'capture-blue.png').unlink()
        with self.assertRaises(FileNotFoundError):
            self.verify()
        self.assertFalse(path.exists())


if __name__ == '__main__':
    if len(sys.argv) not in (2, 3) or (len(sys.argv) == 3 and sys.argv[2] != '--baseline-witness'):
        raise SystemExit(__doc__)
    MODULE = load_validator(Path(sys.argv[1]))
    names = ['test_02_disconnected_clip_leak_is_rejected', 'test_03_extra_canary_frame_extent_is_rejected']
    suite = (unittest.TestSuite(GridVisionTests(name) for name in names) if len(sys.argv) == 3
             else unittest.defaultTestLoader.loadTestsFromTestCase(GridVisionTests))
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    print(json.dumps({'scope': 'SYNTHETIC_OPENCV_VALIDATOR_TEST_ONLY', 'opencv': cv2.__version__,
                      'tests': result.testsRun, 'failures': len(result.failures), 'errors': len(result.errors),
                      'skipped': len(result.skipped), 'nativeSwtCapture': 'NOT_RUN'}))
    raise SystemExit(0 if result.wasSuccessful() and not result.skipped else 1)
