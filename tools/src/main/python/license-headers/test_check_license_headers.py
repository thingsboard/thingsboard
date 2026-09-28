#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

from contextlib import redirect_stderr, redirect_stdout
import io
import json
import os
from pathlib import Path
import tempfile
import textwrap
import unittest
from unittest.mock import patch
import xml.etree.ElementTree as ET

import check_license_headers as p


def temporary_directory(test):
    directory = tempfile.TemporaryDirectory()
    test.addCleanup(directory.cleanup)
    return Path(directory.name)


def this_repository():
    return Path(p.git(Path(__file__).parent, "rev-parse", "--show-toplevel").decode().strip())


def fixture_plugin(version="0"):
    return p.Plugin(version, ET.Element("configuration"))


class GitFixture:
    def __init__(self, root, branch):
        self.root = root
        self("init", "-b", branch)
        self("config", "commit.gpgsign", "false")
        self("config", "user.name", "Test")
        self("config", "user.email", "test@example.invalid")

    def __call__(self, *args):
        return p.git(self.root, *args).decode().strip()

    def write(self, name, text):
        path = self.root / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text)

    def commit(self, message):
        self("add", ".")
        self("commit", "-m", message)
        return self("rev-parse", "HEAD")


class DecisionTests(unittest.TestCase):
    def decide(self, **changes):
        evidence = dict(path="a.java", content_hash=p.raw_hash(b"reviewed"), current="pe-only", curation=None,
                        inherited=True, recreated=False, ce_hash="same", pe_hash="same",
                        candidates=[], historical=[], in_comparison=True)
        return p.decide(p.Evidence(**(evidence | changes)))

    def test_current_header_does_not_establish_origin(self):
        row = self.decide()
        self.assertEqual("apache", row["expectedHeader"])
        self.assertEqual("mismatch", row["status"])
        self.assertEqual("ce-modified", self.decide(pe_hash="changed")["expectedHeader"])

    def test_matching_curation_precedes_inference(self):
        curation = dict(contentHash=p.raw_hash(b"reviewed"), header="pe-only", reason="Independent replacement")
        self.assertEqual("match", self.decide(curation=curation)["status"])
        curation["contentHash"] = p.raw_hash(b"different")
        self.assertEqual("stale-curation", self.decide(curation=curation)["status"])

    def test_recreated_and_missing_ce_files_do_not_silently_pass(self):
        self.assertEqual("unresolved", self.decide(recreated=True)["status"])
        row = self.decide(inherited=False, in_comparison=False, ce_hash=None)
        self.assertEqual("unresolved", row["status"])
        self.assertEqual("pe-only", row["suggestedHeader"])
        self.assertIsNone(row["expectedHeader"])

    def test_identical_cross_path_source_is_not_proof(self):
        row = self.decide(inherited=False, candidates=[{"path": "original.java"}])
        self.assertEqual("unresolved", row["status"])
        self.assertEqual("apache", row["suggestedHeader"])

    def test_pe_additions_pass_without_strong_ce_evidence(self):
        added = dict(inherited=False, in_comparison=False, ce_hash=None, pe_addition=True)
        self.assertEqual("match", self.decide(**added)["status"])
        self.assertEqual("match", self.decide(**added, historical=[{"path": "other/a.java"}])["status"])
        for evidence in (dict(historical=[{"path": "a.java"}]),
                         dict(historical=[{"path": "other/a.java", "contentMatches": True}]),
                         dict(copies=[{"path": "other.java", "similarity": 78}])):
            self.assertEqual("unresolved", self.decide(**added, **evidence)["status"])

    def test_descriptor_of_a_module_absent_in_ce_is_pe_only_despite_similar_ce_descriptors(self):
        descriptor = dict(path="integration/mqtt/pom.xml", inherited=False, in_comparison=False, ce_hash=None,
                          pe_addition=True, copies=[{"path": "transport/mqtt/pom.xml", "similarity": 78}],
                          directory_in_ce=False)
        row = self.decide(**descriptor)
        self.assertEqual("match", row["status"])
        self.assertEqual("pe-only", row["expectedHeader"])
        for evidence in (dict(directory_in_ce=True),
                         dict(path="integration/mqtt/Mqtt.java"),
                         dict(pe_addition=False),
                         dict(candidates=[{"path": "transport/mqtt/pom.xml"}]),
                         dict(historical=[{"path": "transport/mqtt/pom.xml", "contentMatches": True}])):
            self.assertEqual("unresolved", self.decide(**(descriptor | evidence))["status"])

    def test_console_groups_findings_by_required_action(self):
        missing = self.decide(current=p.MISSING)
        foreign = self.decide(path="vendored.java", current=p.FOREIGN)
        candidate = {"path": "original.java", "ref": "b" * 40, "blob": "c" * 40}
        renamed = self.decide(path="new.java", inherited=False, candidates=[candidate, candidate])
        copied = self.decide(path="copied.java", inherited=False, in_comparison=False, ce_hash=None,
                             copies=[{"path": "source.java", "ref": "a" * 40, "similarity": 78}])
        changed = self.decide(curation=dict(contentHash=p.raw_hash(b"other"), header="pe-only", reason="Reviewed"))
        clean = self.decide(path="clean.java", current="apache")
        orphaned = dict(path="deleted.java", status="stale-curation", reason="file is no longer in scope")
        stderr, stdout = io.StringIO(), io.StringIO()
        with redirect_stderr(stderr), redirect_stdout(stdout):
            rows = [missing, foreign, renamed, copied, changed, clean]
            p.print_findings(rows, [orphaned], {"a" * 40: "ce/lts-4.2"})
            p.print_summary(rows, [orphaned], "target/report.json")
        output = stderr.getvalue()
        self.assertEqual(textwrap.dedent(f"""\
            Wrong header (2)
              a.java
                has no header, expected apache
                same path in CE, content identical
              vendored.java
                has unrecognized header, expected apache
                same path in CE, content identical
                not a ThingsBoard header, --fix leaves it for a manual review
              → Rerun with --fix to restamp automatically

            Needs a decision (2)
              new.java
                has pe-only, no expected header yet (hint: apache)
                exact CE match: original.java at {"b" * 11}
                another 1 candidate in the report
              copied.java
                has pe-only, no expected header yet
                similar CE file (78%): source.java at ce/lts-4.2
              → Decide the origin and add a curation, see {p.README}

            Stale curations (2)
              a.java
                file changed since the curation, re-review and update contentHash
              deleted.java
                file is no longer in scope
              → Remove or update the entry in {p.CURATIONS}

            6 findings in 6 files. Report: target/report.json
            """), output)
        self.assertEqual("", stdout.getvalue())

    def test_console_closes_an_interrupted_phase_before_reporting(self):
        stderr = io.StringIO()
        with redirect_stderr(stderr):
            p.console.begin("Resolving Git history")
            p.console.line()
            p.console.line("Error: boom")
        self.assertEqual("Resolving Git history ... failed\n\nError: boom\n", stderr.getvalue())

    def test_console_summarizes_a_clean_run(self):
        rows = [self.decide(current="apache"), self.decide(path="b.java", pe_hash="changed", current="ce-modified")]
        stderr = io.StringIO()
        with redirect_stderr(stderr):
            p.print_restamped(rows)
            p.print_summary(rows, [], "target/report.json")
        self.assertEqual("All 2 files carry the expected header.\n  apache 1 · ce-modified 1\n"
                         "Report: target/report.json\n", stderr.getvalue())

    def test_console_lists_restamped_files(self):
        rows = [self.decide(current="apache"), self.decide(path="b.java", pe_hash="changed", current="ce-modified")]
        rows[1]["previousHeader"] = p.LEGACY
        stderr = io.StringIO()
        with redirect_stderr(stderr):
            p.print_restamped(rows)
            p.print_summary(rows, [], "target/report.json")
        self.assertEqual("Restamped (1)\n  b.java\n    outdated or damaged ThingsBoard header replaced with ce-modified\n\n"
                         "All 2 files carry the expected header (1 restamped).\n  apache 1 · ce-modified 1\n"
                         "Report: target/report.json\n", stderr.getvalue())

    def test_console_summarizes_a_mass_restamp(self):
        rows = [self.decide(path=f"{index}.java", current="apache") for index in range(p.RESTAMP_LISTING + 1)]
        for index, row in enumerate(rows):
            row["previousHeader"] = p.LEGACY if index else p.MISSING
        stderr = io.StringIO()
        with redirect_stderr(stderr):
            p.print_restamped(rows)
        self.assertEqual(f"Restamped ({p.RESTAMP_LISTING + 1})\n  {p.RESTAMP_LISTING} × outdated or damaged ThingsBoard header replaced with apache\n"
                         "  1 × no header replaced with apache\n\n", stderr.getvalue())

    def test_foreign_headers_are_reviewed_by_hand(self):
        rows = [self.decide(current=p.FOREIGN)]
        stderr = io.StringIO()
        with redirect_stderr(stderr):
            p.print_findings(rows, [], {})
        self.assertIn("→ Review the existing header by hand, --fix does not replace unrecognized headers\n",
                      stderr.getvalue())


