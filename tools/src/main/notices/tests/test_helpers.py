#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

import ast
import io
import os
import stat
import subprocess
import sys
import tarfile
import tempfile
import unittest
from unittest import mock

from notices import NOTICES_DIR, escape_teamcity, format_teamcity_problem, write_atomically
from notices.license_files import filter_platforms
from notices.lwm2m import identify_license, parse_header
from notices.render import _extract_from_scancode_image
from notices.scanner import (
    SCANCODE_VERSION,
    _extract_tarball,
    _normalize_scancode_batch,
    ensure_docker_available,
    is_notice_path,
    scan_sources_with_scancode,
)


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
            escape_teamcity("a|b'c[d]e\nf\rg\u0085h\u2028i\u2029j"),
            "a||b|'c|[d|]e|nf|rg|xh|li|pj",
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
        self.assertEqual(format_teamcity_problem('Hello world', max_len=2), 'He')

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

    def test_write_atomically_sets_world_readable_mode(self):
        with tempfile.TemporaryDirectory() as tmp:
            out = os.path.join(tmp, 'nested', 'THIRD-PARTY-NOTICES')
            write_atomically(out, b'hello\n')
            with open(out, 'rb') as f:
                self.assertEqual(f.read(), b'hello\n')
            if os.name == 'posix':
                self.assertEqual(stat.S_IMODE(os.stat(out).st_mode), 0o644)

    def test_extract_from_scancode_image_script_is_valid_python(self):
        captured = {}

        def fake_run(cmd, **kwargs):
            captured['script'] = cmd[cmd.index('-c') + 1]
            return subprocess.CompletedProcess(cmd, 1, '', '')

        with mock.patch('notices.render.subprocess.run', side_effect=fake_run):
            self.assertIsNone(_extract_from_scancode_image('MIT'))
        ast.parse(captured['script'])

    def test_extract_tarball_preserves_paths_without_common_root_and_strips_wrapper(self):
        def make_tar(files):
            buf = io.BytesIO()
            with tarfile.open(fileobj=buf, mode='w:gz') as tf:
                for name, data in files.items():
                    info = tarfile.TarInfo(name)
                    info.size = len(data)
                    tf.addfile(info, io.BytesIO(data))
            return buf.getvalue()

        with tempfile.TemporaryDirectory() as tmp1, tempfile.TemporaryDirectory() as tmp2:
            # No common root dir -> dist/index.js and lib/index.js must not overwrite each other
            _extract_tarball(make_tar({'dist/index.js': b'dist', 'lib/index.js': b'lib'}), 'test.tgz', tmp1)
            self.assertTrue(os.path.exists(os.path.join(tmp1, 'dist', 'index.js')))
            self.assertTrue(os.path.exists(os.path.join(tmp1, 'lib', 'index.js')))

            # Standard npm package/ wrapper -> stripped cleanly
            _extract_tarball(make_tar({'package/index.js': b'ok', 'package/LICENSE': b'MIT'}), 'pkg.tgz', tmp2)
            self.assertTrue(os.path.exists(os.path.join(tmp2, 'index.js')))
            self.assertTrue(os.path.exists(os.path.join(tmp2, 'LICENSE')))

    def test_normalize_scancode_batch_splits_packages_and_validates(self):
        sources = {
            'k1': {'type': 'artifact', 'url': 'https://example/a.tgz', 'sha1': '0' * 40},
            'k2': {'type': 'artifact', 'url': 'https://example/b.tgz', 'sha1': '1' * 40},
        }
        notices = {'k1': [{'path': 'NOTICE', 'text': 'Notice A'}]}

        # Version mismatch stops fail-closed
        bad_ver = {'headers': [{'tool_version': '99.0.0'}], 'files': []}
        with self.assertRaises(SystemExit) as ctx:
            _normalize_scancode_batch(bad_ver, sources, notices)
        self.assertIn('ScanCode version mismatch', str(ctx.exception))

        # Per-file scan error stops fail-closed with source URL
        errored = {
            'headers': [{'tool_version': SCANCODE_VERSION}],
            'files': [{'type': 'file', 'path': 'k1/broken.js', 'scan_errors': ['timeout']}],
        }
        with self.assertRaises(SystemExit) as ctx:
            _normalize_scancode_batch(errored, sources, notices)
        self.assertIn('https://example/a.tgz (broken.js): timeout', str(ctx.exception))

        # Normal multi-package batch splits findings and keeps empty packages
        raw = {
            'headers': [{'tool_version': SCANCODE_VERSION}],
            'files': [
                {'type': 'directory', 'path': 'k1'},
                {
                    'type': 'file',
                    'path': 'k1/src/index.js',
                    'detected_license_expression_spdx': 'MIT',
                    'copyrights': [{'copyright': 'Copyright 2025 A'}],
                },
                {'type': 'file', 'path': 'k2/empty.js', 'detected_license_expression_spdx': None, 'copyrights': []},
            ],
        }
        records = _normalize_scancode_batch(raw, sources, notices)
        self.assertEqual(set(records.keys()), {'k1', 'k2'})
        self.assertEqual(records['k1']['findings'], [{'path': 'src/index.js', 'licenses': ['MIT'], 'copyrights': ['Copyright 2025 A']}])
        self.assertEqual(records['k1']['notices'], [{'path': 'NOTICE', 'text': 'Notice A'}])
        self.assertEqual(records['k2']['findings'], [])
        self.assertEqual(records['k2']['notices'], [])

    def test_scan_sources_with_scancode_batches_docker_invocations(self):
        import json

        docker_calls = []

        def fake_fetch(source, target_dir):
            return []

        def fake_run(cmd, **kwargs):
            docker_calls.append(cmd)
            mount = cmd[cmd.index('-v') + 1]
            host_work = mount.split(':', 1)[0]
            staged_keys = sorted(os.listdir(os.path.join(host_work, 'src')))
            files = [
                {
                    'type': 'file',
                    'path': f'{k}/index.js',
                    'detected_license_expression_spdx': 'Apache-2.0',
                    'copyrights': [],
                }
                for k in staged_keys
            ]
            with open(os.path.join(host_work, 'scancode.json'), 'w', encoding='utf-8') as out:
                json.dump({'headers': [{'tool_version': SCANCODE_VERSION}], 'files': files}, out)
            return subprocess.CompletedProcess(cmd, 0, '', '')

        items = [
            ('k1', 'pkg:npm/a@1.0', {'type': 'artifact', 'url': 'https://example/a.tgz', 'sha1': '1' * 40}),
            ('k2', 'pkg:npm/b@1.0', {'type': 'artifact', 'url': 'https://example/b.tgz', 'sha1': '2' * 40}),
            ('k3', 'pkg:npm/c@1.0', {'type': 'artifact', 'url': 'https://example/c.tgz', 'sha1': '3' * 40}),
        ]
        with mock.patch('notices.scanner._fetch_and_extract', side_effect=fake_fetch), \
             mock.patch('notices.scanner.subprocess.run', side_effect=fake_run):
            results = dict(scan_sources_with_scancode(items, batch_size=2))

        self.assertEqual(len(docker_calls), 2)
        self.assertEqual(set(results.keys()), {'k1', 'k2', 'k3'})
        for k in ('k1', 'k2', 'k3'):
            self.assertEqual(results[k]['findings'], [{'path': 'index.js', 'licenses': ['Apache-2.0'], 'copyrights': []}])


if __name__ == '__main__':
    unittest.main()
