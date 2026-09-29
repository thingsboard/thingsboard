#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

"""License header provenance check with an optional --fix restamp. See README.md for policy and limits."""

from __future__ import annotations

import argparse
from collections import Counter, defaultdict
from contextlib import contextmanager
from copy import deepcopy
from dataclasses import dataclass
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import subprocess
import sys
import tempfile
import uuid
import xml.etree.ElementTree as ET


TEMPLATES = {
    "apache": "tools/src/main/python/license-headers/templates/license-header.txt",
    "ce-modified": "tools/src/main/python/license-headers/templates/license-header-ce-modified.txt",
    "pe-only": "tools/src/main/python/license-headers/templates/license-header-busl.txt",
}
CE_REMOTE = "https://github.com/thingsboard/thingsboard.git"
CURATIONS = "tools/src/main/python/license-headers/curations.json"
README = "tools/src/main/python/license-headers/README.md"
CURATION_FIELDS = {"path", "contentHash", "header", "reason"}
CONTENT_HASH = re.compile(r"sha256:[0-9a-f]{64}")
UNRECOGNIZED = "unrecognized"
MISSING = "missing"
LEGACY = "legacy"
FOREIGN = "foreign"
LEGACY_HEADER = re.compile(rb"Copyright\s+(?:\xc2\xa9\s+)?(?:\d{4}(?:-\d{4})?\s+)?(?:The Thingsboard Authors|ThingsBoard, Inc\.)",
                           re.IGNORECASE)
SPDX_LINE = re.compile(rb"^(?:/+|#|--)?\s*SPDX-")
SCOPE_POMS = ("pom.xml", "msa/black-box-tests/pom.xml")
MODULE_DESCRIPTOR = "pom.xml"
SCOPE_RESULTS = {"PRESENT", "MISSING", "UNKNOWN"}
SKIPPED_DIRECTORIES = {".git", "node_modules", ".gradle", ".angular"}
BLOCK_COMMENT_SUFFIXES = {".java", ".ts", ".tsx", ".js", ".jsx", ".scss", ".proto", ".gradle"}
LINE_COMMENT_TOKENS = (b"///", b"//", b"#", b"--")
BLOCK_CLOSERS = {b"*/", b"-->"} | set(LINE_COMMENT_TOKENS)
RESTAMP_LISTING = 40
REPEATED_HEADERS = 3
HEADER_WINDOW = 4096
XML_SPACE = "{http://www.w3.org/XML/1998/namespace}space"


@dataclass(frozen=True)
class Style:
    name: str
    suffixes: tuple
    settings: tuple


STRICT_XML = Style("strict_xml", ("html", "xml", "xhtml"), (
    ("firstLine", "<!--EOL"),
    ("beforeEachLine", "    "),
    ("endLine", "-->"),
    ("skipLinePattern", r"^<\?xml.*>$"),
    ("firstLineDetectionPattern", r"^\s*<!--\s*$"),
    ("lastLineDetectionPattern", r"^\s*-->\s*$"),
    ("allowBlankLines", "true"),
    ("multiLine", "true"),
))
STRICT_BLOCK = Style("strict_block", tuple(sorted(suffix[1:] for suffix in BLOCK_COMMENT_SUFFIXES)), (
    ("firstLine", "/*"),
    ("beforeEachLine", " * "),
    ("endLine", " */"),
    ("firstLineDetectionPattern", r"^\s*/\*.*$"),
    ("lastLineDetectionPattern", r"^\s*\*/\s*$"),
    ("allowBlankLines", "true"),
    ("multiLine", "true"),
))


class Failure(Exception):
    pass


class Console:
    STYLES = {"dim": "2", "red": "31", "green": "32"}

    def __init__(self):
        self.pending = False
        self.blank = True

    def paint(self, text, style):
        if style is None or not sys.stderr.isatty() or os.environ.get("NO_COLOR"):
            return text
        return f"\033[{self.STYLES[style]}m{text}\033[0m"

    def begin(self, label):
        print(self.paint(f"{label} ... ", "dim"), end="", file=sys.stderr, flush=True)
        self.pending = True
        self.blank = False

    def end(self, detail="done"):
        print(self.paint(detail, "dim"), file=sys.stderr, flush=True)
        self.pending = False

    def line(self, text="", style=None):
        if self.pending:
            self.end("failed")
        if not text and self.blank:
            return
        print(self.paint(text, style), file=sys.stderr, flush=True)
        self.blank = not text


console = Console()


