#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

import os
import subprocess
import sys
import unittest
from unittest import mock

from notices import NOTICES_DIR, escape_teamcity, format_teamcity_problem
from notices.license_files import filter_platforms
from notices.lwm2m import identify_license, parse_header
from notices.scanner import ensure_docker_available, is_notice_path


class HelpersTests(unittest.TestCase):
    def test_filter_platforms_keeps_selected_and_rejects_unknown(self):
        text = (
            'Common\n'
            '======== Only in the Linux build ========\n'
            'Linux only\n'
            '======== Only in the Windows build ========\n'
            'Windows only'
        )
        self.assertEqual(filter_platforms(text, {'linux'}, 'CREDITS.txt'),
                         'Common\n======== Only in the Linux build ========\nLinux only')
        self.assertEqual(filter_platforms(text, {'windows'}, 'CREDITS.txt'),
                         'Common\n======== Only in the Windows build ========\nWindows only')
        self.assertEqual(filter_platforms(text, None, 'CREDITS.txt'), text)
        with self.assertRaises(SystemExit):
            filter_platforms(text, {'macos'}, 'CREDITS.txt')

    def test_lwm2m_header_and_identify(self):
        xml = '<?xml version="1.0"?>\n<!--\nLEGAL DISCLAIMER\nCopyright 2020 OMA.\nPermission is hereby granted, free of charge, to any person.\n-->\n<LWM2M/>'
        copyrights, text = parse_header(xml)
        self.assertEqual(copyrights, ['Copyright 2020 OMA.'])
        self.assertEqual(identify_license(text, '1.xml'), 'MIT')

    def test_is_notice_path(self):
        self.assertTrue(is_notice_path('META-INF/NOTICE'))
        self.assertTrue(is_notice_path('META-INF/NOTICE.txt'))
        self.assertTrue(is_notice_path('THIRD_PARTY_NOTICES.md'))
        self.assertFalse(is_notice_path('com/example/NoticeReference.class'))

    def test_ensure_docker_available_reports_missing_and_unreachable_daemon(self):
        with mock.patch('notices.scanner.subprocess.run', side_effect=FileNotFoundError):
            with self.assertRaises(SystemExit) as ctx:
                ensure_docker_available()
            self.assertIn('not found in PATH', str(ctx.exception))

        failed = subprocess.CompletedProcess(['docker', 'info'], 1, '', 'Cannot connect to the Docker daemon')
        with mock.patch('notices.scanner.subprocess.run', return_value=failed):
            with self.assertRaises(SystemExit) as ctx:
                ensure_docker_available()
            self.assertIn('Docker daemon is not reachable', str(ctx.exception))

    def test_escape_and_format_teamcity_problem(self):
        self.assertEqual(
            escape_teamcity("a|b'c[d]e\nf\rg"),
            "a||b|'c|[d|]e|nf|rg",
        )
        msg = (
            'Third-party notices: these packages are under licenses that ThingsBoard cannot ship.\n'
            '  pkg:maven/com.example/a@1.0: GPL-3.0-only\n'
            '  pkg:maven/com.example/b@2.0: AGPL-3.0-only\n'
            'Remove or replace the dependency.\n'
        )
        self.assertEqual(
            format_teamcity_problem(msg),
            'Third-party notices: these packages are under licenses that ThingsBoard cannot ship. - '
            'pkg:maven/com.example/a@1.0: GPL-3.0-only, pkg:maven/com.example/b@2.0: AGPL-3.0-only',
        )
        truncated = format_teamcity_problem(msg, max_len=145)
        self.assertLessEqual(len(truncated), 145)
        self.assertIn('pkg:maven/com.example/a@1.0: GPL-3.0-only', truncated)
        self.assertTrue(truncated.endswith('(+1 more)'))

    def test_main_emits_teamcity_build_problem_only_on_teamcity(self):
        cmd = [sys.executable, NOTICES_DIR, 'render', '--project-version', '1.0', '--out', '/dev/null']
        env_local = {k: v for k, v in os.environ.items() if k != 'TEAMCITY_VERSION'}
        local = subprocess.run(cmd, capture_output=True, text=True, env=env_local)
        self.assertEqual(local.returncode, 1)
        self.assertNotIn('##teamcity[', local.stdout)
        self.assertIn('Third-party notices: no SBOMs given', local.stderr)

        env_tc = {**env_local, 'TEAMCITY_VERSION': '2024.12.3'}
        tc = subprocess.run(cmd, capture_output=True, text=True, env=env_tc)
        self.assertEqual(tc.returncode, 1)
        self.assertIn(
            "##teamcity[buildProblem description='Third-party notices: no SBOMs given (pass SBOM paths or --sboms-under <directory>)']",
            tc.stdout,
        )
        self.assertIn('Third-party notices: no SBOMs given', tc.stderr)


if __name__ == '__main__':
    unittest.main()