class InputTests(unittest.TestCase):
    def test_header_state_recognizes_legacy_thingsboard_headers(self):
        ce = b"/**\n * Copyright \xc2\xa9 2016-2025 The Thingsboard Authors\n *\n * Licensed under the Apache License\n */\n"
        pe = (b"#\n# ThingsBoard, Inc. (\"COMPANY\") CONFIDENTIAL\n#\n"
              b"# Copyright \xc2\xa9 2016-2026 ThingsBoard, Inc. All Rights Reserved.\n#\n")
        self.assertEqual(p.LEGACY, p.header_state(p.UNRECOGNIZED, ce))
        self.assertEqual(p.LEGACY, p.header_state(p.UNRECOGNIZED, pe))
        self.assertEqual(p.LEGACY, p.header_state(p.UNRECOGNIZED, b" * Copyright 2024 The Thingsboard Authors\n"))
        self.assertEqual(p.LEGACY, p.header_state(p.UNRECOGNIZED, b"// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors\n"
                                                                  b"// SPDX-License-Identifier: Apache-2.0 test\n"))
        self.assertEqual(p.LEGACY, p.header_state(p.UNRECOGNIZED, b"# SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.\n"))
        self.assertEqual(p.FOREIGN, p.header_state(p.UNRECOGNIZED, b"// Copyright 2020 Acme Corp\n// MIT\n"))
        self.assertEqual(p.MISSING, p.header_state(p.UNRECOGNIZED, b""))
        self.assertEqual(p.MISSING, p.header_state(p.UNRECOGNIZED, b"\n"))
        self.assertEqual("apache", p.header_state("apache", ce))
        self.assertEqual(p.LEGACY, p.header_state("apache", b"// SPDX-License-Identifier: Apache-2.0\n" + ce, True))

    def test_swap_header_rewrites_only_the_recognized_lines(self):
        old, new = [b"SPDX-A", b"SPDX-B"], [b"SPDX-X", b"SPDX-Y", b"SPDX-Z"]
        self.assertEqual(b"// SPDX-X\n// SPDX-Y\n// SPDX-Z\n// note\ncode\n",
                         p.swap_header(b"// SPDX-A\n// SPDX-B\n// note\ncode\n", old, new))
        self.assertEqual(b"#!/bin/sh\n#\n# SPDX-X\n# SPDX-Y\n# SPDX-Z\n#\n\nrun\n",
                         p.swap_header(b"#!/bin/sh\n#\n# SPDX-A\n# SPDX-B\n#\n\nrun\n", old, new))
        self.assertEqual(b"<!--\n\n    SPDX-X\n    SPDX-Y\n    SPDX-Z\n\n-->\n<a/>\n",
                         p.swap_header(b"<!--\n\n    SPDX-A\n    SPDX-B\n\n-->\n<a/>\n", old, new))
        self.assertEqual(b"// SPDX-X\r\n// SPDX-Y\r\n// SPDX-Z\r\ncode\r\n",
                         p.swap_header(b"// SPDX-A\r\n// SPDX-B\r\ncode\r\n", old, new))
        self.assertEqual(b"// SPDX-A\n// SPDX-B", p.swap_header(b"// SPDX-X\n// SPDX-Y\n// SPDX-Z", new, old))
        with self.assertRaises(p.Failure):
            p.swap_header(b"// SPDX-A\n\n// SPDX-B\ncode\n", old, new)
        with self.assertRaises(p.Failure):
            p.swap_header(b"code\n", old, new)

    def test_closes_block_accepts_only_complete_comment_blocks(self):
        for removed in (b"", b"/**\n * Copyright\n */\n\n", b"#\n# Copyright\n#\n\n", b"<!--\n  Copyright\n-->\n",
                        b"--\n-- Copyright\n--\n", b"/* Copyright */\n",
                        b"// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors\n// SPDX-License-Identifier: Apache-2.0 test\n",
                        b"///\n/// Copyright\n///\n// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors\n"
                        b"// SPDX-License-Identifier: Apache-2.0\n"):
            self.assertTrue(p.closes_block(removed), removed)
        for removed in (b"#\n# Copyright\n#\n# helper\n", b"// Copyright\n// eslint-disable\n",
                        b"// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors\n// eslint-disable\n"):
            self.assertFalse(p.closes_block(removed), removed)

    def test_extra_copyrights_flags_an_old_header_removed_with_the_spdx_lines(self):
        templates = {header: (this_repository() / name).read_bytes() for header, name in p.TEMPLATES.items()}
        spdx = b"// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors\n// SPDX-License-Identifier: Apache-2.0\n"
        old = b"///\n/// Copyright 2024 The Thingsboard Authors\n///\n"
        self.assertTrue(p.extra_copyrights("apache", spdx + old, templates))
        self.assertTrue(p.extra_copyrights("apache", old + spdx, templates))
        self.assertFalse(p.extra_copyrights("apache", spdx, templates))
        self.assertFalse(p.extra_copyrights("apache", spdx + b"// Copyright 2020 Acme Corp\n", templates))
        self.assertFalse(p.extra_copyrights(p.UNRECOGNIZED, spdx + old, templates))
        modified = b"".join(b"// " + line + b"\n" for line in templates["ce-modified"].splitlines())
        self.assertFalse(p.extra_copyrights("ce-modified", modified, templates))
        self.assertTrue(p.extra_copyrights("ce-modified", modified + old, templates))

    def test_split_removed_gives_back_the_line_after_a_one_line_header(self):
        self.assertEqual(b"class A {}\nclass B {}\n",
                         p.split_removed(b"// Copyright Acme\nclass A {}\nclass B {}\n", b"class B {}\n")[0])
        self.assertEqual(b"#!/bin/sh\nset -e\necho x\n",
                         p.split_removed(b"#!/bin/sh\n# Copyright Acme\nset -e\necho x\n", b"#!/bin/sh\necho x\n")[0])
        self.assertEqual(b"select 1;\nselect 2;\n",
                         p.split_removed(b"-- Copyright Acme\nselect 1;\nselect 2;\n", b"select 2;\n")[0])
        self.assertEqual(b"key: v\n", p.split_removed(b"# Copyright Acme\n# more\nkey: v\n", b"key: v\n")[0])
        self.assertEqual(b"code\n", p.split_removed(b"// Copyright Acme\n\ncode\n", b"code\n")[0])
        self.assertEqual(b"code\n", p.split_removed(b"/**\n * Copyright Acme\n */\ncode\n", b"code\n")[0])
        self.assertEqual(b"same\n", p.split_removed(b"same\n", b"same\n")[0])

    def test_split_removed_isolates_the_stripped_lines(self):
        original = b"#!/bin/sh\n#\n# Copyright 2024 Example\n#\n\necho one\necho two\n"
        self.assertEqual(b"#\n# Copyright 2024 Example\n#\n\n", p.split_removed(original, b"#!/bin/sh\necho one\necho two\n")[1])
        self.assertEqual(b"", p.split_removed(original, original)[1])
        self.assertEqual(b"// header\n", p.split_removed(b"// header\n", b"")[1])
        self.assertEqual(b"// header\r\n", p.split_removed(b"// header\r\nbody\r\n", b"body\r\n")[1])
        self.assertEqual(b"<!--\n  SPDX\n-->\n",
                         p.split_removed(b"<!--\n  SPDX\n-->\n<!--\n  Old\n-->\nbody\n", b"<!--\n  Old\n-->\nbody\n")[1])
        self.assertEqual(b"/**\n * SPDX\n */\n", p.split_removed(b"/**\n * SPDX\n */\n/**\n * Old\n */\n", b"/**\n * Old\n */\n")[1])

    def test_hash_contracts(self):
        self.assertEqual(p.comparison_hash(b"a\r\nb\r\n"), p.comparison_hash(b"a\nb\n"))
        self.assertNotEqual(p.raw_hash(b"a\r\n"), p.raw_hash(b"a\n"))
        self.assertNotEqual(p.comparison_hash(b"a \n"), p.comparison_hash(b"a\n"))
        self.assertNotEqual(p.comparison_hash(b"// comment\na\n"), p.comparison_hash(b"a\n"))

    def test_curation_validation(self):
        path = temporary_directory(self) / "curations.json"
        entry = dict(path="a.java", contentHash=p.raw_hash(b"a"), header="apache", reason="Reviewed")

        def write(entries):
            path.write_text(json.dumps(dict(schemaVersion=1, curations=entries)))

        write([entry])
        self.assertEqual({"a.java"}, set(p.load_curations(path)))
        for entries in ([entry, entry], [entry | {"reason": " "}],
                        [entry | {"path": "../a.java"}], [entry | {"header": "MIT"}]):
            write(entries)
            with self.assertRaises(p.Failure):
                p.load_curations(path)

    def test_plugin_version_and_configuration_come_from_root_pom(self):
        repo = temporary_directory(self)
        p.git(repo, "init")

        def write(version):
            (repo / "pom.xml").write_text(MANAGED_POM.format(version=version))

        write("9.9.9")
        plugin = p.read_plugin(repo)
        self.assertEqual("9.9.9", plugin.version)
        self.assertEqual("com.mycila:license-maven-plugin:9.9.9", plugin.coordinates)
        self.assertEqual("SLASHSTAR_STYLE", plugin.configuration.findtext("mapping/java"))
        for version in ("", "${license.plugin.version}"):
            write(version)
            with self.assertRaises(p.Failure):
                p.read_plugin(repo)
        (repo / "pom.xml").write_text(MODULE_POM.format(parent="../pom.xml", name="root"))
        with self.assertRaises(p.Failure):
            p.read_plugin(repo)