def run(argv, cwd, *, data=None, ok=(0,)):
    result = subprocess.run(argv, cwd=cwd, input=data, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if result.returncode not in ok:
        output = (result.stdout + result.stderr).decode("utf-8", "replace")
        raise Failure(f"Command failed ({result.returncode}): {argv[0]}\n{output[-6000:]}")
    return result


def git(repo, *args):
    return run(["git", *args], repo).stdout


def maven(directory, args):
    return run(["mvn", "-B", "-ntp", *args], directory).stdout.decode("utf-8", "replace")


def resolve(repo, ref):
    if ref.startswith("-"):
        raise Failure("Git refs cannot start with '-'")
    return git(repo, "rev-parse", "--verify", ref + "^{commit}").decode().strip()


def fetch(repo, remote, ref):
    if ref.startswith("-"):
        raise Failure("Git refs cannot start with '-'")
    try:
        git(repo, "fetch", "--no-tags", "--quiet", remote, "--", ref)
    except Failure as exc:
        raise Failure(f"cannot fetch '{ref}' from {remote}. Pass a branch, tag, or commit of the CE repository, "
                      f"or point --ce-remote at the repository that has it.\n{exc}") from None
    return resolve(repo, "FETCH_HEAD")


def unique_base(repo, left, right):
    bases = git(repo, "merge-base", "--all", left, right).decode().splitlines()
    if len(bases) != 1:
        raise Failure(f"found {len(bases)} merge bases between PE HEAD and the CE ref, expected exactly one. "
                      "Check that the right CE branch was passed.")
    return bases[0]


def first_parent_commits(repo, revision):
    return git(repo, "rev-list", "--first-parent", revision).decode().splitlines()


def tree(repo, revision):
    result = {}
    for record in git(repo, "ls-tree", "-r", "-z", "--full-tree", revision).split(b"\0"):
        if not record:
            continue
        metadata, path = record.split(b"\t", 1)
        mode, kind, oid = metadata.split()
        if kind == b"blob" and mode in {b"100644", b"100755"}:
            result[path.decode("utf-8")] = oid.decode("ascii")
    return result


def blobs(repo, ids):
    ids = sorted(set(ids))
    if not ids:
        return {}
    output = run(["git", "cat-file", "--batch"], repo, data=("\n".join(ids) + "\n").encode()).stdout
    result = {}
    offset = 0
    for oid in ids:
        end = output.index(b"\n", offset)
        fields = output[offset:end].split()
        if len(fields) != 3 or fields[0].decode() != oid or fields[1] != b"blob":
            raise Failure(f"Cannot load Git blob {oid}")
        size = int(fields[2])
        offset = end + 1
        result[oid] = output[offset:offset + size]
        offset += size + 1
    return result


@dataclass(frozen=True)
class Change:
    commit: str
    path: str
    old: str
    new: str
    status: str

    @property
    def deleted(self):
        return self.status == "D"


def history(repo, revision, *options):
    raw = git(repo, "log", "--first-parent", "--diff-merges=first-parent", "--root", "--raw", "--no-abbrev",
              "--no-renames", "-z", "--format=%x1e%H", *options, revision, "--")
    return list(parse_history(raw))


def raw_change(token, tokens):
    fields = token[1:].split()
    if len(fields) != 5:
        raise Failure("Unexpected Git raw record")
    status = fields[4].decode("ascii")
    paths = [next(tokens).decode("utf-8") for _ in range(2 if status[0] in "RC" else 1)]
    return fields, status, paths


def parse_history(raw):
    tokens = iter(raw.split(b"\0"))
    commit = None
    for token in tokens:
        token = token.lstrip(b"\n")
        if token.startswith(b"\x1e"):
            commit = token[1:].decode("ascii").strip()
        elif token.startswith(b":"):
            if commit is None:
                raise Failure("Unexpected Git raw-history record")
            fields, status, paths = raw_change(token, tokens)
            yield Change(commit, paths[0], fields[2].decode(), fields[3].decode(), status)
        elif token:
            raise Failure(f"Unexpected Git history token: {token[:80]!r}")


def integration_boundary(repo, anchor, head):
    chain = first_parent_commits(repo, head)
    low, high = 0, len(chain)
    while low < high:
        middle = (low + high) // 2
        code = run(["git", "merge-base", "--is-ancestor", anchor, chain[middle]], repo, ok=(0, 1)).returncode
        if code == 0:
            low = middle + 1
        else:
            high = middle
    if not low:
        raise Failure("CE anchor is not in PE history")
    return chain[low - 1]


def git_ignored(repo, paths):
    if not paths:
        return set()
    result = run(["git", "check-ignore", "--stdin", "-z"], repo,
                 data="".join(path + "\0" for path in paths).encode("utf-8"), ok=(0, 1))
    return {path.decode("utf-8") for path in result.stdout.split(b"\0") if path}


def raw_hash(content):
    return "sha256:" + hashlib.sha256(content).hexdigest()


def comparison_hash(content):
    return raw_hash(content.replace(b"\r\n", b"\n"))


def line_ending(content):
    return b"\r\n" if b"\r\n" in content else b"\n"


def removed_lines(original, stripped):
    before, after = original.splitlines(True), stripped.splitlines(True)
    start = 0
    while start < min(len(before), len(after)) and before[start] == after[start]:
        start += 1
    end = 0
    while end < min(len(before), len(after)) - start and before[-1 - end] == after[-1 - end]:
        end += 1
    stop = len(before) - end
    while 0 < start < stop and before[start - 1] == before[stop - 1]:
        start -= 1
        stop -= 1
    return before, start, stop


def split_removed(original, stripped):
    before, start, stop = removed_lines(original, stripped)
    removed = before[start:stop]
    token = next((token for token in LINE_COMMENT_TOKENS if removed and removed[0].lstrip().startswith(token)), None)
    if token is not None:
        keep = 0
        while keep < len(removed) and (not removed[keep].strip() or removed[keep].lstrip().startswith(token)):
            keep += 1
        if keep < len(removed):
            removed = removed[:keep]
            stripped = b"".join(before[:start] + before[start + keep:])
    return stripped, b"".join(removed)


def header_state(recognized, removed, stacked=False):
    if stacked:
        return LEGACY
    if recognized != UNRECOGNIZED:
        return recognized
    if not removed.strip():
        return MISSING
    return LEGACY if LEGACY_HEADER.search(removed) else FOREIGN


def extra_copyrights(recognized, removed, templates):
    if recognized not in templates:
        return False
    return len(LEGACY_HEADER.findall(removed)) > len(LEGACY_HEADER.findall(templates[recognized]))


def closes_block(removed):
    lines = [line.strip() for line in removed.splitlines() if line.strip()]
    while lines and SPDX_LINE.match(lines[-1]):
        lines.pop()
    return not lines or lines[-1] in BLOCK_CLOSERS or lines[-1].endswith(b"*/")


def template_lines(content):
    return [line.rstrip() for line in content.splitlines() if line.strip()]


def swap_header(content, old, new):
    lines = content.splitlines(True)
    for start in range(min(len(lines), 40)):
        position = lines[start].find(old[0])
        if position < 0:
            continue
        prefix = lines[start][:position]
        block = lines[start:start + len(old)]
        if len(block) == len(old) and all(line.rstrip(b"\r\n") == prefix + text for line, text in zip(block, old)):
            eol = line_ending(content)
            replaced = [prefix + text + eol for text in new]
            if block[-1] == block[-1].rstrip(b"\r\n"):
                replaced[-1] = replaced[-1].rstrip(b"\r\n")
            return b"".join(lines[:start] + replaced + lines[start + len(old):])
    raise Failure("the recognized header lines are not contiguous, review by hand")


def safe_path(path):
    if not isinstance(path, str) or not path or "\\" in path:
        raise Failure(f"Invalid repository path: {path!r}")
    parsed = PurePosixPath(path)
    if parsed.is_absolute() or ".." in parsed.parts or str(parsed) != path:
        raise Failure(f"Invalid repository path: {path!r}")
    return path


def load_curations(path):
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError) as exc:
        raise Failure(f"Cannot load curations: {exc}") from exc
    if not isinstance(data, dict) or data.get("schemaVersion") != 1 or not isinstance(data.get("curations"), list):
        raise Failure("Expected curation schemaVersion 1 and a curations array")
    curations = {}
    for entry in data["curations"]:
        name = validate_curation(entry)
        if name in curations:
            raise Failure(f"Duplicate curation: {name}")
        curations[name] = entry
    return curations


