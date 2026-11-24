#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
"""Synthetic evidence tamper tests; these never claim native SWT execution."""
import importlib.util
import json
import tempfile
import unittest
from pathlib import Path
from unittest import mock
from xml.etree import ElementTree as ET
from zipfile import ZipFile

HERE = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("host", HERE / "verify_gtk_host_tests.py")
host = importlib.util.module_from_spec(spec)
spec.loader.exec_module(host)


class GtkHostProofTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.source = self.root / ".m3-swt"
        self.target = self.root / "target"
        self.proof = self.target / "binding/gtk-host-tests"
        self.native = self.source / host.NATIVE_ROOT
        self.native.mkdir(parents=True)
        self.xml = self.source / host.TEST_ROOT / "target/surefire-reports"
        self.xml.mkdir(parents=True)
        (self.proof / "native").mkdir(parents=True)
        jar = self.target / "repository/plugins" / host.GTK_JAR
        jar.parent.mkdir(parents=True)
        native_hashes = {}
        lines = []
        for prefix in host.NATIVE_PREFIXES:
            path = self.native / (prefix + "4975r24.so")
            path.write_bytes(b"\x7fELFsynthetic fixture " + prefix.encode())
            native_hashes[path.name] = host.digest(path.read_bytes())
            lines.append("[0.123s][info][library] Loaded library " + str(path) + ", handle 0x1234")
        with ZipFile(jar, "w") as archive:
            for path in sorted(self.native.glob("*.so")):
                archive.writestr(path.name, path.read_bytes())
        (self.proof / "native/native-library-123.log").write_text("\n".join(lines) + "\n")
        (self.target / "binding/expected.json").write_text(json.dumps({
            "sourceCommit": host.PIN, "sourceWorktreeHead": host.PIN,
            "bundleSha256": {host.GTK_JAR: host.digest(jar.read_bytes())},
            "nativeSha256": native_hashes}))
        (self.proof / "original-sparse-coverage.json").write_text(json.dumps({
            "schema": "SWT_M3_JUNIT_EXECUTION_V1", "passed": True}))
        for name in sorted(host.REQUIRED | host.NATIVE_CLASSES):
            self.write_report(name, sorted(host.GTK_CASES) if name == "Test_ViewportBoundsGtk" else ["nativeAssertion"])
            methods = sorted(host.GTK_CASES) if name == "Test_ViewportBoundsGtk" else ["nativeAssertion"]
            self.write_java(name, "public class " + name + " {\n" +
                            "\n".join("@Test public void " + method + "() {}" for method in methods) + "\n}\n")
        self.write_java("AllTests", "@Suite @SelectClasses({" + ", ".join(
            name + ".class" for name in sorted(host.REQUIRED | host.NATIVE_CLASSES)) + "}) public class AllTests {}\n")

    def write_java(self, name, text):
        path = self.source / host.JAVA_ROOT / Path(*host.PACKAGE.split(".")) / (name + ".java")
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text("package " + host.PACKAGE + ";\n" + text)
        return path

    def add_selected_class(self, name):
        path = self.source / host.JAVA_ROOT / Path(*host.PACKAGE.split(".")) / "AllTests.java"
        path.write_text(path.read_text().replace("@SelectClasses({", "@SelectClasses({" + name + ".class, "))

    def write_report(self, name, names, status=None, message=None):
        tree = ET.Element("testsuite", {"tests": str(len(names)), "failures": "0", "errors": "0", "skipped": "0"})
        props = ET.SubElement(tree, "properties")
        for key, value in {"swt.library.path": str(self.native), "os.name": "Linux", "os.arch": "amd64"}.items():
            ET.SubElement(props, "property", {"name": key, "value": value})
        for name_value in names:
            case = ET.SubElement(tree, "testcase", {"classname": "org.eclipse.swt.tests.junit." + name, "name": name_value})
            if status:
                ET.SubElement(case, status, {"message": message or "fixture"})
        if status:
            tree.set({"failure": "failures", "error": "errors", "skipped": "skipped"}[status], str(len(names)))
        path = self.xml / ("TEST-" + name + ".xml")
        path.write_text(ET.tostring(tree, encoding="unicode"))
        return path

    def capture(self):
        # These mocks represent Git source admission only; report/native checking
        # executes the actual helper against independent synthetic evidence.
        def git_output(args, **kwargs):
            return host.PIN + "\n" if "rev-parse" in args else b""
        with mock.patch.object(host.subprocess, "check_output", side_effect=git_output), \
                mock.patch.object(host.subprocess, "run"):
            return host.capture(self.source, self.target)

    def mutate_receipt(self, mutator):
        path = self.proof / "receipt.json"
        value = json.loads(path.read_text())
        mutator(value)
        path.write_text(json.dumps(value))

    def test_complete_reports_and_actual_load_format_bind_to_receiver(self):
        result = self.capture()
        self.assertEqual(result["passed"], 9)
        self.assertEqual(result["permittedSkips"], 0)
        self.assertEqual(result["nonHostPlatforms"], "NOT_RUN")
        self.assertEqual(result["terminalVncRuntime"], "PENDING")
        self.assertEqual(host.verify(self.target), result)

    def test_no_original_reports_fail(self):
        for path in self.xml.glob("*.xml"):
            path.unlink()
        with self.assertRaises(ValueError):
            self.capture()

    def test_missing_required_original_class_fails(self):
        (self.xml / "TEST-Test_ItemBitSetMaximumPosition.xml").unlink()
        with self.assertRaises(ValueError):
            self.capture()

    def test_missing_one_gtk_native_assertion_fails(self):
        self.write_report("Test_ViewportBoundsGtk", sorted(host.GTK_CASES)[:-1])
        with self.assertRaises(ValueError):
            self.capture()

    def test_failures_and_errors_are_not_qualification(self):
        for kind in ("failure", "error"):
            with self.subTest(kind=kind):
                self.write_report("Test_org_eclipse_swt_graphics_GC", ["nativeAssertion"], kind)
                reports = self.proof / "reports"
                reports.mkdir(exist_ok=True)
                for path in self.xml.glob("*.xml"):
                    (reports / path.name).write_bytes(path.read_bytes())
                with self.assertRaises(ValueError):
                    host.report_summary(self.proof, str(self.native), [], self.source)

    def test_skipped_required_native_assertions_fail(self):
        self.write_report("Test_ViewportBoundsGtk", sorted(host.GTK_CASES), "skipped", "GTK3 native viewport producer qualification")
        with self.assertRaises(ValueError):
            self.capture()

    def test_unknown_skip_fails(self):
        self.write_report("OptionalTest", ["optional"], "skipped", "native unavailable")
        path = self.source / host.TEST_ROOT / "JUnit Tests/org/eclipse/swt/tests/junit/OptionalTest.java"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text("class OptionalTest { void optional() {} }\n")
        self.add_selected_class("OptionalTest")
        with self.assertRaises(ValueError):
            self.capture()

    def test_explicit_original_opt_in_skip_is_retained_and_bound(self):
        self.write_report("ViewportResourceRegressionTest", ["resource"], "skipped", "Assumption failed: Dedicated resource gate is opt-in")
        path = self.source / host.TEST_ROOT / "JUnit Tests/org/eclipse/swt/tests/junit/ViewportResourceRegressionTest.java"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text('class ViewportResourceRegressionTest {\n'
                        '@BeforeAll static void resourceGateEnabled() { assumeTrue(enabled, "Dedicated resource gate is opt-in"); }\n'
                        '@Test void resource() {}\n}\n')
        self.add_selected_class("ViewportResourceRegressionTest")
        result = self.capture()
        self.assertEqual(result["permittedSkips"], 1)
        receipt = json.loads((self.proof / "receipt.json").read_text())
        self.assertEqual(receipt["reports"]["permittedSkips"][0]["sourceGitBlob"], host.blob(path.read_bytes()))
        self.mutate_receipt(lambda value: value["reports"].update(permittedSkips=[]))
        with self.assertRaises(ValueError):
            host.verify(self.target)

    def test_report_without_jvm_native_loads_fails(self):
        (self.proof / "native/native-library-123.log").write_text("No libraries loaded\n")
        with self.assertRaises(ValueError):
            self.capture()

    def test_load_from_preinstalled_native_fails(self):
        path = self.proof / "native/native-library-123.log"
        path.write_text(path.read_text().replace(str(self.native), "/usr/lib"))
        with self.assertRaises(ValueError):
            self.capture()

    def test_native_bytes_different_from_receiver_fail(self):
        next(self.native.glob("*.so")).write_bytes(b"different")
        with self.assertRaises(ValueError):
            self.capture()

    def test_raw_xml_tamper_fails_cache_reuse(self):
        self.capture()
        path = next((self.proof / "reports").glob("*.xml"))
        path.write_bytes(path.read_bytes() + b"\n")
        with self.assertRaises(ValueError):
            host.verify(self.target)

    def test_raw_native_log_tamper_fails_cache_reuse(self):
        self.capture()
        path = self.proof / "native/native-library-123.log"
        path.write_bytes(path.read_bytes() + b"\n")
        with self.assertRaises(ValueError):
            host.verify(self.target)

    def test_source_pin_and_gtk_bundle_identity_are_independent_gates(self):
        self.capture()
        for key, value in (("sourceCommit", "0" * 40), ("gtkBundleSha256", "0" * 64)):
            with self.subTest(key=key):
                path = self.proof / "receipt.json"
                original = path.read_text()
                self.mutate_receipt(lambda data: data.update({key: value}))
                with self.assertRaises(ValueError):
                    host.verify(self.target)
                path.write_text(original)

    def test_report_counts_cannot_claim_absent_cases(self):
        path = next(self.xml.glob("*.xml"))
        path.write_text(path.read_text().replace('tests="1"', 'tests="99"'))
        with self.assertRaises(ValueError):
            self.capture()

    def test_changed_receiver_and_missing_old_cache_proof_fail(self):
        with self.assertRaises((ValueError, FileNotFoundError)):
            host.verify(self.target)
        self.capture()
        (self.target / "repository/plugins" / host.GTK_JAR).write_bytes(b"other receiver")
        with self.assertRaises(ValueError):
            host.verify(self.target)

    def test_linked_raw_evidence_fails(self):
        self.capture()
        path = next((self.proof / "reports").glob("*.xml"))
        original = self.root / "outside.xml"
        original.write_bytes(path.read_bytes())
        path.unlink()
        path.symlink_to(original)
        with self.assertRaises(ValueError):
            host.verify(self.target)

    def test_p2_may_rename_the_exact_gtk_jar(self):
        jar = self.target / "repository/plugins" / host.GTK_JAR
        jar.rename(jar.with_name("org.eclipse.swt.gtk.linux.x86_64_3.138.0.v20261112-2300.jar"))
        self.assertEqual(self.capture()["gtkHostAllTests"], "PASS")

    def test_duplicate_matching_gtk_jars_are_ambiguous(self):
        jar = self.target / "repository/plugins" / host.GTK_JAR
        jar.with_name("duplicate.jar").write_bytes(jar.read_bytes())
        with self.assertRaises(ValueError):
            self.capture()

    def test_receipt_native_digest_must_match_actual_jar_entry(self):
        path = self.target / "binding/expected.json"
        expected = json.loads(path.read_text())
        expected["nativeSha256"][next(iter(expected["nativeSha256"]))] = "0" * 64
        path.write_text(json.dumps(expected))
        with self.assertRaises(ValueError):
            self.capture()

    def test_all_required_native_classes_cannot_hide_another_missing_suite_leaf(self):
        self.add_selected_class("AnotherOriginalTest")
        self.write_java("AnotherOriginalTest", "class AnotherOriginalTest { @Test void original() {} }")
        with self.assertRaisesRegex(ValueError, "selected leaf classes are missing"):
            self.capture()

    def test_report_cannot_substitute_an_unregistered_class(self):
        self.write_report("UnregisteredTest", ["notSelected"])
        with self.assertRaisesRegex(ValueError, "outside original AllTests selection"):
            self.capture()