class GitHistoryTests(unittest.TestCase):
    def test_only_first_parent_snapshots_participate(self):
        git = GitFixture(temporary_directory(self), "lts-4.2")
        source = "name with spaces.java"
        git.write(source, "initial\n")
        initial = git.commit("initial")
        git("checkout", "-b", "side")
        git.write(source, "side intermediate\n")
        git.commit("intermediate")
        git.write(source, "merged result\n")
        git.commit("side final")
        git("checkout", "lts-4.2")
        git("merge", "--no-ff", "side", "-m", "integrate")
        merged = git("rev-parse", "HEAD")
        changes = p.history(git.root, "HEAD")
        self.assertEqual({initial, merged}, {change.commit for change in changes})
        self.assertEqual(source, changes[0].path)

        git("checkout", "side")
        git.write("side-only.java", "not integrated\n")
        git.commit("side only")
        git("checkout", "lts-4.2")
        self.assertNotIn("side-only.java", {change.path for change in p.history(git.root, "HEAD")})

        git("rm", source)
        git.commit("delete")
        deleted = p.history(git.root, "HEAD")[0]
        self.assertTrue(deleted.deleted)
        self.assertEqual(b"merged result\n", p.blobs(git.root, [deleted.old])[deleted.old])

    def test_integration_boundary_does_not_walk_preintegration_pe_history(self):
        git = GitFixture(temporary_directory(self), "ce")
        git.write("initial", "initial")
        git.commit("initial")
        git("branch", "pe")
        git.write("ce-file", "CE")
        anchor = git.commit("CE addition")
        git("checkout", "pe")
        git.write("pe-file", "PE")
        git.commit("PE addition")
        git("merge", "ce", "-m", "CE integration")
        boundary = git("rev-parse", "HEAD")
        git.write("pe-file", "PE edit")
        git.commit("PE edit")
        self.assertEqual(boundary, p.integration_boundary(git.root, anchor, "HEAD"))

    def test_ignore_filter_keeps_tracked_and_unignored_files(self):
        repo = temporary_directory(self)
        p.git(repo, "init")
        (repo / ".gitignore").write_text("*.java\n")
        paths = ["tracked.java", "local file.java", "new.py"]
        for name in paths:
            (repo / name).write_text("source\n")
        p.git(repo, "add", "-f", "tracked.java")
        self.assertEqual({"local file.java"}, p.git_ignored(repo, paths))
        self.assertEqual(set(), p.git_ignored(repo, ["tracked.java", "new.py"]))
        self.assertEqual(set(), p.git_ignored(repo, []))
        (repo / ".git/info/exclude").write_text("new.py\n")
        self.assertEqual({"local file.java", "new.py"}, p.git_ignored(repo, paths))

    def test_normalized_copy_search_is_bounded_and_ignores_empty_sources(self):
        repo = temporary_directory(self)
        for name in p.TEMPLATES.values():
            (repo / name).write_text("fixture header\n")
        tool = p.Mycila(repo, temporary_directory(self), fixture_plugin())
        source, empty = ("Source.java", "blob"), ("Empty.java", "empty")
        original = b"class Source {\n" + b"    void action() { doUsefulWork(); }\n" * 20 + b"}\n"
        tool.add(source, "Source.java", original)
        tool.add(empty, "Empty.java", b"\n")
        tool.add(("pe", "Renamed.java"), "Renamed.java",
                 original.replace(b"Source", b"Renamed").replace(b"\n", b"\r\n"))
        tool.add(("pe", "Other.java"), "Other.java", b"independent PE implementation\n")
        tool.add(("pe", "Blank.java"), "Blank.java", b"\r\n")
        found = p.normalized_copies(tool, {source: "selected-mainline", empty: "selected-mainline"},
                                    ["Renamed.java", "Other.java", "Blank.java"], tool.nonempty())
        self.assertEqual({"Renamed.java"}, set(found))
        self.assertEqual("Source.java", found["Renamed.java"][0]["path"])
        self.assertEqual("selected-mainline", found["Renamed.java"][0]["ref"])


