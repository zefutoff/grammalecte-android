import tempfile
import unittest
from pathlib import Path

from instrumentation_results import prepare, verify


class InstrumentationReportTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.directory = Path(self.temp.name)

    def report(self, xml):
        (self.directory / "TEST-device.xml").write_text(
            xml, encoding="utf-8"
        )

    def test_missing_report_fails(self):
        with self.assertRaisesRegex(
            RuntimeError, "No instrumentation XML"
        ):
            verify(self.directory)

    def test_passing_report_succeeds(self):
        self.report(
            '<testsuite tests="1">'
            '<testcase name="works"/>'
            '</testsuite>'
        )
        verify(self.directory)

    def test_failing_report_fails(self):
        self.report(
            '<testsuite tests="1" failures="1">'
            '<testcase name="broken"><failure/></testcase>'
            '</testsuite>'
        )
        with self.assertRaises(RuntimeError):
            verify(self.directory)

    def test_only_skipped_fails(self):
        self.report(
            '<testsuite tests="1" skipped="1">'
            '<testcase name="skip"><skipped/></testcase>'
            '</testsuite>'
        )
        with self.assertRaises(RuntimeError):
            verify(self.directory)

    def test_prepare_removes_stale_reports_only(self):
        self.report(
            '<testsuite tests="1">'
            '<testcase name="old"/>'
            '</testsuite>'
        )
        (self.directory / "other.txt").write_text(
            "keep", encoding="utf-8"
        )

        prepare(self.directory)

        self.assertFalse(
            list(self.directory.rglob("TEST-*.xml"))
        )
        self.assertTrue(
            (self.directory / "other.txt").exists()
        )


if __name__ == "__main__":
    unittest.main()