policy = host


class SourcePolicyTest(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.source = Path(temporary.name)

    def write(self, name, content):
        path = self.source / policy.JAVA_ROOT / Path(*policy.PACKAGE.split('.')) / (name + '.java')
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text('package ' + policy.PACKAGE + ';\n' + content)
        return path

    def skip(self, name='Example', method='testA', reason='Assumption failed: original condition'):
        return policy.permitted_skip(self.source, policy.PACKAGE + '.' + name, method, reason)

    def test_recursive_wrapped_suite_inventory_has_original_hashes(self):
        self.write('AllTests', '@Suite @SelectClasses({One.class, AllNested.class}) public class AllTests {}')
        self.write('AllNested', '''@Wrapped public class AllNested {
            @Suite @SelectClasses({Two.class, One.class}) @interface Wrapped {}
        }''')
        one = self.write('One', 'public class One {}')
        self.write('Two', 'public class Two {}')
        result = policy.selected_leaf_classes(self.source)
        self.assertEqual([row['class'] for row in result], [policy.PACKAGE + '.One', policy.PACKAGE + '.Two'])
        self.assertEqual(result[0]['sourceGitBlob'], policy._blob(one.read_bytes()))
        self.assertEqual([row['class'] for row in policy.selected_suite_sources(self.source)],
                         [policy.PACKAGE + '.AllNested', policy.PACKAGE + '.AllTests'])

    def test_comments_and_string_literals_do_not_select_classes(self):
        self.write('AllTests', '''// @SelectClasses(Fake.class)
            @Suite @SelectClasses({One.class /* Fake.class */}) public class AllTests {}''')
        self.write('One', 'public class One { String value = "@SelectClasses(Fake.class)"; }')
        self.assertEqual(len(policy.selected_leaf_classes(self.source)), 1)

    def test_unrelated_java_text_blocks_are_masked(self):
        self.write('AllTests', '@SelectClasses(One.class) public class AllTests {}')
        self.write('One', '''public class One { String value = """
            @SelectClasses(Fake.class) { // not source syntax
            """; @Test void testA() {}
        }''')
        self.assertEqual(len(policy.selected_leaf_classes(self.source)), 1)
        self.assertIn('testA', policy._methods(policy._read(self.source, policy.PACKAGE + '.One')))
        self.write('One', 'public class One { String value = """ unfinished; }')
        with self.assertRaises(ValueError): policy.selected_leaf_classes(self.source)

    def test_unknown_suite_selector_and_cycles_refused(self):
        self.write('AllTests', '@Suite @SelectPackages("other") public class AllTests {}')
        with self.assertRaises(ValueError): policy.selected_leaf_classes(self.source)
        self.write('AllTests', '@SelectClasses(AllTests.class) public class AllTests {}')
        with self.assertRaises(ValueError): policy.selected_leaf_classes(self.source)

    def test_additional_selector_is_not_silently_dropped(self):
        self.write('AllTests', '@SelectClasses(One.class) @SelectMethod("Two#test") public class AllTests {}')
        self.write('One', 'public class One {}')
        with self.assertRaises(ValueError): policy.selected_leaf_classes(self.source)

    def test_direct_method_assumption_is_bound_to_method(self):
        self.write('Example', '''public class Example {
            @Test public void testA() { assumeTrue(condition, "original condition"); }
            @Test public void testB() { other(); }
        }''')
        self.assertEqual(self.skip()['conditionMethod'], 'testA')
        with self.assertRaises(ValueError): self.skip(method='testB')

    def test_matching_unrelated_literal_does_not_authorize_skip(self):
        self.write('Example', '''public class Example {
            @Test public void testA() {
                String text = "original condition";
                assumeTrue(condition, "other reason");
            }
        }''')
        with self.assertRaises(ValueError): self.skip()

    def test_matching_nested_predicate_literal_does_not_authorize_skip(self):
        self.write('Example', '''public class Example {
            @Test public void testA() { assumeTrue(check("original condition"), "other reason"); }
        }''')
        with self.assertRaises(ValueError): self.skip()

    def test_assume_that_actual_value_is_not_a_message(self):
        self.write('Example', '''public class Example {
            @Test public void testA() { assumeThat("original condition", matcher); }
        }''')
        with self.assertRaises(ValueError): self.skip()
        self.write('Example', '''public class Example {
            @Test public void testA() { assumeThat("original condition", actual, matcher); }
        }''')
        self.assertEqual(self.skip()['conditionMethod'], 'testA')

    def test_disabled_method_reason_not_borrowed(self):
        self.write('Example', '''public class Example {
            @Disabled("original condition") @Test public void testA() {}
            @Test public void testB() {}
        }''')
        self.assertEqual(self.skip(reason='original condition')['conditionMethod'], 'testA')
        with self.assertRaises(ValueError): self.skip(method='testB', reason='original condition')

    def test_os_condition_is_exact_and_method_scoped(self):
        self.write('Example', '''public class Example {
            @DisabledOnOs(value = {OS.LINUX}) @Test public void testA() {}
            @DisabledOnOs(OS.WINDOWS) @Test public void testB() {}
        }''')
        self.assertEqual(self.skip(reason='Disabled on operating system: Linux')['rule'], 'original-source-os-condition')
        with self.assertRaises(ValueError): self.skip(method='testB', reason='Disabled on operating system: Linux')

    def test_qualified_os_selector_and_exact_composed_custom_reason(self):
        self.write('Example', '''@DisabledOnOs(value = org.junit.jupiter.api.condition.OS.LINUX,
            disabledReason = "original condition") public class Example { @Test void testA() {} }''')
        reason = 'Disabled on operating system: Linux ==> original condition'
        self.assertEqual(self.skip(reason=reason)['conditionMethod'], '<class>')
        with self.assertRaises(ValueError): self.skip(reason='original condition')
        with self.assertRaises(ValueError): self.skip(reason='Disabled on operating system: Linux ==> unrelated')

    def test_disabled_named_value_and_qualified_windows_selector(self):
        self.write('Example', '''public class Example {
            @Disabled(value = "original condition") @Test void testA() {}
        }''')
        self.assertEqual(self.skip(reason='original condition')['conditionMethod'], 'testA')
        self.write('Example', '''@EnabledOnOs(org.junit.jupiter.api.condition.OS.WINDOWS)
            public class Example { @Test void testA() {} }''')
        self.assertEqual(self.skip(reason='Disabled on operating system: Linux')['conditionMethod'], '<class>')

    def test_only_known_false_direct_platform_default_is_permitted(self):
        self.write('Example', '''import org.eclipse.swt.*; public class Example {
            @BeforeAll void before() { assumeTrue("win32".equals(SWT.getPlatform())); }
            @Test void testA() {}
        }''')
        default = 'Assumption failed: assumption is not true'
        self.assertFalse(self.skip(reason=default)['platformWitness']['conditionValue'])
        for condition in ['"gtk".equals(SWT.getPlatform())', 'environmentAvailable', 'SwtTestUtil.isCocoa && environmentAvailable']:
            self.write('Example', 'import org.eclipse.swt.*; public class Example { @Test void testA() { assumeTrue(' + condition + '); } }')
            with self.assertRaises(ValueError): self.skip(reason=default)

    def test_cocoa_flag_requires_its_actual_original_definition(self):
        self.write('Example', 'public class Example { @Test void testA() { assumeTrue(SwtTestUtil.isCocoa); } }')
        self.write('SwtTestUtil', '''import org.eclipse.swt.SWT; public class SwtTestUtil {
            public final static boolean isCocoa = SWT.getPlatform().startsWith("cocoa");
        }''')
        default = 'Assumption failed: assumption is not true'
        self.assertIn('definitionSourceGitBlob', self.skip(reason=default)['platformWitness'])
        self.write('SwtTestUtil', 'public class SwtTestUtil { public final static boolean isCocoa = true; }')
        with self.assertRaises(ValueError): self.skip(reason=default)

    def test_before_each_has_applicable_scope(self):
        self.write('Example', '''public class Example {
            @BeforeEach public void before() { assumeTrue(condition, "original condition"); }
            @Test public void testA() {}
        }''')
        self.assertEqual(self.skip()['conditionMethod'], 'before')

    def test_inherited_method_and_fixture_keep_original_owner(self):
        parent = self.write('Parent', '''public class Parent {
            @BeforeAll public static void before() { assumeTrue(condition, "original condition"); }
            @Test public void testA() {}
        }''')
        self.write('Example', 'public class Example extends Parent {}')
        result = self.skip()
        self.assertEqual(result['conditionOwner'], policy.PACKAGE + '.Parent')
        self.assertEqual(result['conditionSourceGitBlob'], policy._blob(parent.read_bytes()))

    def test_overridden_fixture_does_not_inherit_parent_skip(self):
        self.write('Parent', '''public class Parent {
            @BeforeEach public void before() { assumeTrue(condition, "original condition"); }
        }''')
        self.write('Example', '''public class Example extends Parent {
            @Override public void before() {}
            @Test public void testA() {}
        }''')
        with self.assertRaises(ValueError): self.skip()

    def test_ancestor_class_disabled_not_assumed_inherited(self):
        self.write('Parent', '@Disabled("original condition") public class Parent { @Test void testA() {} }')
        self.write('Example', 'public class Example extends Parent {}')
        with self.assertRaises(ValueError): self.skip(reason='original condition')

    def test_class_annotation_and_literal_braces(self):
        self.write('Example', '''@org.junit.jupiter.api.Disabled("original condition") public class Example {
            @Test public void testA() { String braces = "{ // }"; }
        }''')
        self.assertEqual(self.skip(reason='original condition')['conditionMethod'], '<class>')

    def test_default_missing_and_ambiguous_methods_refused(self):
        self.write('Example', 'public class Example { @Disabled @Test void testA() {} }')
        with self.assertRaises(ValueError): self.skip()
        with self.assertRaises(ValueError): self.skip(method='absent')
        self.write('Example', '''public class Example {
            @Disabled("original condition") @Test void testA() {}
            @Disabled("original condition") @Test void testA(int value) {}
        }''')
        with self.assertRaises(ValueError): self.skip(reason='original condition')

    def test_ignored_field_initializer_and_comments(self):
        self.write('Example', '''public class Example {
            Object object = factory("original condition");
            /* @BeforeEach void bogus() { assumeTrue(false, "original condition"); } */
            @Test public void testA() {}
        }''')
        with self.assertRaises(ValueError): self.skip()

    def test_empty_assumption_reason_refused(self):
        self.write('Example', 'public class Example { @Test void testA() { assumeTrue(false, ""); } }')
        with self.assertRaises(ValueError): self.skip(reason='Assumption failed: ')



if __name__ == "__main__":
    unittest.main()
