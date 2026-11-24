#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
"""Synthetic evidence tamper tests; these never claim native SWT execution."""
import importlib.util
import json
import shutil
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


def place(source, name, lines):
    """Write an original test class whose 1-based lines hold exact code, as a recorded trace names them."""
    text = ["package " + host.PACKAGE + ";"] + [""] * (max(lines) - 1)
    for number, code in lines.items():
        text[number - 1] = code
    path = source / host.JAVA_ROOT / Path(*host.PACKAGE.split(".")) / (name + ".java")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(text) + "\n")
    return path


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
        # A previous refused capture in the same test leaves its copied reports behind.
        shutil.rmtree(self.proof / "reports", ignore_errors=True)
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

    # Run 45445598676 (SWT 16b74941): AllWidgetTests selects the abstract base classes
    # Test_org_eclipse_swt_widgets_{Widget,Control,...}; JUnit reports only concrete
    # subclasses such as Test_org_eclipse_swt_widgets_Button. Declarations are the real ones.
    def add_widget_hierarchy(self, widget="abstract class Test_org_eclipse_swt_widgets_Widget{", button=True):
        self.write_java("Test_org_eclipse_swt_widgets_Widget", widget + "\n@Test public void test_isDisposed() {}\n}\n")
        self.write_java("Test_org_eclipse_swt_widgets_Control", "abstract class Test_org_eclipse_swt_widgets_Control "
                        "extends Test_org_eclipse_swt_widgets_Widget {\n}\n")
        self.write_java("Test_org_eclipse_swt_widgets_Button", "public class Test_org_eclipse_swt_widgets_Button "
                        "extends Test_org_eclipse_swt_widgets_Control {\n}\n")
        self.add_selected_class("Test_org_eclipse_swt_widgets_Widget")
        self.add_selected_class("Test_org_eclipse_swt_widgets_Control")
        if button:
            self.add_selected_class("Test_org_eclipse_swt_widgets_Button")
            self.write_report("Test_org_eclipse_swt_widgets_Button",
                              ["test_isDisposed", "test_setDataLjava_lang_StringLjava_lang_Object"])

    def test_abstract_selections_are_not_leaves_but_need_a_reported_concrete_subclass(self):
        self.add_widget_hierarchy()
        result = self.capture()
        self.assertEqual(result["passed"], 11)
        inventory = json.loads((self.proof / "receipt.json").read_text())["reports"]["selectedSourceInventory"]
        widget, control, button = (host.PACKAGE + ".Test_org_eclipse_swt_widgets_" + name
                                   for name in ("Widget", "Control", "Button"))
        self.assertEqual([row["class"] for row in inventory["abstractSelections"]], [control, widget])
        self.assertEqual([row["concreteSubclasses"] for row in inventory["abstractSelections"]], [[button], [button]])
        self.assertNotIn(widget, [row["class"] for row in inventory["leaves"]])
        self.assertIn(button, [row["class"] for row in inventory["leaves"]])
        self.assertEqual(host.verify(self.target), result)

    def test_abstract_selection_without_a_reported_concrete_subclass_fails(self):
        self.add_widget_hierarchy(button=False)
        with self.assertRaisesRegex(ValueError, "no reported concrete subclass: .*Test_org_eclipse_swt_widgets_Control"):
            self.capture()

    def test_a_report_for_an_abstract_selection_is_outside_the_executable_selection(self):
        self.add_widget_hierarchy()
        self.write_report("Test_org_eclipse_swt_widgets_Widget", ["test_isDisposed"])
        with self.assertRaisesRegex(ValueError, "outside original AllTests selection"):
            self.capture()

    def test_abstract_only_counts_as_a_declaration_modifier(self):
        for widget in ('/* abstract */ class Test_org_eclipse_swt_widgets_Widget{',
                       '@SuppressWarnings("abstract") class Test_org_eclipse_swt_widgets_Widget{'):
            with self.subTest(widget=widget):
                self.add_widget_hierarchy(widget=widget)
                with self.assertRaisesRegex(ValueError, "selected leaf classes are missing: .*_Widget"):
                    self.capture()
        # A braced annotation argument before the modifier does not hide it.
        self.add_widget_hierarchy(widget='@SuppressWarnings({"restriction", "unused"}) public abstract class '
                                         'Test_org_eclipse_swt_widgets_Widget{')
        self.assertEqual(self.capture()["passed"], 11)

    def test_tampered_abstract_selection_inventory_fails_verification(self):
        self.add_widget_hierarchy()
        self.capture()
        for subclasses in ([], [host.PACKAGE + ".NotSelected"]):
            with self.subTest(subclasses=subclasses):
                path = self.proof / "receipt.json"
                original = path.read_text()
                self.mutate_receipt(lambda value: value["reports"]["selectedSourceInventory"]["abstractSelections"][0]
                                    .update(concreteSubclasses=subclasses))
                with self.assertRaises(ValueError):
                    host.verify(self.target)
                path.write_text(original)

    # Run 45445598676: surefire 3.6.1 writes no message attribute for an aborted
    # assumption; the reason is in the element text (StyledTextViewportProofTest).
    PROOF_TRACE = ("org.opentest4j.TestAbortedException: Assumption failed: opt-in proof lane\n"
                   "\tat org.junit.jupiter.api.Assumptions.throwAssumptionFailed(Assumptions.java:321)\n"
                   "\tat org.junit.jupiter.api.Assumptions.assumeTrue(Assumptions.java:120)\n"
                   "\tat org.eclipse.swt.tests.junit.StyledTextViewportProofTest.setUp(StyledTextViewportProofTest.java:63)\n"
                   "\tat java.base/java.lang.reflect.Method.invoke(Method.java:580)\n")

    def write_aborted_report(self, name, testcase, text, kind="org.opentest4j.TestAbortedException", message=None):
        tree = ET.Element("testsuite", {"tests": "1", "failures": "0", "errors": "0", "skipped": "1"})
        props = ET.SubElement(tree, "properties")
        for key, value in {"swt.library.path": str(self.native), "os.name": "Linux", "os.arch": "amd64"}.items():
            ET.SubElement(props, "property", {"name": key, "value": value})
        case = ET.SubElement(tree, "testcase", {"classname": host.PACKAGE + "." + name, "name": testcase})
        attributes = {"type": kind} if kind else {}
        if message is not None:
            attributes["message"] = message
        ET.SubElement(case, "skipped", attributes).text = text
        (self.xml / ("TEST-" + name + ".xml")).write_text(ET.tostring(tree, encoding="unicode"))

    def add_proof_test(self):
        self.write_java("StyledTextViewportProofTest", "public class StyledTextViewportProofTest {\n"
                        "@BeforeEach\npublic void setUp() throws IOException {\n"
                        'assumeTrue(Boolean.getBoolean("swt.viewport.styledTextProof"), "opt-in proof lane");\n}\n'
                        "@Test\npublic void test_tenMillionLinesWordWrap() throws Exception {}\n}\n")
        self.add_selected_class("StyledTextViewportProofTest")

    def test_aborted_skip_reason_is_read_from_the_element_text(self):
        self.add_proof_test()
        self.write_aborted_report("StyledTextViewportProofTest", "test_tenMillionLinesWordWrap", self.PROOF_TRACE)
        result = self.capture()
        self.assertEqual(result["permittedSkips"], 1)
        row = json.loads((self.proof / "receipt.json").read_text())["reports"]["permittedSkips"][0]
        self.assertEqual((row["reason"], row["rule"], row["conditionMethod"]),
                         ("Assumption failed: opt-in proof lane", "original-source-declared-condition", "setUp"))
        self.assertEqual(host.verify(self.target), result)

    def test_aborted_skip_text_must_start_with_its_declared_type(self):
        self.add_proof_test()
        for kind in ("java.lang.AssertionError", ""):
            with self.subTest(kind=kind):
                self.write_aborted_report("StyledTextViewportProofTest", "test_tenMillionLinesWordWrap",
                                          self.PROOF_TRACE, kind=kind)
                with self.assertRaisesRegex(ValueError, "unexplained skipped"):
                    self.capture()

    def test_a_message_attribute_still_takes_precedence_over_the_text(self):
        self.add_proof_test()
        self.write_aborted_report("StyledTextViewportProofTest", "test_tenMillionLinesWordWrap", self.PROOF_TRACE,
                                  message="Assumption failed: another reason")
        with self.assertRaisesRegex(ValueError, "No applicable original source condition"):
            self.capture()

    # Run 45445598676: Test_org_eclipse_swt_widgets_Table_ViewportDeltas skips from a helper,
    # table() at line 77, called by the test method at line 141.
    TABLE_TRACE = ("org.opentest4j.TestAbortedException: Assumption failed: the viewport of a plain Table is win32 only\n"
                   "\tat org.junit.jupiter.api.Assumptions.throwAssumptionFailed(Assumptions.java:321)\n"
                   "\tat org.junit.jupiter.api.Assumptions.assumeTrue(Assumptions.java:120)\n"
                   "\tat org.eclipse.swt.tests.junit.Test_org_eclipse_swt_widgets_Table_ViewportDeltas.table("
                   "Test_org_eclipse_swt_widgets_Table_ViewportDeltas.java:77)\n"
                   "\tat org.eclipse.swt.tests.junit.Test_org_eclipse_swt_widgets_Table_ViewportDeltas."
                   "columnSweepIsADeltaOfTheViewport(Test_org_eclipse_swt_widgets_Table_ViewportDeltas.java:141)\n"
                   "\tat java.base/java.lang.reflect.Method.invoke(Method.java:580)\n")

    def test_helper_assumption_binds_through_the_recorded_call_site_and_survives_verification(self):
        place(self.source, "Test_org_eclipse_swt_widgets_Table_ViewportDeltas", {
            4: "public class Test_org_eclipse_swt_widgets_Table_ViewportDeltas {",
            74: "\tprivate Table table (int columns) {",
            77: '\t\tAssumptions.assumeTrue (field (table, Table.class, "viewport") != null, '
                '"the viewport of a plain Table is win32 only");',
            78: "\t\treturn null;", 79: "\t}",
            139: "\t@Test", 140: "\tpublic void columnSweepIsADeltaOfTheViewport () {",
            141: "\t\tTable table = table (3);", 142: "\t}", 143: "}"})
        self.add_selected_class("Test_org_eclipse_swt_widgets_Table_ViewportDeltas")
        self.write_aborted_report("Test_org_eclipse_swt_widgets_Table_ViewportDeltas",
                                  "columnSweepIsADeltaOfTheViewport", self.TABLE_TRACE)
        result = self.capture()
        row = json.loads((self.proof / "receipt.json").read_text())["reports"]["permittedSkips"][0]
        self.assertEqual(row["rule"], "original-source-call-site-condition")
        self.assertEqual((row["conditionMethod"], row["conditionLine"]), ("table", 77))
        self.assertEqual([entry.rsplit(".", 1)[1] for entry in row["callPath"]],
                         ["table:77", "columnSweepIsADeltaOfTheViewport:141"])
        self.assertEqual(host.verify(self.target), result)
        self.mutate_receipt(lambda value: value["reports"]["permittedSkips"][0].update(rule="unbound"))
        with self.assertRaises(ValueError):
            host.verify(self.target)


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