def validate_curation(entry):
    if not isinstance(entry, dict) or set(entry) != CURATION_FIELDS:
        raise Failure("Each curation needs exactly path, contentHash, header, reason")
    name = safe_path(entry["path"])
    if not isinstance(entry["header"], str) or entry["header"] not in TEMPLATES:
        raise Failure(f"Unknown curated header: {name}")
    if not isinstance(entry["contentHash"], str) or not CONTENT_HASH.fullmatch(entry["contentHash"]):
        raise Failure(f"Invalid whole-file SHA-256: {name}")
    if not isinstance(entry["reason"], str) or not entry["reason"].strip():
        raise Failure(f"Empty curation reason: {name}")
    return name


def xml_plain(path):
    root = ET.parse(path).getroot()
    for element in root.iter():
        element.tag = element.tag.rsplit("}", 1)[-1]
    return root


@dataclass(frozen=True)
class Plugin:
    version: str
    configuration: ET.Element

    @property
    def coordinates(self):
        return f"com.mycila:license-maven-plugin:{self.version}"


def read_plugin(repo):
    plugins = xml_plain(repo / "pom.xml").findall("./build/pluginManagement/plugins/plugin")
    plugin = next((p for p in plugins if p.findtext("artifactId") == "license-maven-plugin"), None)
    if plugin is None:
        raise Failure("Root pom.xml does not manage the Mycila license plugin")
    version = (plugin.findtext("version") or "").strip()
    if not version or "${" in version:
        raise Failure("Root pom.xml must declare a literal Mycila license plugin version")
    configuration = plugin.find("configuration")
    if configuration is None:
        raise Failure("Missing root Mycila configuration")
    return Plugin(version, configuration)


def reject_module_overrides(repo):
    for name in git(repo, "ls-files", "-z", "**/pom.xml").decode().split("\0"):
        if not name:
            continue
        for plugin in xml_plain(repo / name).iter("plugin"):
            if plugin.findtext("artifactId") == "license-maven-plugin" and plugin.find("configuration") is not None:
                raise Failure(f"Module-local Mycila normalization settings need support: {name}")


@dataclass(frozen=True)
class Scope:
    eligible: list
    unknown: list
    modules: list
    ignored: int


def discover_scope(repo, plugin):
    states = defaultdict(set)
    modules = []
    for entry in SCOPE_POMS:
        for report in scope_check(repo, plugin, entry):
            module = report.parent.parent
            modules.append({"path": str(module.relative_to(repo)), "report": str(report.relative_to(repo))})
            for name, result in read_scope_report(repo, module, report):
                states[name].add(result)
    ignored = git_ignored(repo, states)
    eligible = sorted(name for name, results in states.items()
                      if name not in ignored and results & {"PRESENT", "MISSING"})
    unknown = sorted(name for name, results in states.items()
                     if name not in ignored and results == {"UNKNOWN"})
    if not eligible:
        raise Failure("Mycila selected no supported files")
    return Scope(eligible, unknown, modules, len(ignored))


def scope_check(repo, plugin, entry):
    token = "license-provenance-" + uuid.uuid4().hex + ".json"
    log = maven(repo, ["-f", entry, plugin.coordinates + ":check", "-Dlicense.failIfMissing=false",
                       "-Dlicense.report.format=json", "-Dlicense.report.skip=false",
                       f"-Dlicense.report.location=${{project.basedir}}/target/{token}"])
    reports = scope_reports(repo, token)
    executions = len(re.findall(r"--- license:" + re.escape(plugin.version) + r":check .* @ ", log))
    if not executions or executions != len(reports):
        raise Failure(f"Incomplete scope: {executions} executions, {len(reports)} reports")
    return reports


def scope_reports(repo, token):
    reports = []
    for base, directories, files in os.walk(repo):
        directories[:] = [name for name in directories if name not in SKIPPED_DIRECTORIES]
        if token in files:
            report = Path(base) / token
            if report.parent.name != "target":
                raise Failure(f"Unexpected scope report location: {report}")
            reports.append(report)
    return reports


def read_scope_report(repo, module, report):
    data = json.loads(report.read_text())
    if data.get("goal") != "CHECK" or not isinstance(data.get("files"), list):
        raise Failure(f"Invalid Mycila scope report: {report}")
    for item in data["files"]:
        candidate = (module / item["path"]).resolve()
        if not candidate.is_relative_to(repo):
            raise Failure(f"Mycila selected a file outside this repository: {candidate}")
        if item["result"] not in SCOPE_RESULTS:
            raise Failure(f"Unexpected scope result: {item}")
        yield candidate.relative_to(repo).as_posix(), item["result"]


def append_elements(parent, pairs):
    for tag, value in pairs:
        element = ET.SubElement(parent, tag)
        element.text = value
        if value != value.strip():
            element.set(XML_SPACE, "preserve")