class FetchTests(unittest.TestCase):
    def setUp(self):
        self.ce = GitFixture(temporary_directory(self), "lts-4.2")
        self.ce.write("Base.java", "base\n")
        self.first = self.ce.commit("first")
        self.ce("tag", "-a", "v4.2.0", "-m", "release")
        self.ce.write("Base.java", "second\n")
        self.second = self.ce.commit("second")
        self.pe = GitFixture(temporary_directory(self), "pe")
        self.pe.write("Pe.java", "pe\n")
        self.pe.commit("pe base")

    def fetch(self, ref):
        return p.fetch(self.pe.root, str(self.ce.root), ref)

    def test_fetches_branches_tags_and_commits(self):
        self.assertEqual(self.second, self.fetch("lts-4.2"))
        self.assertEqual(self.second, self.fetch("refs/heads/lts-4.2"))
        self.assertEqual(self.first, self.fetch("v4.2.0"))
        self.assertEqual(self.first, self.fetch(self.first))
        self.assertEqual("second", self.pe("show", f"{self.second}:Base.java"))

    def test_every_run_fetches_the_current_tip(self):
        self.assertEqual(self.second, self.fetch("lts-4.2"))
        self.ce.write("Base.java", "third\n")
        third = self.ce.commit("third")
        self.assertEqual(third, self.fetch("lts-4.2"))

    def test_missing_ref_and_option_like_ref_are_rejected(self):
        with self.assertRaisesRegex(p.Failure, r"(?s)cannot fetch 'missing' from .*couldn't find remote ref missing"):
            self.fetch("missing")
        with self.assertRaisesRegex(p.Failure, "cannot start with '-'"):
            self.fetch("--upload-pack=true")