class CallSiteSkipTest(unittest.TestCase):
    """Skips whose assumption is in a helper, bound through the recorded stack trace.

    Classes, messages, methods and line numbers are fragments of run 45445598676
    (SWT 16b74941); the surrounding code is reduced to what the binding reads.
    """
    ABORTED = "org.opentest4j.TestAbortedException: Assumption failed: "
    ASSUMPTIONS = ("\tat org.junit.jupiter.api.Assumptions.throwAssumptionFailed(Assumptions.java:321)\n"
                   "\tat org.junit.jupiter.api.Assumptions.assumeTrue(Assumptions.java:120)\n")
    REFLECTION = "\tat java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)\n"
    COMBO = "Test_org_eclipse_swt_widgets_Combo_ViewportDifferential"
    COMBO_MESSAGE = "the Combo viewport is win32 only"

    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.source = Path(temporary.name)

    @staticmethod
    def frame(name, method, line):
        return "\tat %s.%s.%s(%s.java:%d)\n" % (host.PACKAGE, name, method, name, line)

    def skip(self, name, testcase, message, trace):
        return host.permitted_skip(self.source, host.PACKAGE + "." + name, testcase, "Assumption failed: " + message, trace)

    def combo(self, assumption_line=216, message=None):
        # Three overloads of run(); only the last one assumes.
        lines = {4: "public class %s {" % self.COMBO,
                 140: "\t@Test", 141: "\tpublic void closedDeltasAnswerLikeTheReference () throws IOException {",
                 142: '\t\trun ("closed", SWT.DROP_DOWN, false, CLOSED, 40);', 143: "\t}",
                 144: "\t@Test", 145: "\tpublic void readOnlyComboAnswersLikeTheReference () throws IOException {",
                 146: '\t\trun ("readOnly", SWT.READ_ONLY);', 147: "\t}",
                 207: "\tprivate void run (String name, int style) throws IOException {",
                 208: "\t\trun (name, style, false);", 209: "\t}",
                 211: "\tprivate void run (String name, int style, boolean verify) throws IOException {",
                 212: "\t\trun (name, style, verify, null, STEPS);", 213: "\t}",
                 215: "\tprivate void run (String name, int style, boolean verify, Ops ops, int steps) throws IOException {",
                 assumption_line: '\t\tAssumptions.assumeTrue ("win32".equals (SWT.getPlatform ()), "%s");'
                                  % (message or self.COMBO_MESSAGE),
                 217: "\t\tboolean reference = ViewportDifferential.isReference (\"Combo\");", 218: "\t}", 219: "}"}
        place(self.source, self.COMBO, lines)

    def combo_trace(self, run_line=216, test="closedDeltasAnswerLikeTheReference", test_line=142, between=""):
        return (self.ABORTED + self.COMBO_MESSAGE + "\n" + self.ASSUMPTIONS + self.frame(self.COMBO, "run", run_line)
                + between + self.frame(self.COMBO, test, test_line) + self.REFLECTION)

    def test_overloaded_helper_assumption_binds_to_the_exact_call_line(self):
        self.combo()
        row = self.skip(self.COMBO, "closedDeltasAnswerLikeTheReference", self.COMBO_MESSAGE, self.combo_trace())
        self.assertEqual(row["rule"], "original-source-call-site-condition")
        self.assertEqual((row["conditionOwner"], row["conditionMethod"], row["conditionLine"]),
                         (host.PACKAGE + "." + self.COMBO, "run", 216))
        self.assertEqual(row["conditionSourceGitBlob"], host._blob((self.source / host.JAVA_ROOT / Path(
            *host.PACKAGE.split(".")) / (self.COMBO + ".java")).read_bytes()))

    def test_a_line_without_the_assumption_does_not_bind(self):
        self.combo()
        for line in (215, 217):
            with self.subTest(line=line), self.assertRaises(ValueError):
                self.skip(self.COMBO, "closedDeltasAnswerLikeTheReference", self.COMBO_MESSAGE, self.combo_trace(line))

    def test_another_message_at_the_line_does_not_bind(self):
        self.combo(message="the Combo viewport is gtk only")
        with self.assertRaises(ValueError):
            self.skip(self.COMBO, "closedDeltasAnswerLikeTheReference", self.COMBO_MESSAGE, self.combo_trace())

    def test_the_path_must_end_in_the_skipped_test_method(self):
        self.combo()
        trace = self.combo_trace(test="readOnlyComboAnswersLikeTheReference", test_line=146)
        with self.assertRaises(ValueError):
            self.skip(self.COMBO, "closedDeltasAnswerLikeTheReference", self.COMBO_MESSAGE, trace)

    def test_a_framework_frame_inside_the_path_breaks_it(self):
        self.combo()
        trace = self.combo_trace(between="\tat java.base/java.util.ArrayList.forEach(ArrayList.java:1596)\n")
        with self.assertRaises(ValueError):
            self.skip(self.COMBO, "closedDeltasAnswerLikeTheReference", self.COMBO_MESSAGE, trace)

    def test_frames_must_name_the_method_that_holds_their_line(self):
        self.combo()
        for method in ("lambda$closedDeltasAnswerLikeTheReference$0", "readOnlyComboAnswersLikeTheReference"):
            trace = self.combo_trace().replace(".run(", "." + method + "(")
            with self.subTest(method=method), self.assertRaises(ValueError):
                self.skip(self.COMBO, "closedDeltasAnswerLikeTheReference", self.COMBO_MESSAGE, trace)

    def test_without_a_trace_the_helper_assumption_stays_unbound(self):
        self.combo()
        with self.assertRaisesRegex(ValueError, "No applicable original source condition"):
            self.skip(self.COMBO, "closedDeltasAnswerLikeTheReference", self.COMBO_MESSAGE, "")

    def test_assumption_in_another_source_class_binds_along_the_whole_path(self):
        # ViewportDifferential.compareWithRecording:325 <- M3OfficialScene.run:186
        # <- BatchScopeDifferential.run:59 <- plainWidgetsAnswerLikeTheOfficialJar:64
        message = "-Dswt.viewport.recording=<dir> is not set"
        place(self.source, "ViewportDifferential", {
            4: "public final class ViewportDifferential<S> {",
            324: "\tpublic void compareWithRecording (S subject, boolean record, String directory) throws IOException {",
            325: '\t\tAssumptions.assumeTrue (directory != null, "%s");' % message, 326: "\t}", 327: "}"})
        place(self.source, "M3OfficialScene", {
            4: "final class M3OfficialScene {", 180: "\tstatic void run (String name, boolean plain) throws IOException {",
            186: "\t\tdifferential.compareWithRecording (subject, false, directory);", 187: "\t}", 188: "}"})
        name = "Test_org_eclipse_swt_widgets_BatchScopeDifferential"
        place(self.source, name, {
            4: "public class %s {" % name, 58: "\tprivate void run (String name, boolean plain) throws IOException {",
            59: "\t\tM3OfficialScene.run (name, plain);", 60: "\t}",
            62: "\t@Test", 63: "\tpublic void plainWidgetsAnswerLikeTheOfficialJar () throws IOException {",
            64: '\t\trun ("plain", true);', 65: "\t}", 66: "}"})
        trace = (self.ABORTED + message + "\n" + self.ASSUMPTIONS + self.frame("ViewportDifferential", "compareWithRecording", 325)
                 + self.frame("M3OfficialScene", "run", 186) + self.frame(name, "run", 59)
                 + self.frame(name, "plainWidgetsAnswerLikeTheOfficialJar", 64) + self.REFLECTION)
        row = self.skip(name, "plainWidgetsAnswerLikeTheOfficialJar", message, trace)
        self.assertEqual((row["conditionOwner"], row["conditionLine"]), (host.PACKAGE + ".ViewportDifferential", 325))
        self.assertEqual(len(row["callPath"]), 4)
        # An intermediate frame whose line is outside the method it names does not bind.
        with self.assertRaises(ValueError):
            self.skip(name, "plainWidgetsAnswerLikeTheOfficialJar", message,
                      trace.replace("M3OfficialScene.java:186", "M3OfficialScene.java:179"))

    def test_class_level_initialization_error_binds_only_to_a_before_all_call(self):
        name = "Test_org_eclipse_swt_custom_StyledText_ApiScale"
        message = "opt-in: -Dswt.styledtext.scaleTest=true (1M lines, about 1.2 GB heap)"
        trace = (self.ABORTED + message + "\n" + self.frame(name, "setUp", 57)
                 + "\tat java.base/java.lang.reflect.Method.invoke(Method.java:580)\n")
        for annotation, binds in (("@BeforeAll", True), ("@BeforeEach", False)):
            with self.subTest(annotation=annotation):
                place(self.source, name, {
                    4: "public class %s {" % name, 55: "\t" + annotation, 56: "\tpublic static void setUp() {",
                    57: '\t\tassumeTrue(Boolean.getBoolean("swt.styledtext.scaleTest"), "%s");' % message,
                    58: "\t}", 60: "\t@Test", 61: "\tpublic void wholeApiAtScale() {}", 62: "}"})
                if binds:
                    self.assertEqual(self.skip(name, "initializationError", message, trace)["conditionMethod"], "setUp")
                else:
                    with self.assertRaises(ValueError):
                        self.skip(name, "initializationError", message, trace)
        with self.assertRaises(ValueError):
            self.skip(name, "initializationError", message, "")

    def test_skip_reason_reads_only_the_declared_exception_header(self):
        element = ET.fromstring('<skipped type="org.opentest4j.TestAbortedException"/>')
        element.text = self.ABORTED + "opt-in proof lane\n" + self.ASSUMPTIONS
        self.assertEqual(host.skip_reason(element), "Assumption failed: opt-in proof lane")
        element.set("message", "Disabled on operating system: Linux")
        self.assertEqual(host.skip_reason(element), "Disabled on operating system: Linux")
        for kind in ("org.opentest4j.TestSkippedException", "not a type"):
            element = ET.fromstring('<skipped type="%s"/>' % kind)
            element.text = self.ABORTED + "opt-in proof lane\n"
            with self.subTest(kind=kind):
                self.assertEqual(host.skip_reason(element), "")


if __name__ == "__main__":
    unittest.main()
