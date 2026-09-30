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
    VERSION = {"path": "a.java", "ref": "b" * 40, "blob": "c" * 40}
    COPY = {"path": "source.java", "ref": "a" * 40, "blob": "c" * 40, "similarity": 78}

    def decide(self, **changes):
        evidence = dict(path="a.java", content_hash=p.raw_hash(b"reviewed"), current=p.NON_CE,
                        ce_versions=(self.VERSION,), identical=self.VERSION)
        return p.decide(p.Evidence(**(evidence | changes)))

    def new(self, **changes):
        return self.decide(**(dict(ce_versions=(), identical=None) | changes))

    def test_current_header_does_not_establish_origin(self):
        row = self.decide()
        self.assertEqual(("mismatch", "apache"), (row["status"], row["expectedHeader"]))
        self.assertEqual(self.VERSION, row["identicalTo"])
        row = self.decide(identical=None)
        self.assertEqual(("ce-modified", "same path in CE, content differs"), (row["expectedHeader"], row["reason"]))
        self.assertEqual([self.VERSION], row["ceVersions"])

    def test_clean_merge_of_ce_versions_is_apache(self):
        merge = {"path": "a.java", "merge": ["b" * 40, "d" * 40], "base": "e" * 40}
        row = self.decide(current="apache", identical=merge)
        self.assertEqual(("match", "apache"), (row["status"], row["expectedHeader"]))
        self.assertEqual("same path in CE, content identical to a clean merge of its CE versions", row["reason"])
        self.assertEqual(merge, row["identicalTo"])

    def test_new_path_without_ce_evidence_is_non_ce(self):
        row = self.new()
        self.assertEqual(("match", p.NON_CE, "not in CE, no CE evidence"),
                         (row["status"], row["expectedHeader"], row["reason"]))

    def test_ce_evidence_for_a_new_path_needs_a_decision(self):
        row = self.new(copies=(self.COPY,))
        self.assertEqual(("unresolved", None, None), (row["status"], row["expectedHeader"], row["suggestedHeader"]))
        self.assertEqual([self.COPY], row["copyCandidates"])
        self.assertEqual("apache", self.new(copies=(self.COPY | {"similarity": 100},))["suggestedHeader"])
        deleted = {"path": "a.java", "ref": "f" * 40 + "^1", "blob": "c" * 40}
        row = self.new(deleted=deleted)
        self.assertEqual(("unresolved", "CE history had this path"), (row["status"], row["reason"]))
        self.assertEqual(deleted, row["historicalCandidate"])

    def test_descriptor_of_a_module_absent_in_ce_is_non_ce_despite_similar_ce_descriptors(self):
        copy = self.COPY | {"path": "transport/mqtt/pom.xml"}
        descriptor = dict(path="integration/mqtt/pom.xml", copies=(copy,), directory_in_ce=False)
        row = self.new(**descriptor)
        self.assertEqual(("match", p.NON_CE), (row["status"], row["expectedHeader"]))
        for evidence in (dict(directory_in_ce=True),
                         dict(path="integration/mqtt/Mqtt.java"),
                         dict(copies=(copy | {"similarity": 100},)),
                         dict(deleted={"path": "integration/mqtt/pom.xml", "ref": "f" * 40, "blob": "c" * 40})):
            self.assertEqual("unresolved", self.new(**(descriptor | evidence))["status"], evidence)

    def test_matching_curation_precedes_inference(self):
        curation = dict(contentHash=p.raw_hash(b"reviewed"), header="ce-modified", reason="Reviewed")
        row = self.decide(current="ce-modified", curation=curation)
        self.assertEqual(("match", "ce-modified", "curation: Reviewed"),
                         (row["status"], row["expectedHeader"], row["reason"]))
        self.assertNotIn("redundantCuration", row)
        row = self.new(copies=(self.COPY,), curation=curation | {"header": p.NON_CE})
        self.assertEqual(("match", p.NON_CE), (row["status"], row["expectedHeader"]))
        self.assertNotIn("redundantCuration", row)
        curation["contentHash"] = p.raw_hash(b"different")
        self.assertEqual("stale-curation", self.decide(curation=curation)["status"])

    def test_curation_that_repeats_the_inferred_header_is_redundant(self):
        curation = dict(contentHash=p.raw_hash(b"reviewed"), header="apache", reason="Reviewed")
        row = self.decide(current="apache", curation=curation)
        self.assertEqual(("match", "apache", "same path in CE, content identical", True),
                         (row["status"], row["expectedHeader"], row["reason"], row["redundantCuration"]))
        row = self.new(curation=curation | {"header": p.NON_CE})
        self.assertTrue(row["redundantCuration"])

    def test_console_groups_findings_by_required_action(self):
        missing = self.decide(current=p.MISSING)
        foreign = self.decide(path="vendored.java", current=p.FOREIGN)
        copied = self.new(path="copied.java", copies=(self.COPY, self.COPY | {"path": "other.java", "similarity": 60}))
        historical = self.new(path="gone.java", deleted={"path": "gone.java", "ref": "d" * 40 + "^1", "blob": "c" * 40})
        changed = self.decide(curation=dict(contentHash=p.raw_hash(b"other"), header=p.NON_CE, reason="Reviewed"))
        clean = self.decide(path="clean.java", current="apache")
        redundant = self.decide(path="curated.java", current="apache",
                                curation=dict(contentHash=p.raw_hash(b"reviewed"), header="apache", reason="Reviewed"))
        orphaned = dict(path="deleted.java", status="stale-curation", reason="file is no longer in scope")
        stderr, stdout = io.StringIO(), io.StringIO()
        with redirect_stderr(stderr), redirect_stdout(stdout):
            rows = [missing, foreign, copied, historical, changed, clean, redundant]
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
              copied.java
                has {p.NON_CE}, no expected header yet
                similar CE file (78%): source.java at ce/lts-4.2
                another 1 candidate in the report
              gone.java
                has {p.NON_CE}, no expected header yet
                CE history: gone.java at {"d" * 11}^1
              → Decide the origin and add a curation, see {p.README}

            Stale curations (2)
              a.java
                file changed since the curation, re-review and update contentHash
              deleted.java
                file is no longer in scope
              → Review the file and update the entry in {p.CURATIONS}; --fix removes entries of files no longer in scope

            Redundant curations (1)
              curated.java
                curated apache, the check decides apache by itself
              → Rerun with --fix to remove them from {p.CURATIONS}

            6 findings in 7 files. Report: target/report.json
            """), output)
        self.assertEqual("", stdout.getvalue())

    def test_orphaned_curations_alone_point_to_fix(self):
        orphaned = dict(path="deleted.java", status="stale-curation", reason="file is no longer in scope")
        stderr = io.StringIO()
        with redirect_stderr(stderr):
            p.print_findings([self.decide(current="apache")], [orphaned], {})
        self.assertIn("  → Rerun with --fix to remove them\n", stderr.getvalue())

    def test_console_lists_removed_curations(self):
        stderr = io.StringIO()
        with redirect_stderr(stderr):
            p.print_removed_curations([{"path": "a.java", "reason": "file is no longer in scope"}])
            p.print_removed_curations([])
        self.assertEqual("Removed curations (1)\n  a.java\n    file is no longer in scope\n\n", stderr.getvalue())

    def test_console_closes_an_interrupted_phase_before_reporting(self):
        stderr = io.StringIO()
        with redirect_stderr(stderr):
            p.console.begin("Resolving Git history")
            p.console.line()
            p.console.line("Error: boom")
        self.assertEqual("Resolving Git history ... failed\n\nError: boom\n", stderr.getvalue())

    def test_console_summarizes_a_clean_run(self):
        rows = [self.decide(current="apache"), self.decide(path="b.java", identical=None, current="ce-modified")]
        stderr = io.StringIO()
        with redirect_stderr(stderr):
            p.print_restamped(rows)
            p.print_summary(rows, [], "target/report.json")
        self.assertEqual("All 2 files carry the expected header.\n  apache 1 · ce-modified 1\n"
                         "Report: target/report.json\n", stderr.getvalue())

    def test_console_lists_restamped_files(self):
        rows = [self.decide(current="apache"), self.decide(path="b.java", identical=None, current="ce-modified")]
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
                        [entry | {"path": "../a.java"}], [entry | {"header": "MIT"}],
                        [entry | {"header": "pe-only"}]):
            write(entries)
            with self.assertRaises(p.Failure):
                p.load_curations(path)

    def test_non_ce_header_is_the_only_other_template(self):
        directory = temporary_directory(self)
        for name in ("license-header.txt", "license-header-ce-modified.txt", "notes.txt"):
            (directory / name).write_text("header\n")
        with self.assertRaises(RuntimeError):
            p.non_ce_header(directory)
        (directory / "license-header-busl.txt").write_text("header\n")
        self.assertEqual("busl", p.non_ce_header(directory))
        (directory / "license-header-pe.txt").write_text("header\n")
        with self.assertRaises(RuntimeError):
            p.non_ce_header(directory)
        self.assertEqual(f"{p.TEMPLATE_DIRECTORY}/license-header-{p.NON_CE}.txt", p.TEMPLATES[p.NON_CE])
        self.assertTrue((this_repository() / p.TEMPLATES[p.NON_CE]).is_file())

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

        git("rm", source)
        git.commit("delete")
        deleted = p.history(git.root, "HEAD", "--diff-filter=D")
        self.assertEqual(["D"], [change.status for change in deleted])
        self.assertEqual(b"merged result\n", p.blobs(git.root, [deleted[0].old])[deleted[0].old])

    def relicensed_fixture(self):
        git = GitFixture(temporary_directory(self), "master")
        git.write("Base.java", "base\n")
        git.commit("CE base")
        git("checkout", "-b", "lts-4.3")
        git.write("Fix.java", "lts fix\n")
        fix = git.commit("CE fix")
        git("checkout", "master")
        git.write("Feature.java", "CE feature\n")
        last = git.commit("last Apache commit")
        git.write("Tb.java", "TB feature\n")
        relicensing = git.commit("relicensing")
        git("merge", "--no-ff", "lts-4.3", "-m", "integrate CE fix")
        return git, fix, last, relicensing

    def test_relicensed_ce_bases_are_the_latest_ce_commits_merged_into_the_branch(self):
        git, fix, last, relicensing = self.relicensed_fixture()
        with patch.object(p, "RELICENSING_COMMIT", relicensing):
            self.assertEqual(sorted([fix, last]), p.relicensed_ce_bases(git.root, git("rev-parse", "HEAD")))
            git("checkout", "lts-4.3")
            git.write("Fix.java", "second lts fix\n")
            second = git.commit("second CE fix")
            git("checkout", "master")
            self.assertEqual(sorted([fix, last]), p.relicensed_ce_bases(git.root, git("rev-parse", "HEAD")))
            git("merge", "--no-ff", "lts-4.3", "-m", "integrate second CE fix")
            self.assertEqual(sorted([second, last]), p.relicensed_ce_bases(git.root, git("rev-parse", "HEAD")))
            self.assertEqual([last], p.relicensed_ce_bases(git.root, relicensing))

    def test_relicensed_ce_bases_need_the_relicensing_commit_in_history(self):
        git, _, _, relicensing = self.relicensed_fixture()
        with patch.object(p, "RELICENSING_COMMIT", relicensing), self.assertRaisesRegex(p.Failure, "Pass --ce-ref"):
            p.relicensed_ce_bases(git.root, git("rev-parse", "lts-4.3"))
        with patch.object(p, "RELICENSING_COMMIT", "0" * 40), self.assertRaisesRegex(p.Failure, "Pass --ce-ref"):
            p.relicensed_ce_bases(git.root, git("rev-parse", "HEAD"))

    def test_merged_ce_bases_follow_the_integrated_ce_commit(self):
        git = GitFixture(temporary_directory(self), "ce")
        git.write("Base.java", "base\n")
        git.commit("CE base")
        git("checkout", "-b", "integrating")
        git.write("Own.java", "own\n")
        git.commit("own addition")
        git("checkout", "ce")
        git.write("Base.java", "CE fix\n")
        integrated = git.commit("CE fix")
        git("checkout", "integrating")
        git("merge", "--no-ff", "ce", "-m", "integrate CE")
        git("checkout", "ce")
        git.write("Base.java", "later CE fix\n")
        tip = git.commit("not yet integrated")
        self.assertEqual([integrated], p.merged_ce_bases(git.root, git("rev-parse", "integrating"), tip))
        git("checkout", "--orphan", "unrelated")
        git.write("Other.java", "other\n")
        unrelated = git.commit("unrelated history")
        with self.assertRaisesRegex(p.Failure, "shares no history"):
            p.merged_ce_bases(git.root, unrelated, tip)

    def test_merged_ce_bases_below_a_relicensed_ce_ref_exclude_relicensed_commits(self):
        git, fix, last, relicensing = self.relicensed_fixture()
        ce_tip = git("rev-parse", "HEAD")
        git("checkout", "-b", "downstream", git("rev-list", "--max-parents=0", "HEAD"))
        git.write("Own.java", "own\n")
        own = git.commit("own history without the relicensing commit")
        git("merge", "--no-ff", "master", "-m", "integrate CE master")
        head = git("rev-parse", "HEAD")
        with patch.object(p, "RELICENSING_COMMIT", relicensing):
            self.assertIn(own, p.relicensed_ce_bases(git.root, head))
            self.assertEqual(sorted([fix, last]), p.merged_ce_bases(git.root, head, ce_tip))

    def test_lineage_records_first_parent_ce_deletions(self):
        git = GitFixture(temporary_directory(self), "ce")
        git.write("Gone.java", "removed from CE\n")
        git.write("Kept.java", "kept\n")
        git.commit("CE base")
        git("checkout", "-b", "side")
        git.write("Temporary.java", "never on the mainline\n")
        git.commit("side addition")
        git("rm", "Temporary.java")
        git.commit("side removal")
        git("checkout", "ce")
        git("merge", "--no-ff", "side", "-m", "integrate side")
        git("rm", "Gone.java")
        removal = git.commit("CE removal")
        lineage = p.Lineage.load(git.root, removal, [removal])
        self.assertEqual({"Gone.java"}, set(lineage.deleted))
        self.assertEqual(removal + "^1", lineage.deleted["Gone.java"]["ref"])
        self.assertEqual(b"removed from CE\n", p.blobs(git.root, [lineage.deleted["Gone.java"]["blob"]])
                         [lineage.deleted["Gone.java"]["blob"]])
        self.assertEqual([(removal, git("rev-parse", f"{removal}:Kept.java"))], lineage.versions("Kept.java"))
        self.assertEqual([], lineage.versions("Gone.java"))

    def test_merge_blobs_returns_only_clean_merges(self):
        git = GitFixture(temporary_directory(self), "main")
        git.write("File.java", "one\ntwo\nthree\nfour\nfive\n")
        git.commit("base")
        base = git("rev-parse", "HEAD:File.java")

        def blob(content):
            git.write("File.java", content)
            git.commit(content.splitlines()[0])
            return git("rev-parse", "HEAD:File.java")

        ours, theirs = blob("ONE\ntwo\nthree\nfour\nfive\n"), blob("one\ntwo\nthree\nfour\nFIVE\n")
        self.assertEqual(b"ONE\ntwo\nthree\nfour\nFIVE\n", p.merge_blobs(git.root, ours, base, theirs))
        self.assertIsNone(p.merge_blobs(git.root, ours, base, blob("uno\ntwo\nthree\nfour\nfive\n")))

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
            (repo / name).parent.mkdir(parents=True, exist_ok=True)
            (repo / name).write_text("fixture header\n")
        tool = p.Mycila(repo, temporary_directory(self), fixture_plugin())
        source, empty = ("Source.java", "blob"), ("Empty.java", "empty")
        original = b"class Source {\n" + b"    void action() { doUsefulWork(); }\n" * 20 + b"}\n"
        tool.add(source, "Source.java", original)
        tool.add(empty, "Empty.java", b"\n")
        tool.add(("head", "Renamed.java"), "Renamed.java",
                 original.replace(b"Source", b"Renamed").replace(b"\n", b"\r\n"))
        tool.add(("head", "Other.java"), "Other.java", b"independent implementation\n")
        tool.add(("head", "Blank.java"), "Blank.java", b"\r\n")
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
        self.local = GitFixture(temporary_directory(self), "main")
        self.local.write("Own.java", "own\n")
        self.local.commit("own base")

    def fetch(self, ref):
        return p.fetch(self.local.root, str(self.ce.root), ref)

    def test_fetches_branches_tags_and_commits(self):
        self.assertEqual(self.second, self.fetch("lts-4.2"))
        self.assertEqual(self.second, self.fetch("refs/heads/lts-4.2"))
        self.assertEqual(self.first, self.fetch("v4.2.0"))
        self.assertEqual(self.first, self.fetch(self.first))
        self.assertEqual("second", self.local("show", f"{self.second}:Base.java"))

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
    LINES = "".join(f"line {index}\n" for index in range(10))

    def setUp(self):
        self.repo = temporary_directory(self)
        self.git = git = GitFixture(self.repo, "master")
        self.body = "CE source implementation\n" * 20
        git.write(".gitignore", "target/\n")
        for category, name in p.TEMPLATES.items():
            git.write(name, f"fixture {category}\n")
        git.write(p.CURATIONS, '{"schemaVersion": 1, "curations": []}')
        git.write("Base.java", fixture("apache") + "base implementation\n")
        git.write("Changed.java", fixture("apache") + "CE implementation\n")
        git.write("Merged.java", fixture("apache") + self.LINES)
        git.write("Source.java", fixture("apache") + self.body)
        self.base = git.commit("CE base")
        git("checkout", "-b", "lts-4.3")
        git.write("Merged.java", fixture("apache") + self.LINES.replace("line 9", "lts fix"))
        git.write("Fixed.java", fixture("apache") + "lts addition\n")
        self.fix = git.commit("CE fix")
        git("checkout", "master")
        git.write("Deleted.java", fixture("apache") + "removed CE source\n")
        self.addition = git.commit("CE addition")
        git.write("Merged.java", fixture("apache") + self.LINES.replace("line 0", "master change"))
        git("rm", "-q", "Deleted.java")
        self.last = git.commit("last Apache commit")
        git.write("Changed.java", fixture("ce-modified") + "CE implementation\nTB change\n")
        git.write("New.java", fixture(p.NON_CE) + "TB feature\n")
        self.relicensing = git.commit("relicensing")
        git("merge", "--no-ff", "lts-4.3", "-m", "integrate CE fix")
        self.scope = ["Base.java", "Changed.java", "Fixed.java", "Merged.java", "New.java", "Source.java"]
        self.output = self.repo / "target/report.json"

    def check(self, *args, scope=None):
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
            for key, body in list(tool.contents.items()):
                tool.write(key, split(body)[1])
            return {key: p.comparison_hash(body) for key, body in tool.contents.items()}

        def restamp(tool, expected):
            for key, header in expected.items():
                (tool.root / tool.paths[key]).write_bytes(fixture(header).encode() + split(tool.contents[key])[1])
            return {key: (tool.root / tool.paths[key]).read_bytes() for key in expected}, {}

        stderr = io.StringIO()
        with (patch.object(p.Path, "cwd", return_value=self.repo),
              patch.object(p, "RELICENSING_COMMIT", self.relicensing),
              patch.object(p, "read_plugin", return_value=fixture_plugin()),
              patch.object(p, "discover_scope", return_value=p.Scope(scope or self.scope, [], [], 0)),
              patch.object(p.Mycila, "current_headers", current_headers),
              patch.object(p.Mycila, "strip_headers", strip_headers),
              patch.object(p.Mycila, "restamp", restamp),
              redirect_stdout(io.StringIO()), redirect_stderr(stderr)):
            code = p.main(["check", "--output", str(self.output), *args])
        report = json.loads(self.output.read_text()) if self.output.exists() else None
        return code, report, stderr.getvalue()

    def rows(self, report):
        return {row["path"]: row for row in report["files"]}

    def test_relicensed_branch_passes(self):
        code, report, _ = self.check()
        self.assertEqual(0, code)
        self.assertEqual({"match": 6}, report["summary"])
        bases = sorted([self.fix, self.last])
        self.assertEqual({"relicensingCommit": self.relicensing, "bases": bases}, report["ce"])
        rows = self.rows(report)
        self.assertEqual({"Base.java": "apache", "Changed.java": "ce-modified", "Fixed.java": "apache",
                          "Merged.java": "apache", "New.java": p.NON_CE, "Source.java": "apache"},
                         {name: row["expectedHeader"] for name, row in rows.items()})
        self.assertEqual({"path": "Merged.java", "merge": bases, "base": self.base}, rows["Merged.java"]["identicalTo"])
        self.assertEqual({"path": "Fixed.java", "ref": self.fix, "blob": self.git("rev-parse", f"{self.fix}:Fixed.java")},
                         rows["Fixed.java"]["identicalTo"])

    def test_fix_restamps_decided_headers_only(self):
        self.git.write("Base.java", FOREIGN_LINE.decode() + "base implementation\n")
        self.git.write("New.java", fixture("apache") + "TB feature\n")
        shortened = self.body.replace("CE source implementation\n", "", 1)
        self.git.write("Source.java", fixture("apache") + shortened)
        code, report, _ = self.check()
        self.assertEqual(1, code)
        self.assertEqual({"match": 3, "mismatch": 3}, report["summary"])
        rows = self.rows(report)
        self.assertEqual((p.FOREIGN, "apache"), (rows["Base.java"]["currentHeader"], rows["Base.java"]["expectedHeader"]))
        self.assertEqual(("apache", p.NON_CE), (rows["New.java"]["currentHeader"], rows["New.java"]["expectedHeader"]))
        self.assertEqual(("apache", "ce-modified"),
                         (rows["Source.java"]["currentHeader"], rows["Source.java"]["expectedHeader"]))

        code, report, _ = self.check("--fix")

        self.assertEqual(1, code)
        self.assertEqual(["New.java", "Source.java"], report["restamped"])
        self.assertEqual({"match": 5, "mismatch": 1}, report["summary"])
        self.assertEqual(fixture(p.NON_CE) + "TB feature\n", (self.repo / "New.java").read_text())
        self.assertEqual(fixture("ce-modified") + shortened, (self.repo / "Source.java").read_text())
        self.assertEqual(FOREIGN_LINE.decode() + "base implementation\n", (self.repo / "Base.java").read_text())
        code, report, _ = self.check("--fix")
        self.assertEqual([], report["restamped"])
        self.assertEqual({"match": 5, "mismatch": 1}, report["summary"])

    def test_ce_evidence_for_new_paths_needs_a_decision(self):
        self.git.write("Copy.java", fixture(p.NON_CE) + self.body)
        self.git.write("Deleted.java", fixture(p.NON_CE) + "removed CE source\n")
        code, report, output = self.check(scope=self.scope + ["Copy.java", "Deleted.java"])
        self.assertEqual(1, code)
        self.assertEqual({"match": 6, "unresolved": 2}, report["summary"])
        rows = self.rows(report)
        self.assertEqual("Source.java", rows["Copy.java"]["copyCandidates"][0]["path"])
        self.assertEqual({"path": "Deleted.java", "ref": self.last + "^1",
                          "blob": self.git("rev-parse", f"{self.addition}:Deleted.java")},
                         rows["Deleted.java"]["historicalCandidate"])
        self.assertIn("Needs a decision (2)\n", output)

    def test_fix_removes_redundant_and_orphaned_curations(self):
        self.git.write("Copy.java", fixture(p.NON_CE) + self.body)

        def curation(name, header):
            return dict(path=name, header=header, reason="Reviewed",
                        contentHash=p.raw_hash((self.repo / name).read_bytes()))

        needed, redundant = curation("Copy.java", p.NON_CE), curation("New.java", p.NON_CE)
        orphaned = dict(path="Gone.java", header=p.NON_CE, reason="Reviewed", contentHash=p.raw_hash(b"gone"))
        self.git.write(p.CURATIONS, json.dumps(dict(schemaVersion=1, curations=[needed, redundant, orphaned]),
                                               indent=2) + "\n")
        scope = self.scope + ["Copy.java"]
        code, report, output = self.check(scope=scope)
        self.assertEqual(1, code)
        self.assertEqual({"match": 7}, report["summary"])
        self.assertEqual(["New.java"], report["redundantCurations"])
        self.assertEqual([dict(path="Gone.java", status="stale-curation", reason="file is no longer in scope")],
                         report["curationIssues"])
        self.assertIn("Redundant curations (1)\n", output)

        code, report, output = self.check("--fix", scope=scope)

        self.assertEqual(0, code)
        self.assertEqual([{"path": "Gone.java", "reason": "file is no longer in scope"},
                          {"path": "New.java", "reason": "the check decides the same header by itself"}],
                         report["removedCurations"])
        self.assertEqual(([], []), (report["redundantCurations"], report["curationIssues"]))
        self.assertEqual(json.dumps(dict(schemaVersion=1, curations=[needed]), indent=2) + "\n",
                         (self.repo / p.CURATIONS).read_text())
        self.assertIn("Removed curations (2)\n", output)
        code, report, _ = self.check("--fix", scope=scope)
        self.assertEqual((0, []), (code, report["removedCurations"]))

    def test_missing_relicensing_commit_fails_before_the_scan(self):
        self.relicensing = "0" * 40
        code, report, output = self.check()
        self.assertEqual((2, None), (code, report))
        self.assertIn("Resolving Git history ... failed\n", output)
        self.assertIn("Pass --ce-ref", output)

    def test_ce_ref_mode_compares_against_the_merged_ce_commit(self):
        code, report, output = self.check("--ce-ref", "lts-4.3", "--ce-remote", str(self.repo))
        self.assertEqual(1, code)
        self.assertEqual({"remote": str(self.repo), "ref": "lts-4.3", "commit": self.fix, "bases": [self.fix]},
                         report["ce"])
        rows = self.rows(report)
        self.assertEqual(("mismatch", "ce-modified"), (rows["Merged.java"]["status"], rows["Merged.java"]["expectedHeader"]))
        self.assertEqual({"match": 5, "mismatch": 1}, report["summary"])
        self.assertIn(f"  CE ref    lts-4.3 → {self.fix[:11]}\n", output)
        self.assertIn("Resolving Git history ... done (CE base lts-4.3)\n", output)

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
            code = p.main(["check", "--ce-ref", "lts-4.3", "--output", str(self.output)])
        self.assertEqual(0, code)
        self.assertEqual([(p.CE_REMOTE, "lts-4.3")], fetched)

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
                    key = ("head", category + extension + repr(eol))
                    sample = SAMPLE_PREFIX[extension] + commented(header, extension) + SAMPLE_BODY[extension]
                    tool.add(key, "sample." + extension, sample.replace("\n", eol).encode())
                    expected[key] = p.comparison_hash((SAMPLE_PREFIX[extension] + SAMPLE_BODY[extension]).encode())
        missing = ("head", "missing")
        tool.add(missing, "missing.java", b"// Ordinary documentation\nclass Missing {}\n")
        expected[missing] = p.comparison_hash(b"// Ordinary documentation\nclass Missing {}\n")
        legacy = ("ce", "legacy")
        tool.add(legacy, "legacy.java", LEGACY_JAVA)
        expected[legacy] = p.comparison_hash(b"class Legacy {}\n")
        foreign = ("head", "foreign")
        tool.add(foreign, "foreign.java", FOREIGN_LINE + b"class Foreign {}\n")
        expected[foreign] = p.comparison_hash(b"class Foreign {}\n")
        one_line = ("head", "one-line")
        tool.add(one_line, "one.sh", b"#!/bin/sh\n# Copyright 2024 The Thingsboard Authors\nset -e\necho x\n")
        expected[one_line] = p.comparison_hash(b"#!/bin/sh\nset -e\necho x\n")
        spdx = stamped((repo / p.TEMPLATES["apache"]).read_text(), "java").encode()
        stacked = ("head", "stacked")
        tool.add(stacked, "stacked.java", spdx + LEGACY_JAVA)
        expected[stacked] = p.comparison_hash(b"class Legacy {}\n")
        reversed_stack = ("head", "reversed")
        tool.add(reversed_stack, "reversed.java", LEGACY_JAVA.replace(b"class Legacy {}\n", spdx + b"class Legacy {}\n"))
        expected[reversed_stack] = p.comparison_hash(b"class Legacy {}\n")
        mentioned = ("head", "mentioned")
        tool.add(mentioned, "mentioned.java", spdx + b"\n" + FOREIGN_LINE + b"class Mentioned {}\n")
        expected[mentioned] = p.comparison_hash(FOREIGN_LINE + b"class Mentioned {}\n")
        pairs = {}
        for extension, legacy_file, body in (("ts", LEGACY_TS, b"export const legacy = 1;\n"),
                                             ("xml", LEGACY_XML, b"<example/>\n")):
            new = stamped((repo / p.TEMPLATES["apache"]).read_text(), extension).encode()
            old = legacy_file[:-len(body)]
            for order, content in (("new-old", new + old + body), ("old-new", old + new + body)):
                key = ("head", order + "." + extension)
                pairs[key] = content
                tool.add(key, key[1], content)
                expected[key] = p.comparison_hash(body)
        header_only = {}
        for extension in SAMPLE_BODY:
            key = ("head", "only." + extension)
            header_only[key] = SAMPLE_PREFIX[extension] + commented((repo / p.TEMPLATES[p.NON_CE]).read_text(), extension)
            tool.add(key, "only." + extension, header_only[key].encode())
            expected[key] = p.comparison_hash(SAMPLE_PREFIX[extension].encode())

        headers = tool.current_headers([key for key in expected if key[0] == "head"])
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
            self.assertEqual(p.NON_CE, headers.pop(key), key)
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
                    key = ("head", "missing-" + category + extension + repr(eol))
                    tool.add(key, "sample." + extension, body.encode())
                    wanted[key] = category
                    expected[key] = (SAMPLE_PREFIX[extension] + stamped(header, extension) + SAMPLE_BODY[extension]).replace("\n", eol).encode()
        legacy = ("head", "legacy")
        tool.add(legacy, "legacy.java", LEGACY_JAVA)
        wanted[legacy], expected[legacy] = "apache", (stamped(templates["apache"], "java") + "class Legacy {}\n").encode()
        confidential = ("head", "confidential")
        tool.add(confidential, "start.sh", b"#!/bin/bash\n#\n# ThingsBoard, Inc. (\"COMPANY\") CONFIDENTIAL\n#\n"
                                           b"# Copyright \xc2\xa9 2016-2026 ThingsBoard, Inc. All Rights Reserved.\n#\n\nset -e\n")
        wanted[confidential], expected[confidential] = p.NON_CE, ("#!/bin/bash\n" + stamped(templates[p.NON_CE], "sh") + "set -e\n").encode()
        header_only = ("head", "only")
        tool.add(header_only, "only.java", b"/**\n * Copyright 2024 The Thingsboard Authors\n */\n")
        wanted[header_only], expected[header_only] = "ce-modified", stamped(templates["ce-modified"], "java").encode()
        shebang = ("head", "shebang")
        tool.add(shebang, "shebang.sh", b"#!/bin/sh\n#\n# Copyright 2024 The Thingsboard Authors\n#\n")
        wanted[shebang], expected[shebang] = p.NON_CE, ("#!/bin/sh\n" + stamped(templates[p.NON_CE], "sh")).encode()
        glued = ("head", "glued")
        tool.add(glued, "glued.sh", b"#\n# Copyright 2024 The Thingsboard Authors\n#\n# helper\nset -e\n")
        wanted[glued] = p.NON_CE
        doubled = ("head", "doubled")
        tool.add(doubled, "doubled.java", LEGACY_JAVA.replace(b"class Legacy {}\n", LEGACY_JAVA))
        wanted[doubled], expected[doubled] = p.NON_CE, (stamped(templates[p.NON_CE], "java") + "class Legacy {}\n").encode()
        damaged = ("head", "damaged")
        tool.add(damaged, "damaged.java", stamped(templates["apache"], "java").replace("Apache-2.0", "Apache-2.0 test").encode()
                 + b"class Damaged {}\n")
        wanted[damaged], expected[damaged] = "apache", (stamped(templates["apache"], "java") + "class Damaged {}\n").encode()
        truncated = ("head", "truncated")
        tool.add(truncated, "truncated.java", b"// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors\nclass Truncated {}\n")
        wanted[truncated], expected[truncated] = "apache", (stamped(templates["apache"], "java") + "class Truncated {}\n").encode()
        vendored = ("head", "vendored")
        tool.add(vendored, "vendored.java", LEGACY_JAVA.replace(b"class Legacy {}\n", FOREIGN_LINE + b"// MIT\nclass Legacy {}\n"))
        wanted[vendored] = p.NON_CE
        spdx = stamped(templates["apache"], "java").encode()
        stacked = ("head", "stacked")
        tool.add(stacked, "stacked.java", spdx + LEGACY_JAVA)
        wanted[stacked], expected[stacked] = "ce-modified", (stamped(templates["ce-modified"], "java") + "class Legacy {}\n").encode()
        reversed_stack = ("head", "reversed")
        tool.add(reversed_stack, "reversed.java", LEGACY_JAVA.replace(b"class Legacy {}\n", spdx + b"\nclass Legacy {}\n"))
        wanted[reversed_stack], expected[reversed_stack] = "apache", (stamped(templates["apache"], "java") + "class Legacy {}\n").encode()
        mentioned = ("head", "mentioned")
        tool.add(mentioned, "mentioned.java", spdx + b"\n" + FOREIGN_LINE + b"class Mentioned {}\n")
        wanted[mentioned] = p.NON_CE
        for extension, legacy_file, body in (("ts", LEGACY_TS, b"export const legacy = 1;\n"),
                                             ("xml", LEGACY_XML, b"<example/>\n")):
            new = stamped(templates["apache"], extension).encode()
            old = legacy_file[:-len(body)]
            for order, content in (("new-old", new + old + body), ("old-new", old + new + body)):
                key = ("head", order + "." + extension)
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