class CheckCommandTests(unittest.TestCase):
    def setUp(self):
        self.repo = temporary_directory(self)
        self.git = git = GitFixture(self.repo, "lts-4.2")
        self.body = "new CE feature implementation\n" * 20
        git.write("Base.java", "base implementation\n")
        git.commit("release base")
        git("branch", "pe")
        git("checkout", "-b", "fix/my-fix")
        git.write("Feature.java", self.body)
        git.write("Deleted.java", "historical CE feature source\n")
        git.commit("CE fix sources")
        git("rm", "Deleted.java")
        self.feature = git.commit("remove temporary source")
        git("checkout", "-b", "unrelated", "lts-4.2")
        git.write("Unrelated.java", "unrelated source\n")
        git.commit("unrelated source")
        git("checkout", "pe")
        git.write("Renamed.java", fixture("pe-only") + self.body)
        git.write("Deleted.java", fixture("pe-only") + "historical CE feature source\n")
        git.write("Local.java", fixture("pe-only") + "unrelated source\n")
        git.write("Swapped.java", fixture("apache") + "swapped source\n")
        for category, name in p.TEMPLATES.items():
            git.write(name, f"fixture {category}\n")
        git.write(p.CURATIONS, '{"schemaVersion": 1, "curations": []}')
        self.output = self.repo / "target/report.json"

    def check(self, ref, scope, fix=False):
        def recognized(content):
            for header in p.TEMPLATES:
                if content.startswith(fixture(header).encode()):
                    return header
            return p.UNRECOGNIZED

        def split(content):
            first, _, rest = content.partition(b"\n")
            return (first + b"\n", rest) if first.startswith(b"// ") else (b"", content)

        def current_headers(tool, keys):
            return {key: recognized((tool.root / tool.paths[key]).read_bytes()) for key in keys}

        def strip_headers(tool):
            tool.removed = {key: split(body)[0] for key, body in tool.contents.items()}
            tool.stacked = set()
            return {key: p.comparison_hash(split(body)[1]) for key, body in tool.contents.items()}

        def restamp(tool, expected):
            for key, header in expected.items():
                (tool.root / tool.paths[key]).write_bytes(fixture(header).encode() + split(tool.contents[key])[1])
            return {key: (tool.root / tool.paths[key]).read_bytes() for key in expected}, {}

        with (patch.object(p.Path, "cwd", return_value=self.repo),
              patch.object(p, "read_plugin", return_value=fixture_plugin()),
              patch.object(p, "discover_scope", return_value=p.Scope(scope, [], [], 0)),
              patch.object(p.Mycila, "current_headers", current_headers),
              patch.object(p.Mycila, "strip_headers", strip_headers),
              patch.object(p.Mycila, "restamp", restamp),
              redirect_stdout(io.StringIO()), redirect_stderr(io.StringIO())):
            code = p.main(["check", "--ce-ref", ref, "--ce-remote", str(self.repo), "--output", str(self.output)]
                          + (["--fix"] if fix else []))
        return code, json.loads(self.output.read_text())

    def test_matching_mainline_passes(self):
        code, report = self.check("lts-4.2", ["Renamed.java", "Deleted.java", "Local.java"])
        self.assertEqual(0, code)
        self.assertEqual({"match": 3}, report["summary"])
        self.assertEqual({"remote": str(self.repo), "input": "lts-4.2", "commit": self.git("rev-parse", "lts-4.2")},
                         report["ceComparison"])

    def test_ce_ref_is_fetched_from_the_public_ce_repository_by_default(self):
        fetched = []

        def fetch(repo, remote, ref):
            fetched.append((remote, ref))
            return p.resolve(repo, ref)

        with (patch.object(p.Path, "cwd", return_value=self.repo),
              patch.object(p, "fetch", fetch),
              patch.object(p, "read_plugin", return_value=fixture_plugin()),
              patch.object(p, "discover_scope", return_value=p.Scope([], [], [], 0)),
              redirect_stdout(io.StringIO()), redirect_stderr(io.StringIO())):
            code = p.main(["check", "--ce-ref", "lts-4.2", "--output", str(self.output)])
        self.assertEqual(0, code)
        self.assertEqual([(p.CE_REMOTE, "lts-4.2")], fetched)

    def test_unfetchable_ce_ref_fails_before_the_history_walk(self):
        stderr = io.StringIO()
        with (patch.object(p.Path, "cwd", return_value=self.repo),
              patch.object(p, "read_plugin", return_value=fixture_plugin()),
              redirect_stdout(io.StringIO()), redirect_stderr(stderr)):
            code = p.main(["check", "--ce-ref", "missing", "--ce-remote", str(self.repo), "--output", str(self.output)])
        self.assertEqual(2, code)
        self.assertFalse(self.output.exists())
        self.assertIn(f"Fetching missing from {self.repo} ... failed\n", stderr.getvalue())
        self.assertIn(f"Error: cannot fetch 'missing' from {self.repo}.", stderr.getvalue())

    def test_feature_ref_exposes_rename_and_history_candidates(self):
        code, report = self.check("fix/my-fix", ["Renamed.java", "Deleted.java", "Local.java"])
        self.assertEqual(1, code)
        rows = {row["path"]: row for row in report["files"]}
        self.assertEqual("unresolved", rows["Renamed.java"]["status"])
        candidate = rows["Renamed.java"]["candidates"][0]
        self.assertEqual(("Feature.java", self.feature), (candidate["path"], candidate["ref"]))
        self.assertEqual("unresolved", rows["Deleted.java"]["status"])
        self.assertTrue(rows["Deleted.java"]["historicalCandidates"])
        self.assertEqual("pe-only", rows["Local.java"]["expectedHeader"])
        self.assertEqual({"tip": self.feature, "firstParent": True}, report["ceHistory"])

    def test_curation_for_deleted_file_fails_the_check(self):
        curation = dict(path="Gone.java", contentHash=p.raw_hash(b"gone"), header="pe-only", reason="Reviewed")
        self.git.write(p.CURATIONS, json.dumps(dict(schemaVersion=1, curations=[curation])))
        code, report = self.check("lts-4.2", ["Renamed.java", "Deleted.java", "Local.java"])
        self.assertEqual(1, code)
        self.assertEqual({"match": 3}, report["summary"])
        self.assertEqual([dict(path="Gone.java", status="stale-curation", reason="file is no longer in scope")],
                         report["curationIssues"])

    def test_fix_restamps_decided_headers_only(self):
        self.git("merge", "--no-ff", "fix/my-fix", "-m", "integrate CE fix")
        self.git.write("Base.java", FOREIGN_LINE.decode() + "base implementation\n")
        scope = ["Base.java", "Feature.java", "Renamed.java", "Swapped.java"]
        code, report = self.check(self.feature, scope)
        self.assertEqual(1, code)
        self.assertEqual({"mismatch": 3, "unresolved": 1}, report["summary"])
        self.assertEqual([], report["restamped"])
        rows = {row["path"]: row for row in report["files"]}
        self.assertEqual((p.FOREIGN, "apache"), (rows["Base.java"]["currentHeader"], rows["Base.java"]["expectedHeader"]))
        self.assertEqual((p.MISSING, "apache"),
                         (rows["Feature.java"]["currentHeader"], rows["Feature.java"]["expectedHeader"]))
        self.assertEqual(("apache", "pe-only"),
                         (rows["Swapped.java"]["currentHeader"], rows["Swapped.java"]["expectedHeader"]))

        code, report = self.check(self.feature, scope, fix=True)

        self.assertEqual(1, code)
        self.assertEqual(["Feature.java", "Swapped.java"], report["restamped"])
        self.assertEqual({"match": 2, "mismatch": 1, "unresolved": 1}, report["summary"])
        rows = {row["path"]: row for row in report["files"]}
        self.assertEqual(("match", "apache", p.MISSING),
                         (rows["Feature.java"]["status"], rows["Feature.java"]["currentHeader"],
                          rows["Feature.java"]["previousHeader"]))
        self.assertEqual(fixture("apache") + self.body, (self.repo / "Feature.java").read_text())
        self.assertEqual(fixture("pe-only") + "swapped source\n", (self.repo / "Swapped.java").read_text())
        self.assertEqual("apache", rows["Swapped.java"]["previousHeader"])
        self.assertEqual("mismatch", rows["Base.java"]["status"])
        self.assertNotIn("previousHeader", rows["Base.java"])
        self.assertEqual(FOREIGN_LINE.decode() + "base implementation\n", (self.repo / "Base.java").read_text())
        self.assertEqual(fixture("pe-only") + self.body, (self.repo / "Renamed.java").read_text())
        code, report = self.check(self.feature, scope, fix=True)
        self.assertEqual([], report["restamped"])
        self.assertEqual({"match": 2, "mismatch": 1, "unresolved": 1}, report["summary"])

    def test_integrated_ce_file_compares_against_shared_ancestor(self):
        self.git("merge", "--no-ff", "fix/my-fix", "-m", "integrate CE fix")
        _, report = self.check(self.feature, ["Feature.java"])
        self.assertEqual("apache", report["files"][0]["expectedHeader"])
        self.assertEqual(self.feature, report["sharedAncestor"])
        self.git.write("Feature.java", self.body + "PE-specific addition\n")
        _, report = self.check(self.feature, ["Feature.java"])
        self.assertEqual("ce-modified", report["files"][0]["expectedHeader"])