class Mycila:
    def __init__(self, repo, directory, plugin):
        self.root = directory
        self.plugin = plugin
        self.report = directory / "report.json"
        self.paths = {}
        self.contents = {}
        self.removed = {}
        self.stacked = set()
        self.sentinel = ("license-provenance-" + uuid.uuid4().hex).encode("ascii")
        self.templates = {header: (repo / name).read_bytes() for header, name in TEMPLATES.items()}
        for header, name in TEMPLATES.items():
            (directory / name).parent.mkdir(parents=True, exist_ok=True)
            (directory / name).write_bytes(self.templates[header])

    def add(self, key, name, content):
        group = "pe" if key[0] == "pe" else "ce"
        path = self.root / "files" / group / str(len(self.paths)) / PurePosixPath(name).name
        path.parent.mkdir(parents=True)
        path.write_bytes(content)
        self.paths[key] = path.relative_to(self.root).as_posix()
        self.contents[key] = content

    def file(self, key):
        return self.root / self.paths[key]

    def write(self, key, content):
        self.file(key).write_bytes(content)
        self.contents[key] = content

    def nonempty(self):
        return {key for key, content in self.contents.items() if content.strip()}

    def current_headers(self, keys):
        keys = list(keys)
        recognized = defaultdict(list)
        for header in TEMPLATES:
            for key, result in self.invoke("check", header, keys).items():
                if result == "PRESENT":
                    recognized[key].append(header)
        return {key: recognized[key][0] if len(recognized[key]) == 1 else UNRECOGNIZED for key in keys}

    @contextmanager
    def guarded(self, keys):
        for key in keys:
            content = self.contents[key]
            eol = line_ending(content)
            self.file(key).write_bytes(content + self.sentinel + eol if content.endswith(eol)
                                       else content + eol + self.sentinel + eol)
        yield
        for key in keys:
            content = self.file(key).read_bytes()
            original = self.contents[key]
            eol = line_ending(original)
            if not content.endswith(self.sentinel + eol):
                raise Failure(f"Mycila rewrote the end of {self.paths[key]}")
            content = content[:-len(self.sentinel + eol)]
            if not original.endswith(eol) and content.endswith(eol):
                content = content[:-len(eol)]
            self.write(key, content)

    def remove_headers(self, keys):
        before = {key: self.contents[key] for key in keys}
        with self.guarded(keys):
            results = self.invoke("remove", "pe-only", keys, styles=(STRICT_XML,))
            unchanged = [key for key, result in results.items()
                         if result == "NOOP" and PurePosixPath(self.paths[key]).suffix in BLOCK_COMMENT_SUFFIXES]
            if unchanged:
                self.invoke("remove", "pe-only", unchanged, styles=(STRICT_XML, STRICT_BLOCK))
        removed = {}
        for key in keys:
            content, removed[key] = split_removed(before[key], self.contents[key])
            if content != self.contents[key]:
                self.write(key, content)
        return removed

    def strip_headers(self):
        passes, _ = self.strip_stacked(list(self.paths))
        self.removed = {key: b"".join(blocks) for key, blocks in passes.items()}
        self.stacked = {key for key, blocks in passes.items() if len(blocks) > 1}
        return {key: comparison_hash(content) for key, content in self.contents.items()}

    def header_states(self, recognized):
        return {key: header_state(header, self.removed[key],
                                  key in self.stacked or extra_copyrights(header, self.removed[key], self.templates))
                for key, header in recognized.items()}

    def strip_stacked(self, keys, *, every=False):
        passes = {key: [block] for key, block in self.remove_headers(keys).items()}
        leftover = {}
        again = [key for key in keys if passes[key][0].strip() and (every or self.leading_legacy(key))]
        for _ in range(REPEATED_HEADERS):
            if not again:
                break
            before = {key: self.contents[key] for key in again}
            more = self.remove_headers(again)
            for key in again:
                if LEGACY_HEADER.search(more[key]) and closes_block(more[key]):
                    passes[key].append(more[key])
                elif more[key]:
                    leftover[key] = more[key]
                    self.write(key, before[key])
            again = [key for key in again if more[key] and key not in leftover and (every or self.leading_legacy(key))]
        return passes, leftover

    def leading_legacy(self, key):
        return LEGACY_HEADER.search(self.contents[key][:HEADER_WINDOW]) is not None

    def restamp(self, expected):
        keys = list(expected)
        passes, leftover = self.strip_stacked(keys, every=True)
        rejected = {}
        for key in keys:
            if not closes_block(passes[key][0]):
                rejected[key] = "the old header runs into the next comment, review by hand"
            elif key in leftover:
                rejected[key] = "more than one header, review by hand"
        accepted = [key for key in keys if key not in rejected]
        with self.guarded(accepted):
            for header in TEMPLATES:
                group = [key for key in accepted if expected[key] == header]
                if group:
                    self.invoke("format", header, group)
        return {key: self.contents[key] for key in accepted}, rejected

    def invoke(self, goal, header, keys, *, styles=()):
        keys = list(keys)
        if not keys:
            return {}
        self.write_pom(header, keys, styles)
        self.report.unlink(missing_ok=True)
        try:
            maven(self.root, [self.plugin.coordinates + ":" + goal])
        except Failure as exc:
            unknown = self.unknown_inputs(keys)
            if unknown:
                raise Failure(f"Mycila cannot normalize these inputs: {unknown[:20]}") from exc
            raise
        results = self.read_results()
        if set(results) != {self.paths[key] for key in keys} or "UNKNOWN" in results.values():
            raise Failure("Mycila comparison scope was incomplete or unknown")
        return {key: results[self.paths[key]] for key in keys}

    def read_results(self):
        if not self.report.exists():
            raise Failure("Mycila did not produce its comparison report")
        return {item["path"]: item["result"] for item in json.loads(self.report.read_text())["files"]}

    def unknown_inputs(self, keys):
        try:
            results = self.read_results()
        except Failure:
            return []
        return [str(key) for key in keys if results.get(self.paths[key]) == "UNKNOWN"]

    def write_pom(self, header, keys, styles):
        project = ET.Element("project", xmlns="http://maven.apache.org/POM/4.0.0")
        append_elements(project, [("modelVersion", "4.0.0"), ("groupId", "license.provenance"),
                                  ("artifactId", "temporary-comparison"), ("version", "1")])
        plugin = ET.SubElement(ET.SubElement(ET.SubElement(project, "build"), "plugins"), "plugin")
        append_elements(plugin, [("groupId", "com.mycila"), ("artifactId", "license-maven-plugin"),
                                 ("version", self.plugin.version)])
        config = ET.SubElement(plugin, "configuration")
        append_elements(config, [("encoding", "UTF-8"), ("strictCheck", "true"), ("quiet", "true"),
                                 ("failIfMissing", "false"), ("failIfUnknown", "true"),
                                 ("reportFormat", "json"), ("reportLocation", str(self.report))])
        config.append(self.mapping(styles))
        license_set = ET.SubElement(ET.SubElement(config, "licenseSets"), "licenseSet")
        append_elements(license_set, [("header", str(self.root / TEMPLATES[header])),
                                      ("useDefaultExcludes", "false")])
        includes = ET.SubElement(license_set, "includes")
        append_elements(includes, [("include", pattern) for pattern in self.include_patterns(keys)])
        if styles:
            parent = ET.SubElement(license_set, "inlineHeaderStyles")
            for style in styles:
                append_elements(ET.SubElement(parent, "inlineHeaderStyle"), (("name", style.name),) + style.settings)
        ET.ElementTree(project).write(self.root / "pom.xml", encoding="utf-8", xml_declaration=True)

    def mapping(self, styles):
        mapping = self.plugin.configuration.find("mapping")
        mapping = deepcopy(mapping) if mapping is not None else ET.Element("mapping")
        for style in styles:
            for extension in style.suffixes:
                existing = mapping.find(extension)
                if existing is not None:
                    mapping.remove(existing)
                ET.SubElement(mapping, extension).text = style.name
        return mapping

    def include_patterns(self, keys):
        selected = set(keys)
        if selected == set(self.paths):
            return ["files/**"]
        if selected == {key for key in self.paths if key[0] == "pe"}:
            return ["files/pe/**"]
        return [str(PurePosixPath(self.paths[key]).parent) + "/*" for key in keys]


