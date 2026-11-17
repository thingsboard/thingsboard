#!/bin/bash
#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

# Makes the third-party notices of one artifact from its SBOMs.
#
# With --sboms-under <directory> in place of the SBOMs, makes them from every SBOM that the modules under the directory
# left in their target/sbom: the notices of the whole product (see the notices-product execution in the root pom.xml).
#
# The SBOMs are turned into the list of their third-party packages (sbom_to_ort.py). ORT scans the source of each
# package for licenses and copyright statements, and writes the notices with THIRD-PARTY-NOTICES.ftl. The NOTICE files
# of the packages are added as they are (notice_files.py).
#
# Needs Docker and python3. The results of the scans are kept in ~/.ort/scanner (or in $NOTICES_ORT_HOME/scanner):
# a package that is not there yet is scanned, which takes minutes. The scans that others have made are downloaded
# first, from the archive that the machines share (or from the one at $NOTICES_SCAN_CACHE_URL): see scan_cache.py.
# The jars are read from ~/.m2/repository (or from $NOTICES_MAVEN_REPOSITORY); one that is not there is downloaded.
# A package without a source archive and without a repository stops the notices, until a curation gives its source or
# says that it has none.
#
# A stop says what is wrong and what to do. sbom_to_ort.py and notice_files.py print only that. The logs of ORT are long:
# the message that a template stopped with is cut out of them, and a scan that stops on errors gets ORT to list those
# that ort-config/resolutions.yml does not accept, with scan-issues.ftl.
#
# The decisions that people made are in four files:
#   ort-config/curations/curations.json     what a package says wrongly or not at all, and what the scan finds wrongly
#                                           in it: its source, and its license for the versions someone has looked at
#   license-choices.yml                     the license we take where a library offers several
#   ort-config/license-classifications.yml  what the notices owe for each license
#   ort-config/copyright-garbage.yml        copyright statements that are about the text of a license
set -euo pipefail