MANAGED_POM = textwrap.dedent("""\
    <project xmlns="http://maven.apache.org/POM/4.0.0">
      <modelVersion>4.0.0</modelVersion>
      <groupId>scope</groupId>
      <artifactId>root</artifactId>
      <version>1</version>
      <build>
        <pluginManagement>
          <plugins>
            <plugin>
              <groupId>com.mycila</groupId>
              <artifactId>license-maven-plugin</artifactId>
              <version>{version}</version>
              <configuration>
                <mapping><java>SLASHSTAR_STYLE</java></mapping>
              </configuration>
            </plugin>
          </plugins>
        </pluginManagement>
      </build>
    </project>
    """)

ROOT_POM = textwrap.dedent("""\
    <project xmlns="http://maven.apache.org/POM/4.0.0">
      <modelVersion>4.0.0</modelVersion>
      <groupId>scope</groupId>
      <artifactId>root</artifactId>
      <version>1</version>
      <packaging>pom</packaging>
      <modules>
        <module>one</module>
        <module>two</module>
      </modules>
      <build>
        <plugins>
          <plugin>
            <groupId>com.mycila</groupId>
            <artifactId>license-maven-plugin</artifactId>
            <version>{version}</version>
            <configuration>
              <licenseSets>
                <licenseSet>
                  <header>{header}</header>
                  <includes><include>src/**</include></includes>
                  <excludes><exclude>src/test/**</exclude></excludes>
                </licenseSet>
              </licenseSets>
            </configuration>
          </plugin>
        </plugins>
      </build>
    </project>
    """)

MODULE_POM = textwrap.dedent("""\
    <project>
      <modelVersion>4.0.0</modelVersion>
      <parent>
        <groupId>scope</groupId>
        <artifactId>root</artifactId>
        <version>1</version>
        <relativePath>{parent}</relativePath>
      </parent>
      <artifactId>{name}</artifactId>
    </project>
    """)

SAMPLE_PREFIX = {"java": "", "html": "", "xml": '<?xml version="1.0"?>\n', "sh": "#!/bin/sh\n"}
SAMPLE_BODY = {
    "java": "// Ordinary documentation\nclass Example {}\n",
    "html": "<!-- Ordinary documentation -->\n<div>Example</div>\n",
    "xml": "<!-- Ordinary documentation -->\n<example/>\n",
    "sh": "# Ordinary documentation\necho example\n",
}
LEGACY_TS = (b"///\n/// Copyright 2024 The Thingsboard Authors\n///\n"
             b"/// Licensed under the Apache License, Version 2.0\n///\n\nexport const legacy = 1;\n")
LEGACY_XML = (b"<!--\n\n    Copyright 2024 The Thingsboard Authors\n\n"
              b"    Licensed under the Apache License, Version 2.0\n\n-->\n<example/>\n")
LEGACY_JAVA = (b"/**\n * Copyright 2024 The Thingsboard Authors\n *\n"
               b" * Licensed under the Apache License, Version 2.0\n *\n * Ordinary license text\n */\n\n"
               b"class Legacy {}\n")
FOREIGN_LINE = b"// Copyright 2020 Acme Corp\n"


def fixture(category):
    return f"// fixture {category}\n"


def commented(header, extension):
    lines = header.splitlines()
    if extension in {"html", "xml"}:
        return "<!--\n\n" + "".join(f"    {line}\n" for line in lines) + "\n-->\n"
    if extension in {"java", "ts"}:
        return "".join(f"// {line}\n" for line in lines) + "\n"
    return "#\n" + "".join(f"# {line}\n" for line in lines) + "#\n\n"


def stamped(header, extension):
    text = commented(header, extension)
    return text[:-1] if extension in {"java", "ts"} else text


