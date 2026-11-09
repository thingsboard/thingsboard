#!/bin/bash
#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

set -euo pipefail

usage() {
  cat <<EOF
Generates the CycloneDX SBOM of every architecture of the pushed ThingsBoard Docker images
and attaches it to the image as an OCI referrer artifact.

Run it from the repository root after the release build has pushed the images
(-Dpush-docker-amd-arm-images), in the same workspace: the image SBOMs are checked
against the packages that build left in target/.

Usage: ./sbom-docker-images.sh [options]

  --tag <tag>          image tag to process; repeat for several tags (default: the project version).
                       Every tag pushed by the release build must be listed, e.g. --tag 4.4.1 --tag latest.
  --projects <list>    comma-separated msa modules (default: every image module that has an image SBOM).
  --registry <host>    registry the images were pushed to (default: Docker Hub).
  --insecure-registry  talk to the registry over plain HTTP (for testing with a local registry).
  --out <dir>          directory to collect the image SBOMs in (default: target/image-sboms).

Requires docker with buildx, syft and oras on the PATH.
When the registry rate-limits pulls, the script waits and retries.
Extra Maven arguments can be passed in MAVEN_ARGS, e.g. MAVEN_ARGS=-Ddocker.repo=example.
Running it again for the same images attaches the SBOMs again.
EOF
}

SBOM_TYPE="application/vnd.cyclonedx+json"
TAGS=()
PROJECTS=""
REGISTRY=""
INSECURE=false
OUT="target/image-sboms"

while [ $# -gt 0 ]; do
  case "$1" in
    --tag) TAGS+=("$2"); shift 2 ;;
    --projects) PROJECTS="$2"; shift 2 ;;
    --registry) REGISTRY="${2%/}"; shift 2 ;;
    --insecure-registry) INSECURE=true; shift ;;
    --out) OUT="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown option $1" >&2; usage >&2; exit 2 ;;
  esac
done

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

RATE_LIMIT_WAIT=600
RATE_LIMIT_RETRIES=6

registry() {
  local attempt errors
  errors=$(mktemp)
  for attempt in $(seq 0 "$RATE_LIMIT_RETRIES"); do
    if "$@" 2> "$errors"; then
      rm -f "$errors"
      return 0
    fi
    if ! grep -qi 'toomanyrequests\|429 Too Many Requests' "$errors" || [ "$attempt" -eq "$RATE_LIMIT_RETRIES" ]; then
      cat "$errors" >&2
      rm -f "$errors"
      return 1
    fi
    echo "  registry rate limit reached; retrying in $((RATE_LIMIT_WAIT / 60)) minutes" >&2
    sleep "$RATE_LIMIT_WAIT"
  done
}

[ -f pom.xml ] && [ -d msa ] || fail "run the script from the repository root"
for tool in docker syft oras mvn; do
  command -v "$tool" > /dev/null || fail "$tool is not installed"
done
docker buildx version > /dev/null 2>&1 || fail "docker buildx is not installed"
echo "Using $(syft version 2>/dev/null | awk '/^Version:/ {print "syft " $2}'), $(oras version 2>/dev/null | awk '/^Version:/ {print "oras " $2}')"

ORAS_ARGS=()
if [ "$INSECURE" = true ]; then
  ORAS_ARGS+=(--plain-http)
  export SYFT_REGISTRY_INSECURE_USE_HTTP=true
fi
export SYFT_FILE_METADATA_SELECTION=none

if [ -z "$PROJECTS" ]; then
  PROJECTS=$(grep -l '<docker.name>' $(find msa -name pom.xml -not -path '*/target/*' -not -path '*/node_modules/*') \
    | xargs grep -l '<artifactId>sbom-maven-plugin</artifactId>' | xargs -n1 dirname | sort | paste -sd, -)
fi
[ -n "$PROJECTS" ] || fail "no image modules found"

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT

echo "Reading image settings of $PROJECTS ..."
mvn -q -pl "$PROJECTS" org.codehaus.mojo:exec-maven-plugin:3.5.1:exec -Dexec.executable=echo \
  '-Dexec.args=SBOM-IMAGE|${project.basedir}|${sbom.image.repository}|${project.version}|${sbom.image.scanExclude}|' \
  | grep '^SBOM-IMAGE|' > "$WORK/images" || fail "cannot read the image settings from Maven"
