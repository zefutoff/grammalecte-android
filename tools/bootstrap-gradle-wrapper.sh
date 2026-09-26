#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VERSION="9.6.1"
BIN_SHA256="9c0f7faeeb306cb14e4279a3e084ca6b596894089a0638e68a07c945a32c9e14"
URL="https://services.gradle.org/distributions/gradle-${VERSION}-bin.zip"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

for command in curl unzip sha256sum; do
    if ! command -v "$command" >/dev/null 2>&1; then
        echo "Required command not found: $command" >&2
        exit 1
    fi
done

ARCHIVE="$TMP/gradle-${VERSION}-bin.zip"
printf 'Downloading Gradle %s for one-time wrapper bootstrap...\n' "$VERSION"
curl --fail --location --retry 3 --silent --show-error "$URL" -o "$ARCHIVE"
printf '%s  %s\n' "$BIN_SHA256" "$ARCHIVE" | sha256sum --check --status
unzip -q "$ARCHIVE" -d "$TMP"

cd "$ROOT"
"$TMP/gradle-${VERSION}/bin/gradle" wrapper \
    --gradle-version "$VERSION" \
    --distribution-type bin \
    --gradle-distribution-sha256-sum "$BIN_SHA256"
printf '%s\n' "Gradle wrapper generated and checksum-bootstrapped from the official Gradle distribution."
printf '%s\n' "Commit gradlew, gradlew.bat and gradle/wrapper/* before the first public release."