def normalized_copies(mycila, source_refs, names, nonempty):
    sources = set(source_refs) & nonempty
    destinations = {("pe", name) for name in names} & nonempty
    if not sources or not destinations:
        return {}
    git(mycila.root, "init", "--quiet")
    git(mycila.root, "config", "core.autocrlf", "false")
    git(mycila.root, "config", "core.attributesFile", "/dev/null")
    before = stage(mycila, sources)
    after = stage(mycila, destinations)
    raw = git(mycila.root, "diff-tree", "--no-commit-id", "-r", "--raw", "--no-abbrev", "-z",
              "-C50%", "--find-copies-harder", "-l0", before, after)
    keys = {path: key for key, path in mycila.paths.items()}
    copies = defaultdict(list)
    for source, destination, similarity in parse_copies(raw):
        source_key, destination_key = keys[source], keys[destination]
        if source_key not in sources or destination_key not in destinations:
            raise Failure("Normalized copy escaped the selected CE/PE inputs")
        copies[destination_key[1]].append({"path": source_key[0], "blob": source_key[1],
                                           "ref": source_refs[source_key], "similarity": similarity})
    return dict(copies)


def stage(mycila, keys):
    for key in keys:
        mycila.write(key, mycila.contents[key].replace(b"\r\n", b"\n"))
    pathspec = "".join(mycila.paths[key] + "\0" for key in sorted(keys)).encode("utf-8")
    run(["git", "--literal-pathspecs", "add", "-f", "--pathspec-from-file=-", "--pathspec-file-nul"],
        mycila.root, data=pathspec)
    return git(mycila.root, "write-tree").decode().strip()


def parse_copies(raw):
    tokens = iter(raw.split(b"\0"))
    for token in tokens:
        token = token.lstrip(b"\n")
        if token:
            _, status, paths = raw_change(token, tokens)
            if status[0] == "C":
                yield paths[0], paths[1], int(status[1:])


@dataclass(frozen=True)
class Evidence:
    path: str
    content_hash: str
    current: str
    curation: dict | None
    inherited: bool
    recreated: bool
    ce_hash: str | None
    pe_hash: str
    candidates: list
    historical: list
    in_comparison: bool
    pe_addition: bool = False
    copies: tuple = ()
    directory_in_ce: bool = True
    ce_ref: str | None = None
    ce_blob: str | None = None

    @property
    def strong(self):
        return [c for c in self.historical if c["path"] == self.path or c.get("contentMatches")]

    def ce_traces(self):
        return bool(self.candidates or self.copies or self.in_comparison or self.strong)

    def pe_module_descriptor(self):
        return (PurePosixPath(self.path).name == MODULE_DESCRIPTOR and not self.directory_in_ce
                and not (self.candidates or self.in_comparison or self.strong))


def decide(evidence):
    row = {"path": evidence.path, "contentHash": evidence.content_hash, "currentHeader": evidence.current}
    if evidence.curation is not None:
        return row | curated_verdict(evidence)
    if evidence.recreated:
        return row | {"status": "unresolved", "expectedHeader": None,
                      "reason": "deleted and recreated at this path, origin unclear"}
    if evidence.inherited and evidence.ce_hash is not None:
        comparison = {"comparison": {"path": evidence.path, "ref": evidence.ce_ref, "blob": evidence.ce_blob}}
        if evidence.pe_hash == evidence.ce_hash:
            return row | verdict(evidence.current, "apache", "same path in CE, content identical") | comparison
        return row | verdict(evidence.current, "ce-modified", "same path in CE, content differs") | comparison
    if evidence.pe_addition and not evidence.ce_traces():
        return row | verdict(evidence.current, "pe-only", "added in PE, no CE evidence")
    if evidence.pe_addition and evidence.pe_module_descriptor():
        return row | verdict(evidence.current, "pe-only", "build descriptor of a module that does not exist in CE")
    return row | open_question(evidence)


def verdict(current, expected, reason):
    return {"status": "match" if current == expected else "mismatch", "expectedHeader": expected, "reason": reason}


def curated_verdict(evidence):
    curation = evidence.curation
    if curation["contentHash"] != evidence.content_hash:
        return {"status": "stale-curation", "expectedHeader": None,
                "reason": "file changed since the curation, re-review and update contentHash"}
    return verdict(evidence.current, curation["header"], "curation: " + curation["reason"])


