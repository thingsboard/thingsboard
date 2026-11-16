#!/bin/bash
#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

# Makes the third-party notices of one artifact from its SBOMs.
#
# The SBOMs are turned into the list of their third-party packages (sbom_to_ort.py). ORT scans the source of each
# package for licenses and copyright statements, and writes the notices with THIRD-PARTY-NOTICES.ftl. The NOTICE files
# of the packages are added as they are (notice_files.py).
#
# Needs Docker and python3. The results of the scans are kept in ~/.ort/scanner (or in $NOTICES_ORT_HOME/scanner):
# a package that is not there yet is scanned, which takes minutes.
# The jars are read from ~/.m2/repository (or from $NOTICES_MAVEN_REPOSITORY); one that is not there is downloaded.
# A package without a source archive and without a repository fails the scan, until a curation gives its source or
# says that it has none.
#
# The decisions that people made are in four files:
#   ort-config/curations/curations.json     what a package says wrongly or not at all, and what the scan finds wrongly
#                                           in it: its source, and its license for the versions someone has looked at
#   license-choices.yml                     the license we take where a library offers several
#   ort-config/license-classifications.yml  what the notices owe for each license
#   ort-config/copyright-garbage.yml        copyright statements that are about the text of a license
set -euo pipefail

if [ $# -lt 4 ]; then
  echo "usage: $0 <work directory> <project version> <notices file> <SBOM> ... [options of sbom_to_ort.py]" >&2
  exit 2
fi
WORK=$(realpath -m "$1")
VERSION=$2
NOTICES=$3
shift 3
HERE=$(dirname "$(realpath "$0")")
ORT_HOME=${NOTICES_ORT_HOME:-$HOME/.ort}
# ORT runs as the user of this script, so that it can write to the mounted directories whoever that is; its home
# directory in the image belongs to another user, so ORT keeps its data and its home in /ort
ORT=(docker run --rm -u "$(id -u):$(id -g)" -w /work -v "$ORT_HOME:/ort" -v "$HERE:/notices:ro" -v "$WORK:/work"
  -e HOME=/ort/home -e ORT_DATA_DIR=/ort -e ORT_CONFIG_DIR=/notices/ort-config ghcr.io/oss-review-toolkit/ort:98.0.0)

step() { # log file, command: the log is shown only when the command fails
  local log=$1; shift
  "$@" > "$log" 2>&1 || { cat "$log" >&2; echo "Third-party notices: failed, see $log" >&2; exit 1; }
}

# Only what an earlier run wrote is removed: the directory may hold other files
rm -rf "$WORK/project" "$WORK/package-configurations"
rm -f "$WORK"/{analyzer-result.json,scan-result.json,jars.json,notice-files.txt,THIRD-PARTY-NOTICES.ftl,THIRD-PARTY-NOTICES}
rm -f "$WORK"/{convert,analyze,scan,package-configurations,notice-files,report}.log
mkdir -p "$WORK/project" "$ORT_HOME"
step "$WORK/convert.log" python3 "$HERE/sbom_to_ort.py" --project-version "$VERSION" --cache "$ORT_HOME/maven-sources.json" \
  --curations "$HERE/ort-config/curations/curations.json" \
  --own org.thingsboard.ai/common --own org.thingsboard.license/client --own org.thingsboard.license/shared \
  --out "$WORK/project/ortproject.json" --jars "$WORK/jars.json" "$@"
step "$WORK/analyze.log" "${ORT[@]}" -P ort.analyzer.enabledPackageManagers=OrtProjectFile analyze -f JSON -i /work/project -o /work
step "$WORK/scan.log" "${ORT[@]}" --info scan -f JSON -i /work/analyzer-result.json -o /work --package-types PACKAGE
step "$WORK/package-configurations.log" python3 "$HERE/package_configurations.py" "$WORK/scan-result.json" "$WORK/package-configurations"
step "$WORK/notice-files.log" python3 "$HERE/notice_files.py" --jars "$WORK/jars.json" --scan-result "$WORK/scan-result.json" \
  --archive "$ORT_HOME/scanner/archive" --maven-repository "${NOTICES_MAVEN_REPOSITORY:-$HOME/.m2/repository}" \
  --downloads "$ORT_HOME/maven-jars" --out "$WORK/notice-files.txt"
# The template includes notice-files.txt, and can only include what is beside it
cp "$HERE/THIRD-PARTY-NOTICES.ftl" "$WORK/"
step "$WORK/report.log" "${ORT[@]}" --info report -i /work/scan-result.json -o /work -f PlainTextTemplate \
  -O PlainTextTemplate=templatePaths=/work/THIRD-PARTY-NOTICES.ftl \
  --package-configurations-dir /work/package-configurations --repository-configuration-file /notices/license-choices.yml

mkdir -p "$(dirname "$NOTICES")"
cp "$WORK/THIRD-PARTY-NOTICES" "$NOTICES"
echo "Third-party notices: $(head -1 "$WORK/convert.log"), $(wc -l < "$NOTICES") lines: $NOTICES"