@unittest.skipUnless(os.environ.get("LICENSE_HEADERS_MAVEN_TESTS") == "1", "opt-in Maven fixtures")
class MavenTests(unittest.TestCase):
    def test_scope_reports_respect_module_relative_exclusions(self):
        repo = temporary_directory(self)
        p.git(repo, "init")
        (repo / "header.txt").write_text("SPDX-FileCopyrightText: Copyright Example\n"
                                         "SPDX-License-Identifier: Apache-2.0\n")
        plugin = fixture_plugin(p.read_plugin(this_repository()).version)
        (repo / "pom.xml").write_text(ROOT_POM.format(version=plugin.version, header=repo / "header.txt"))
        for module in ("one", "two", "msa/black-box-tests"):
            directory = repo / module
            directory.mkdir(parents=True)
            parent = "../../pom.xml" if "/" in module else "../pom.xml"
            (directory / "pom.xml").write_text(MODULE_POM.format(parent=parent, name=directory.name))
            for name in ("src/main/Included.java", "src/test/Excluded.java"):
                path = directory / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text("class Example {}\n")
        before = {str(path): path.read_bytes() for path in repo.rglob("*.java")}
        ignored = repo / "one/src/main/Local.java"
        ignored.write_text("class Local {}\n")
        (repo / ".git/info/exclude").write_text("one/src/main/Local.java\n")

        scope = p.discover_scope(repo, plugin)

        self.assertEqual(["msa/black-box-tests/src/main/Included.java", "one/src/main/Included.java",
                          "two/src/main/Included.java"], scope.eligible)
        self.assertEqual([], scope.unknown)
        self.assertEqual(4, len(scope.modules))
        self.assertEqual(1, scope.ignored)
        self.assertEqual(before | {str(ignored): b"class Local {}\n"},
                         {str(path): path.read_bytes() for path in repo.rglob("*.java")})

    def test_headers_and_ordinary_content(self):
        repo = this_repository()
        config = ET.fromstring("<configuration><mapping><java>SINGLE_LINE_DOUBLESLASH_STYLE</java>"
                               "<ts>SINGLE_LINE_DOUBLESLASH_STYLE</ts></mapping>"
                               "</configuration>")
        tool = p.Mycila(repo, temporary_directory(self), p.Plugin(p.read_plugin(repo).version, config))
        expected = {}
        for category, template in p.TEMPLATES.items():
            header = (repo / template).read_text()
            for extension in SAMPLE_BODY:
                for eol in ("\n", "\r\n"):
                    key = ("pe", category + extension + repr(eol))
                    sample = SAMPLE_PREFIX[extension] + commented(header, extension) + SAMPLE_BODY[extension]
                    tool.add(key, "sample." + extension, sample.replace("\n", eol).encode())
                    expected[key] = p.comparison_hash((SAMPLE_PREFIX[extension] + SAMPLE_BODY[extension]).encode())
        missing = ("pe", "missing")
        tool.add(missing, "missing.java", b"// Ordinary documentation\nclass Missing {}\n")
        expected[missing] = p.comparison_hash(b"// Ordinary documentation\nclass Missing {}\n")
        legacy = ("ce", "legacy")
        tool.add(legacy, "legacy.java", LEGACY_JAVA)
        expected[legacy] = p.comparison_hash(b"class Legacy {}\n")
        foreign = ("pe", "foreign")
        tool.add(foreign, "foreign.java", FOREIGN_LINE + b"class Foreign {}\n")
        expected[foreign] = p.comparison_hash(b"class Foreign {}\n")
        one_line = ("pe", "one-line")
        tool.add(one_line, "one.sh", b"#!/bin/sh\n# Copyright 2024 The Thingsboard Authors\nset -e\necho x\n")
        expected[one_line] = p.comparison_hash(b"#!/bin/sh\nset -e\necho x\n")
        spdx = stamped((repo / p.TEMPLATES["apache"]).read_text(), "java").encode()
        stacked = ("pe", "stacked")
        tool.add(stacked, "stacked.java", spdx + LEGACY_JAVA)
        expected[stacked] = p.comparison_hash(b"class Legacy {}\n")
        reversed_stack = ("pe", "reversed")
        tool.add(reversed_stack, "reversed.java", LEGACY_JAVA.replace(b"class Legacy {}\n", spdx + b"class Legacy {}\n"))
        expected[reversed_stack] = p.comparison_hash(b"class Legacy {}\n")
        mentioned = ("pe", "mentioned")
        tool.add(mentioned, "mentioned.java", spdx + b"\n" + FOREIGN_LINE + b"class Mentioned {}\n")
        expected[mentioned] = p.comparison_hash(FOREIGN_LINE + b"class Mentioned {}\n")
        pairs = {}
        for extension, legacy_file, body in (("ts", LEGACY_TS, b"export const legacy = 1;\n"),
                                             ("xml", LEGACY_XML, b"<example/>\n")):
            new = stamped((repo / p.TEMPLATES["apache"]).read_text(), extension).encode()
            old = legacy_file[:-len(body)]
            for order, content in (("new-old", new + old + body), ("old-new", old + new + body)):
                key = ("pe", order + "." + extension)
                pairs[key] = content
                tool.add(key, key[1], content)
                expected[key] = p.comparison_hash(body)
        header_only = {}
        for extension in SAMPLE_BODY:
            key = ("pe", "only." + extension)
            header_only[key] = SAMPLE_PREFIX[extension] + commented((repo / p.TEMPLATES["pe-only"]).read_text(), extension)
            tool.add(key, "only." + extension, header_only[key].encode())
            expected[key] = p.comparison_hash(SAMPLE_PREFIX[extension].encode())

        headers = tool.current_headers([key for key in expected if key[0] == "pe"])
        recognized = dict(headers)

        self.assertEqual(p.UNRECOGNIZED, headers.pop(missing))
        self.assertEqual(p.UNRECOGNIZED, headers.pop(foreign))
        self.assertEqual(p.UNRECOGNIZED, headers.pop(one_line))
        self.assertEqual("apache", headers.pop(stacked))
        self.assertEqual("apache", headers.pop(reversed_stack))
        self.assertEqual("apache", headers.pop(mentioned))
        for key in pairs:
            headers.pop(key)
        for key in header_only:
            self.assertEqual("pe-only", headers.pop(key), key)
        for key, header in headers.items():
            self.assertTrue(key[1].startswith(header), (key, header))
        self.assertEqual(expected, tool.strip_headers())
        self.assertEqual(p.MISSING, p.header_state(p.UNRECOGNIZED, tool.removed[missing]))
        self.assertEqual(p.LEGACY, p.header_state(p.UNRECOGNIZED, tool.removed[legacy]))
        self.assertEqual(p.FOREIGN, p.header_state(p.UNRECOGNIZED, tool.removed[foreign]))
        self.assertEqual(p.LEGACY, p.header_state(p.UNRECOGNIZED, tool.removed[one_line]))
        self.assertLessEqual({stacked, reversed_stack}, tool.stacked)
        self.assertLessEqual(tool.stacked, {stacked, reversed_stack} | set(pairs))
        self.assertEqual(spdx + LEGACY_JAVA[:-len(b"class Legacy {}\n")], tool.removed[stacked])
        self.assertEqual(spdx + b"\n", tool.removed[mentioned])
        states = tool.header_states(recognized)
        for key, content in pairs.items():
            self.assertEqual(p.LEGACY, states[key], key)
            self.assertEqual(content[:len(tool.removed[key])], tool.removed[key], key)

    def test_restamp_reproduces_the_repository_layout(self):
        repo = this_repository()
        config = ET.fromstring("<configuration><mapping><java>SINGLE_LINE_DOUBLESLASH_STYLE</java>"
                               "<ts>SINGLE_LINE_DOUBLESLASH_STYLE</ts></mapping>"
                               "</configuration>")
        tool = p.Mycila(repo, temporary_directory(self), p.Plugin(p.read_plugin(repo).version, config))
        templates = {category: (repo / name).read_text() for category, name in p.TEMPLATES.items()}
        expected, wanted = {}, {}
        for category, header in templates.items():
            for extension in SAMPLE_BODY:
                for eol in ("\n", "\r\n"):
                    body = (SAMPLE_PREFIX[extension] + SAMPLE_BODY[extension]).replace("\n", eol)
                    key = ("pe", "missing-" + category + extension + repr(eol))
                    tool.add(key, "sample." + extension, body.encode())
                    wanted[key] = category
                    expected[key] = (SAMPLE_PREFIX[extension] + stamped(header, extension) + SAMPLE_BODY[extension]).replace("\n", eol).encode()
        legacy = ("pe", "legacy")
        tool.add(legacy, "legacy.java", LEGACY_JAVA)
        wanted[legacy], expected[legacy] = "apache", (stamped(templates["apache"], "java") + "class Legacy {}\n").encode()
        confidential = ("pe", "confidential")
        tool.add(confidential, "start.sh", b"#!/bin/bash\n#\n# ThingsBoard, Inc. (\"COMPANY\") CONFIDENTIAL\n#\n"
                                           b"# Copyright \xc2\xa9 2016-2026 ThingsBoard, Inc. All Rights Reserved.\n#\n\nset -e\n")
        wanted[confidential], expected[confidential] = "pe-only", ("#!/bin/bash\n" + stamped(templates["pe-only"], "sh") + "set -e\n").encode()
        header_only = ("pe", "only")
        tool.add(header_only, "only.java", b"/**\n * Copyright 2024 The Thingsboard Authors\n */\n")
        wanted[header_only], expected[header_only] = "ce-modified", stamped(templates["ce-modified"], "java").encode()
        shebang = ("pe", "shebang")
        tool.add(shebang, "shebang.sh", b"#!/bin/sh\n#\n# Copyright 2024 The Thingsboard Authors\n#\n")
        wanted[shebang], expected[shebang] = "pe-only", ("#!/bin/sh\n" + stamped(templates["pe-only"], "sh")).encode()
        glued = ("pe", "glued")
        tool.add(glued, "glued.sh", b"#\n# Copyright 2024 The Thingsboard Authors\n#\n# helper\nset -e\n")
        wanted[glued] = "pe-only"
        doubled = ("pe", "doubled")
        tool.add(doubled, "doubled.java", LEGACY_JAVA.replace(b"class Legacy {}\n", LEGACY_JAVA))
        wanted[doubled], expected[doubled] = "pe-only", (stamped(templates["pe-only"], "java") + "class Legacy {}\n").encode()
        damaged = ("pe", "damaged")
        tool.add(damaged, "damaged.java", stamped(templates["apache"], "java").replace("Apache-2.0", "Apache-2.0 test").encode()
                 + b"class Damaged {}\n")
        wanted[damaged], expected[damaged] = "apache", (stamped(templates["apache"], "java") + "class Damaged {}\n").encode()
        truncated = ("pe", "truncated")
        tool.add(truncated, "truncated.java", b"// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors\nclass Truncated {}\n")
        wanted[truncated], expected[truncated] = "apache", (stamped(templates["apache"], "java") + "class Truncated {}\n").encode()
        vendored = ("pe", "vendored")
        tool.add(vendored, "vendored.java", LEGACY_JAVA.replace(b"class Legacy {}\n", FOREIGN_LINE + b"// MIT\nclass Legacy {}\n"))
        wanted[vendored] = "pe-only"
        spdx = stamped(templates["apache"], "java").encode()
        stacked = ("pe", "stacked")
        tool.add(stacked, "stacked.java", spdx + LEGACY_JAVA)
        wanted[stacked], expected[stacked] = "ce-modified", (stamped(templates["ce-modified"], "java") + "class Legacy {}\n").encode()
        reversed_stack = ("pe", "reversed")
        tool.add(reversed_stack, "reversed.java", LEGACY_JAVA.replace(b"class Legacy {}\n", spdx + b"\nclass Legacy {}\n"))
        wanted[reversed_stack], expected[reversed_stack] = "apache", (stamped(templates["apache"], "java") + "class Legacy {}\n").encode()
        mentioned = ("pe", "mentioned")
        tool.add(mentioned, "mentioned.java", spdx + b"\n" + FOREIGN_LINE + b"class Mentioned {}\n")
        wanted[mentioned] = "pe-only"
        for extension, legacy_file, body in (("ts", LEGACY_TS, b"export const legacy = 1;\n"),
                                             ("xml", LEGACY_XML, b"<example/>\n")):
            new = stamped(templates["apache"], extension).encode()
            old = legacy_file[:-len(body)]
            for order, content in (("new-old", new + old + body), ("old-new", old + new + body)):
                key = ("pe", order + "." + extension)
                tool.add(key, key[1], content)
                wanted[key] = "ce-modified"
                expected[key] = stamped(templates["ce-modified"], extension).encode() + body

        result, rejected = tool.restamp(wanted)

        self.assertEqual({glued, vendored, mentioned}, set(rejected))
        self.assertEqual(expected, result)
        self.assertEqual({key: category for key, category in wanted.items() if key not in rejected},
                         tool.current_headers(list(result)))


if __name__ == "__main__":
    unittest.main()