def open_question(evidence):
    strong = evidence.strong
    ranked = [
        (evidence.candidates, "identical content exists in CE under another path", "apache"),
        (evidence.copies, "similar content exists in CE under another path", None),
        (strong, "CE history has this path or content", None),
        (evidence.in_comparison, "present in CE, but not in the shared ancestry", None),
        (True, "no CE evidence, but not proven to be added in PE", "pe-only"),
    ]
    _, reason, suggested = next(item for item in ranked if item[0])
    return {
        "status": "unresolved",
        "expectedHeader": None,
        "suggestedHeader": suggested,
        "reason": reason,
        "candidates": evidence.candidates[:10],
        "historicalCandidates": strong[:10],
        "copyCandidates": list(evidence.copies),
        "candidateCount": len(evidence.candidates),
        "historicalCandidateCount": len(strong),
    }


@dataclass(frozen=True)
class Lineage:
    head: str
    baseline: str
    anchor: str
    boundary: str
    ce_commits: frozenset
    ce_changes: list
    comparison: dict
    shared: dict
    pe_tree: dict
    additions: dict
    recreated: frozenset

    @classmethod
    def load(cls, repo, head, baseline):
        if git(repo, "rev-parse", "--is-shallow-repository").decode().strip() == "true":
            raise Failure("this clone is shallow and the check needs full history. Run git fetch --unshallow.")
        anchor = unique_base(repo, head, baseline)
        ce_chain = first_parent_commits(repo, baseline)
        ce_commits = frozenset(ce_chain)
        if anchor not in ce_commits:
            raise Failure("the shared ancestor is not on the CE ref's first-parent chain. "
                          "Pass the CE mainline branch that the PE branch integrates.")
        boundary = integration_boundary(repo, anchor, head)
        shared = tree(repo, anchor)
        additions = {}
        for change in history(repo, head, "--diff-filter=A"):
            additions.setdefault(change.path, change.commit)
        ce_changes = history(repo, baseline)
        divergent = frozenset(ce_chain[:ce_chain.index(anchor)])
        divergence = (history(repo, f"{boundary}..{head}")
                      + [change for change in ce_changes if change.commit in divergent])
        return cls(
            head=head,
            baseline=baseline,
            anchor=anchor,
            boundary=boundary,
            ce_commits=ce_commits,
            ce_changes=ce_changes,
            comparison=tree(repo, baseline),
            shared=shared,
            pe_tree=tree(repo, head),
            additions=additions,
            recreated=frozenset(change.path for change in divergence
                                if change.status in {"A", "D"} and change.path in shared),
        )

    def unmatched(self, names):
        return {name for name in names
                if name not in self.shared or name not in self.comparison or name in self.recreated}

    def pe_addition(self, name):
        if name not in self.pe_tree:
            return True
        return name in self.additions and self.additions[name] not in self.ce_commits

    def added_by(self, name):
        if name not in self.pe_tree:
            return self.additions.get(name, "working-tree addition")
        return self.additions.get(name)


def historical_versions(ce_changes, unmatched):
    basenames = {PurePosixPath(name).name for name in unmatched}
    versions = {}
    for change in ce_changes:
        if change.path not in unmatched and PurePosixPath(change.path).name not in basenames:
            continue
        oid = change.old if change.deleted else change.new
        if set(oid) == {"0"}:
            continue
        versions.setdefault((change.path, oid), change.commit + "^1" if change.deleted else change.commit)
    return versions


def comparable_ce_files(comparison, names):
    suffixes = {PurePosixPath(name).suffix for name in names} - {""}
    extensionless = {PurePosixPath(name).name for name in names if not PurePosixPath(name).suffix}
    return {path: oid for path, oid in comparison.items()
            if PurePosixPath(path).suffix in suffixes or PurePosixPath(path).name in extensionless}


@dataclass(frozen=True)
class Comparison:
    lineage: Lineage
    contents: dict
    unmatched: set
    headers: dict
    normalized: dict
    nonempty: set
    copies: dict
    exact_index: dict
    historic_index: dict
    ce_directories: frozenset

    def classify(self, name, curation):
        key = ("pe", name)
        lineage = self.lineage
        unmatched = name in self.unmatched
        historic = self.historical_candidates(name) if unmatched else []
        evidence = Evidence(
            path=name,
            content_hash=raw_hash(self.contents[name]),
            current=self.headers[key],
            curation=curation,
            inherited=name in lineage.shared,
            recreated=name in lineage.recreated,
            ce_hash=self.normalized.get((name, lineage.comparison.get(name))),
            pe_hash=self.normalized[key],
            candidates=self.exact_index.get(self.normalized[key], []) if unmatched else [],
            historical=historic,
            in_comparison=name in lineage.comparison,
            pe_addition=lineage.pe_addition(name),
            copies=self.copies.get(name, []),
            directory_in_ce=str(PurePosixPath(name).parent) in self.ce_directories,
            ce_ref=lineage.baseline,
            ce_blob=lineage.comparison.get(name),
        )
        row = decide(evidence)
        if unmatched:
            row["peAddition"] = lineage.added_by(name)
            row["basenameHintCount"] = len(historic) - len(evidence.strong)
        return row

    def historical_candidates(self, name):
        key = ("pe", name)
        candidates = []
        for version in self.historic_index.get(PurePosixPath(name).name, []):
            matches = (key in self.nonempty
                       and self.normalized[key] == self.normalized[(version["path"], version["blob"])])
            candidates.append(version | {"contentMatches": matches})
        candidates.sort(key=lambda candidate: (not candidate["contentMatches"], candidate["path"] != name))
        return candidates