mkdir -p "$OUT"

FAILED=()
while IFS='|' read -r _ BASEDIR REPOSITORY VERSION EXCLUDE _; do
  MODULE=${BASEDIR#"$PWD"/}
  IMAGE="${REGISTRY:+$REGISTRY/}$REPOSITORY"
  ORAS_IMAGE="${REGISTRY:-docker.io}/$REPOSITORY"
  IMAGE_TAGS=("${TAGS[@]:-$VERSION}")
  echo "=== $IMAGE (${IMAGE_TAGS[*]})"
  set +e
  (
    set -euo pipefail
    declare -A ARCH_DIGESTS=()
    for TAG in "${IMAGE_TAGS[@]}"; do
      INSPECT=$(registry docker buildx imagetools inspect "$IMAGE:$TAG" --format \
        '{{.Manifest.Digest}}{{"\n"}}{{range .Manifest.Manifests}}{{if eq .Platform.OS "linux"}}{{.Platform.Architecture}} {{.Digest}}{{"\n"}}{{end}}{{end}}') \
        || fail "cannot inspect $IMAGE:$TAG"
      INDEX=$(head -n1 <<< "$INSPECT")
      [[ "$INDEX" == sha256:* ]] || fail "$IMAGE:$TAG has no digest"
      while read -r ARCH DIGEST; do
        [ -n "$ARCH" ] && ARCH_DIGESTS["$DIGEST"]=$ARCH
      done < <(tail -n +2 <<< "$INSPECT")
      echo "  $TAG -> $INDEX"
    done
    [ ${#ARCH_DIGESTS[@]} -gt 0 ] || fail "$IMAGE has no linux images"

    DIGESTS=$(for DIGEST in "${!ARCH_DIGESTS[@]}"; do echo "${ARCH_DIGESTS[$DIGEST]} $DIGEST"; done | sort | cut -d' ' -f2)
    for DIGEST in $DIGESTS; do
      ARCH=${ARCH_DIGESTS[$DIGEST]}
      SHORT=${DIGEST#sha256:}
      SHORT=${SHORT:0:12}
      OS_SBOM="$BASEDIR/target/sbom/os-$ARCH-$SHORT.cdx.json"
      IMAGE_SBOM="$BASEDIR/target/sbom/image-$ARCH-$SHORT.cdx.json"
      SBOM_NAME="${REPOSITORY//\//_}_${VERSION}_${ARCH}_$SHORT.cdx.json"
      mkdir -p "$BASEDIR/target/sbom"
      echo "  $ARCH $DIGEST: scanning"
      registry syft scan "registry:$IMAGE@$DIGEST" ${EXCLUDE:+--exclude "$EXCLUDE"} -o "cyclonedx-json@1.6=$OS_SBOM"
      echo "  $ARCH $DIGEST: generating the image SBOM"
      mvn -q -pl "$MODULE" validate org.thingsboard:sbom-maven-plugin:image@image-sbom \
        -Dsbom.image.osSbom="$OS_SBOM" -Dsbom.image.digest="$DIGEST" -Dsbom.image.architecture="$ARCH" \
        -Dsbom.image.tag="$VERSION" -Dsbom.image.outputFile="$IMAGE_SBOM"
      cp "$IMAGE_SBOM" "$OUT/$SBOM_NAME"
      echo "  $ARCH $DIGEST: attaching the SBOM"
      (cd "$OUT" && registry oras attach "${ORAS_ARGS[@]}" --artifact-type "$SBOM_TYPE" \
        "$ORAS_IMAGE@$DIGEST" "$SBOM_NAME:$SBOM_TYPE" > /dev/null)
    done
    echo "  done"
  ) < /dev/null
  STATUS=$?
  set -e
  if [ $STATUS -ne 0 ]; then
    echo "  FAILED" >&2
    FAILED+=("$MODULE")
  fi
done < "$WORK/images"

if [ ${#FAILED[@]} -gt 0 ]; then
  echo "FAILED: ${FAILED[*]}" >&2
  echo "Rerun them with --projects $(IFS=,; echo "${FAILED[*]}")" >&2
  exit 1
fi
echo "Done: the image SBOMs are in $OUT and attached to the images"