HERE=$(dirname "$(realpath "$0")")
if [ $# -lt 4 ]; then
  echo "usage: $0 <work directory> <project version> <notices file> <SBOM> ... [options of sbom_to_ort.py]" >&2
  echo "       $0 <work directory> <project version> <notices file> --sboms-under <directory> [options of sbom_to_ort.py]" >&2
  exit 2
fi
WORK=$(realpath -m "$1")
VERSION=$2
NOTICES=$3
shift 3
# --sboms-under: every SBOM that the modules under the directory left in their target/sbom, for the notices of the
# whole product (the notices-product execution in the root pom.xml)
if [ "$1" = --sboms-under ]; then
  [ $# -ge 2 ] || { echo "$0: --sboms-under needs a directory" >&2; exit 2; }
  UNDER=$(realpath "$2")
  mapfile -t SBOMS < <(find "$UNDER" -path '*/node_modules' -prune -o -path '*/target/sbom/*.cdx.json' -print | sort)
  if [ ${#SBOMS[@]} -eq 0 ]; then
    echo "Third-party notices: no SBOMs in the target/sbom directories under $2. Build with them first:" \
      "mvn clean package -DskipTests -Dsbom.skip=false" >&2
    exit 1
  fi
  echo "Third-party notices from ${#SBOMS[@]} SBOMs:"
  printf '  %s\n' "${SBOMS[@]#"$UNDER"/}"
  # An SBOM of another version was left by an older build: the notices would list what that build had
  OTHER=$(python3 -c 'import json, sys
for path in sys.argv[2:]:
    version = json.load(open(path, encoding="utf-8")).get("metadata", {}).get("component", {}).get("version")
    if version != sys.argv[1]:
        print(f"  {path} ({version})")' "$VERSION" "${SBOMS[@]}")
  if [ -n "$OTHER" ]; then
    echo "Third-party notices: these SBOMs are not of the version $VERSION, so an older build left them. Build again" \
      "from clean: mvn clean package -DskipTests -Dsbom.skip=false" >&2
    echo "$OTHER" >&2
    exit 1
  fi
  shift 2
  set -- "${SBOMS[@]}" "$@"
fi
ORT_HOME=${NOTICES_ORT_HOME:-$HOME/.ort}
# ORT runs as the user of this script, so that it can write to the mounted directories whoever that is; its home
# directory in the image belongs to another user, so ORT keeps its data and its home in /ort
ORT=(docker run --rm -u "$(id -u):$(id -g)" -w /work -v "$ORT_HOME:/ort" -v "$HERE:/notices:ro" -v "$WORK:/work"
  -e HOME=/ort/home -e ORT_DATA_DIR=/ort -e ORT_CONFIG_DIR=/notices/ort-config ghcr.io/oss-review-toolkit/ort:98.0.0)

TIMES=
lap() { # name of the step that just ended: how long it took is printed at the end, or on a failure
  TIMES+="${TIMES:+, }$1 $((SECONDS - LAP)) s"
  LAP=$SECONDS
}
durations() {
  echo "Third-party notices: took $SECONDS s${TIMES:+ ($TIMES)}"
}
fail() { # log file, what to say
  echo "$2" >&2
  durations >&2
  echo "Third-party notices: failed. The whole log: $1" >&2
  exit 1
}
stop_message() { # log file: the first message a template stopped with (see stop.ftl), as a stack trace can repeat it
  sed -n '/^NOTICES-MESSAGE-START$/,/^NOTICES-MESSAGE-END$/{/^NOTICES-MESSAGE-/!p;/^NOTICES-MESSAGE-END$/q}' "$1"
}
step() { # log file, command of ours: its log is its message
  local log=$1; shift
  "$@" > "$log" 2>&1 || fail "$log" "$(cat "$log")"
}
ort_step() { # log file, arguments of ORT: the message a template stopped with, else the end of the log
  local log=$1 text; shift
  "${ORT[@]}" "$@" > "$log" 2>&1 && return
  text=$(stop_message "$log")
  fail "$log" "${text:-$(tail -n 30 "$log")}"
}

# Only what an earlier run wrote is removed: the directory may hold other files
rm -rf "$WORK/project" "$WORK/package-configurations" "$WORK/scan-issues"
rm -f "$WORK"/{analyzer-result.json,scan-result.json,jars.json,notice-files.txt,THIRD-PARTY-NOTICES.ftl,stop.ftl,THIRD-PARTY-NOTICES}
rm -f "$WORK"/{convert,analyze,scan,scan-issues,package-configurations,notice-files,report}.log
mkdir -p "$WORK/project" "$ORT_HOME"
LAP=$SECONDS
python3 "$HERE/scan_cache.py" download --home "$ORT_HOME" \
  --url "${NOTICES_SCAN_CACHE_URL:-https://repo.thingsboard.io/artifactory/ort/scan-cache.tar.gz}"
lap "cache download"
step "$WORK/convert.log" python3 "$HERE/sbom_to_ort.py" --project-version "$VERSION" --cache "$ORT_HOME/maven-sources.json" \
  --curations "$HERE/ort-config/curations/curations.json" \
  --own org.thingsboard.ai/common --own org.thingsboard.license/client --own org.thingsboard.license/shared \
  --out "$WORK/project/ortproject.json" --jars "$WORK/jars.json" "$@"
lap convert
ort_step "$WORK/analyze.log" -P ort.analyzer.enabledPackageManagers=OrtProjectFile analyze -f JSON -i /work/project -o /work
lap analyze
if ! "${ORT[@]}" --info scan -f JSON -i /work/analyzer-result.json -o /work --package-types PACKAGE > "$WORK/scan.log" 2>&1; then
  "${ORT[@]}" report -i /work/scan-result.json -o /work/scan-issues -f PlainTextTemplate \
    -O PlainTextTemplate=templatePaths=/notices/scan-issues.ftl > "$WORK/scan-issues.log" 2>&1 || true
  text=$(stop_message "$WORK/scan-issues.log")
  fail "$WORK/scan.log" "${text:-$(tail -n 30 "$WORK/scan.log")}"
fi
lap scan
step "$WORK/package-configurations.log" python3 "$HERE/package_configurations.py" "$WORK/scan-result.json" "$WORK/package-configurations"
lap "package configurations"
step "$WORK/notice-files.log" python3 "$HERE/notice_files.py" --jars "$WORK/jars.json" --scan-result "$WORK/scan-result.json" \
  --archive "$ORT_HOME/scanner/archive" --maven-repository "${NOTICES_MAVEN_REPOSITORY:-$HOME/.m2/repository}" \
  --downloads "$ORT_HOME/maven-jars" --out "$WORK/notice-files.txt"
lap "NOTICE files"
# The template includes notice-files.txt and imports stop.ftl, and can only read what is beside it
cp "$HERE/THIRD-PARTY-NOTICES.ftl" "$HERE/stop.ftl" "$WORK/"
ort_step "$WORK/report.log" --info report -i /work/scan-result.json -o /work -f PlainTextTemplate \
  -O PlainTextTemplate=templatePaths=/work/THIRD-PARTY-NOTICES.ftl \
  --package-configurations-dir /work/package-configurations --repository-configuration-file /notices/license-choices.yml
lap report

mkdir -p "$(dirname "$NOTICES")"
cp "$WORK/THIRD-PARTY-NOTICES" "$NOTICES"
echo "Third-party notices: $(head -1 "$WORK/convert.log"), $(wc -l < "$NOTICES") lines: $NOTICES"
durations
# Where the jars of the NOTICE files came from: downloads explain a slow run
sed -n '/^Jars:/{s/:$//;p}' "$WORK/notice-files.log"