def compare(repo, plugin, lineage, contents):
    names = list(contents)
    unmatched = lineage.unmatched(names)
    versions = historical_versions(lineage.ce_changes, unmatched)
    counterparts = comparable_ce_files(lineage.comparison, names)
    source_refs = versions | {key: lineage.baseline for key in counterparts.items()}
    bytes_by_id = blobs(repo, [oid for _, oid in source_refs])
    with tempfile.TemporaryDirectory(prefix="license-provenance-") as temporary:
        mycila = Mycila(repo, Path(temporary), plugin)
        for name, content in contents.items():
            mycila.add(("pe", name), name, content)
        for path, oid in sorted(source_refs):
            mycila.add((path, oid), path, bytes_by_id[oid])
        console.begin("Recognizing headers")
        recognized = mycila.current_headers([("pe", name) for name in names])
        console.end()
        console.begin("Comparing against CE")
        normalized = mycila.strip_headers()
        headers = mycila.header_states(recognized)
        nonempty = mycila.nonempty()
        copies = normalized_copies(mycila, source_refs, unmatched, nonempty)
    exact_index = defaultdict(list)
    for path, oid in counterparts.items():
        if (path, oid) in nonempty:
            exact_index[normalized[(path, oid)]].append({"path": path, "ref": lineage.baseline, "blob": oid})
    historic_index = defaultdict(list)
    for (path, oid), ref in versions.items():
        historic_index[PurePosixPath(path).name].append({"path": path, "ref": ref, "blob": oid})
    console.end(f"done ({len(normalized)} file versions)")
    return Comparison(
        lineage=lineage,
        contents=contents,
        unmatched=unmatched,
        headers=headers,
        normalized=normalized,
        nonempty=nonempty,
        copies=copies,
        exact_index=dict(exact_index),
        historic_index=dict(historic_index),
        ce_directories=frozenset(str(parent) for path in lineage.comparison for parent in PurePosixPath(path).parents),
    )


def current_branch(repo):
    branch = git(repo, "rev-parse", "--abbrev-ref", "HEAD").decode().strip()
    return "" if branch == "HEAD" else branch


def print_banner(repo, head, ce_remote, ce_ref, baseline):
    console.line("License header provenance check")
    console.line(f"  PE head   {head[:11]}  {current_branch(repo)}".rstrip())
    console.line(f"  CE remote {ce_remote}")
    console.line(f"  CE ref    {ce_ref}" + (f" → {baseline[:11]}" if baseline != ce_ref else ""))
    console.line()


def header_name(header):
    return {MISSING: "no header", LEGACY: "outdated or damaged ThingsBoard header",
            FOREIGN: "unrecognized header"}.get(header, header)


def ref_name(ref, ref_names):
    return ref_names.get(ref) or re.sub(r"^[0-9a-f]{40}", lambda match: match.group()[:11], ref)


def fixable(row):
    return row["status"] == "mismatch" and row["currentHeader"] != FOREIGN


def describe_mismatch(row):
    details = [f"has {header_name(row['currentHeader'])}, expected {row['expectedHeader']}", row["reason"]]
    if not fixable(row):
        details.append("not a ThingsBoard header, --fix leaves it for a manual review")
    if "skipped" in row:
        details.append("--fix skipped it: " + row["skipped"])
    return details


def describe_unresolved(row, ref_names):
    hint = f" (hint: {row['suggestedHeader']})" if row.get("suggestedHeader") else ""
    details = [f"has {header_name(row['currentHeader'])}, no expected header yet{hint}"]
    evidence = ([("exact CE match", candidate) for candidate in row.get("candidates", [])]
                + [(f"similar CE file ({candidate['similarity']}%)", candidate)
                   for candidate in row.get("copyCandidates", [])]
                + [("CE history", candidate) for candidate in row.get("historicalCandidates", [])])
    if not evidence:
        return details + [row["reason"]]
    label, candidate = evidence[0]
    details.append(f"{label}: {candidate['path']} at {ref_name(candidate['ref'], ref_names)}")
    others = (row.get("candidateCount", 0) + len(row.get("copyCandidates", []))
              + row.get("historicalCandidateCount", 0) - 1)
    if others > 0:
        details.append(f"another {others} candidate{'s' if others != 1 else ''} in the report")
    return details


def describe_stale(row):
    return [row["reason"]]


def print_group(title, rows, describe, hint):
    console.line(f"{title} ({len(rows)})", "red")
    for row in rows:
        console.line(f"  {row['path']}")
        for detail in describe(row):
            console.line(f"    {detail}")
    console.line(f"  → {hint}")
    console.line()


def print_restamped(rows):
    restamped = [row for row in rows if "previousHeader" in row]
    if not restamped:
        return
    console.line(f"Restamped ({len(restamped)})", "green")
    if len(restamped) > RESTAMP_LISTING:
        changes = Counter((row["previousHeader"], row["currentHeader"]) for row in restamped)
        for (previous, current), count in sorted(changes.items()):
            console.line(f"  {count} × {header_name(previous)} replaced with {current}")
    else:
        for row in restamped:
            console.line(f"  {row['path']}")
            console.line(f"    {header_name(row['previousHeader'])} replaced with {row['currentHeader']}")
    console.line()


def print_findings(rows, curation_issues, ref_names):
    mismatches = [row for row in rows if row["status"] == "mismatch"]
    unresolved = [row for row in rows if row["status"] == "unresolved"]
    stale = [row for row in rows if row["status"] == "stale-curation"] + curation_issues
    if mismatches:
        hint = ("Rerun with --fix to restamp automatically" if any(fixable(row) for row in mismatches)
                else "Review the existing header by hand, --fix does not replace unrecognized headers")
        print_group("Wrong header", mismatches, describe_mismatch, hint)
    if unresolved:
        print_group("Needs a decision", unresolved, lambda row: describe_unresolved(row, ref_names),
                    f"Decide the origin and add a curation, see {README}")
    if stale:
        print_group("Stale curations", stale, describe_stale, f"Remove or update the entry in {CURATIONS}")


def print_summary(rows, curation_issues, report):
    findings = sum(row["status"] != "match" for row in rows) + len(curation_issues)
    restamped = sum("previousHeader" in row for row in rows)
    note = f" ({restamped} restamped)" if restamped else ""
    if findings:
        console.line(f"{findings} finding{'s' if findings != 1 else ''} in {len(rows)} files{note}. Report: {report}",
                     "red")
        return
    console.line(f"All {len(rows)} files carry the expected header{note}.", "green")
    counts = Counter(row["expectedHeader"] for row in rows)
    console.line("  " + " · ".join(f"{header} {counts[header]}" for header in TEMPLATES if counts[header]))
    console.line(f"Report: {report}")


