# SPDX-License-Identifier: EPL-2.0
"""Offline refusal and idempotence tests for source-pinned Eclipse parent POM."""
import tempfile
import unittest
from pathlib import Path

from prepare_eclipse_platform_parent import (
    validate_parent, validate_receiver, publish
)

NS = 'xmlns="http://maven.apache.org/POM/4.0.0"'
PARENT = (f'<project {NS}><groupId>org.eclipse</groupId>'
          '<artifactId>eclipse-platform-parent</artifactId>'
          '<version>4.42.0-SNAPSHOT</version><packaging>pom</packaging>'
          '<properties><tycho.version>5.0.4</tycho.version></properties>'
          '</project>').encode()
SWT = (f'<project {NS}><parent><groupId>org.eclipse</groupId>'
       '<artifactId>eclipse-platform-parent</artifactId>'
       '<version>4.42.0-SNAPSHOT</version>'
       '<relativePath>../eclipse-platform-parent</relativePath>'
       '</parent></project>').encode()


class OriginalEclipseParentSourceTest(unittest.TestCase):
    def test_maven_parent_and_swt_relative_path(self):
        validate_parent(PARENT)
        validate_receiver(SWT)

    def test_rejects_wrong_version_coordinates_and_tycho(self):
        for old, new in [
            (b'4.42.0-SNAPSHOT', b'4.41.0-SNAPSHOT'),
            (b'5.0.4', b'4.0.12'),
            (b'eclipse-platform-parent', b'other-parent'),
            (b'<packaging>pom</packaging>', b'<packaging>jar</packaging>'),
        ]:
            with self.subTest(new=new):
                with self.assertRaises(ValueError):
                    validate_parent(PARENT.replace(old, new, 1))

    def test_rejects_drifted_swt_parent(self):
        for old, new in [
            (b'4.42.0-SNAPSHOT', b'4.41.0-SNAPSHOT'),
            (b'../eclipse-platform-parent', b'../another-parent'),
        ]:
            with self.subTest(new=new):
                with self.assertRaises(ValueError):
                    validate_receiver(SWT.replace(old, new, 1))

    def test_publication_is_idempotent_and_never_overwrites(self):
        with tempfile.TemporaryDirectory() as root:
            destination = Path(root) / "eclipse-platform-parent" / "pom.xml"
            publish(destination, PARENT)
            publish(destination, PARENT)
            self.assertEqual(PARENT, destination.read_bytes())
            with self.assertRaises(ValueError):
                publish(destination, SWT)
            link = Path(root) / "parent-link.xml"
            link.symlink_to(destination)
            with self.assertRaises(ValueError):
                publish(link, PARENT)


if __name__ == "__main__":
    unittest.main()