def repository_root():
    return Path(git(Path.cwd(), "rev-parse", "--show-toplevel").decode().strip()).resolve()


def report_destination(repo, output):
    destination = Path(output).resolve()
    if not destination.is_relative_to(repo / "target"):
        raise Failure(f"the report must be written below {repo / 'target'}, not {destination}.")
    return destination


def ensure_inputs_unchanged(repo, lineage, curation_path, curation_bytes, contents):
    changed = (resolve(repo, "HEAD") != lineage.head
               or curation_path.read_bytes() != curation_bytes
               or any((repo / name).read_bytes() != content for name, content in contents.items()))
    if changed:
        raise Failure("checked files or curations.json changed during the scan. Rerun after finishing your edits.")


def restamp(repo, plugin, rows, contents):
    targets = [row for row in rows if fixable(row)]
    if not targets:
        return
    console.begin("Restamping headers")
    current = {row["path"]: row["currentHeader"] for row in targets}
    wanted = {row["path"]: row["expectedHeader"] for row in targets}
    stamped, skipped, expected = {}, {}, {}
    with tempfile.TemporaryDirectory(prefix="license-provenance-") as temporary:
        mycila = Mycila(repo, Path(temporary), plugin)
        templates = {header: template_lines(content) for header, content in mycila.templates.items()}
        for name, header in current.items():
            if header not in TEMPLATES:
                expected[("pe", name)] = wanted[name]
                mycila.add(("pe", name), name, contents[name])
                continue
            try:
                stamped[name] = swap_header(contents[name], templates[header], templates[wanted[name]])
            except Failure as exc:
                skipped[name] = str(exc)
        if expected:
            results, rejected = mycila.restamp(expected)
            stamped |= {key[1]: content for key, content in results.items()}
            skipped |= {key[1]: reason for key, reason in rejected.items()}
        for name, content in stamped.items():
            if ("pe", name) not in mycila.paths:
                mycila.add(("pe", name), name, content)
        headers = mycila.current_headers([("pe", name) for name in stamped])
    wrong = sorted(key[1] for key, header in headers.items() if header != wanted[key[1]])
    if wrong:
        raise Failure(f"Restamping did not produce the expected header: {wrong[:20]}")
    for row in targets:
        if row["path"] in skipped:
            row["skipped"] = skipped[row["path"]]
            continue
        (repo / row["path"]).write_bytes(stamped[row["path"]])
        row["previousHeader"] = row["currentHeader"]
        row["currentHeader"] = row["expectedHeader"]
        row["status"] = "match"
    console.end(f"{len(stamped)} files" + (f", {len(skipped)} skipped" if skipped else ""))


def check(args):
    repo = repository_root()
    head = resolve(repo, "HEAD")
    console.begin(f"Fetching {args.ce_ref} from {args.ce_remote}")
    baseline = fetch(repo, args.ce_remote, args.ce_ref)
    console.end()
    print_banner(repo, head, args.ce_remote, args.ce_ref, baseline)
    destination = report_destination(repo, args.output)
    plugin = read_plugin(repo)
    reject_module_overrides(repo)
    curation_path = repo / CURATIONS
    curation_bytes = curation_path.read_bytes()
    curations = load_curations(curation_path)
    console.begin("Resolving Git history")
    lineage = Lineage.load(repo, head, baseline)
    console.end()
    console.begin("Collecting Mycila scope")
    scope = discover_scope(repo, plugin)
    console.end(f"{len(scope.eligible)} files ({scope.ignored} Git-ignored skipped)")
    contents = {name: (repo / name).read_bytes() for name in scope.eligible}
    comparison = compare(repo, plugin, lineage, contents)
    console.line()
    rows = [comparison.classify(name, curations.get(name)) for name in scope.eligible]
    orphaned = [{"path": name, "status": "stale-curation", "reason": "file is no longer in scope"}
                for name in sorted(set(curations) - set(scope.eligible))]
    ensure_inputs_unchanged(repo, lineage, curation_path, curation_bytes, contents)
    if args.fix:
        restamp(repo, plugin, rows, contents)
    summary = Counter(row["status"] for row in rows)
    result = {
        "schemaVersion": 1,
        "peHead": lineage.head,
        "ceComparison": {"remote": args.ce_remote, "input": args.ce_ref, "commit": lineage.baseline},
        "ceHistory": {"tip": lineage.baseline, "firstParent": True},
        "sharedAncestor": lineage.anchor,
        "peIntegrationBoundary": lineage.boundary,
        "scope": {"supported": len(scope.eligible), "unknown": scope.unknown, "modules": scope.modules},
        "summary": dict(summary),
        "restamped": sorted(row["path"] for row in rows if "previousHeader" in row),
        "curationIssues": orphaned,
        "files": rows,
    }
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(result, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    report = destination.relative_to(repo) if destination.is_relative_to(repo) else destination
    print_restamped(rows)
    print_findings(rows, orphaned, {baseline: args.ce_ref})
    print_summary(rows, orphaned, report)
    return 1 if orphaned or summary.keys() - {"match"} else 0


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=["check"])
    parser.add_argument("--ce-ref", required=True,
                        help="CE tree and first-parent history to check against; a branch, tag, or commit "
                             "of the CE repository, fetched from --ce-remote on every run")
    parser.add_argument("--ce-remote", default=CE_REMOTE,
                        help=f"Git URL or path of the CE repository to fetch --ce-ref from (default: {CE_REMOTE})")
    parser.add_argument("--output", default="target/license-provenance/report.json")
    parser.add_argument("--fix", action="store_true",
                        help="restamp files whose expected header the check determined; unrecognized headers are kept")
    args = parser.parse_args(argv)
    try:
        return check(args)
    except (Failure, OSError, ValueError, ET.ParseError) as exc:
        console.line()
        console.line(f"Error: {exc}", "red")
        return 2


if __name__ == "__main__":
    sys.exit(main())
