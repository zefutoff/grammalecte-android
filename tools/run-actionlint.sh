#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

ACTIONLINT_VERSION="1.7.12"
TOOLS_DIR="$ROOT/.tools/actionlint/v$ACTIONLINT_VERSION"
ACTIONLINT="$TOOLS_DIR/actionlint"

platform="$(uname -s)"
arch="$(uname -m)"

case "$platform" in
    Linux)
        os="linux"
        ;;
    Darwin)
        os="darwin"
        ;;
    *)
        echo "Unsupported platform for actionlint bootstrap: $platform" >&2
        exit 1
        ;;
esac

case "$arch" in
    x86_64|amd64)
        arch="amd64"
        ;;
    arm64|aarch64)
        arch="arm64"
        ;;
    *)
        echo "Unsupported architecture for actionlint bootstrap: $arch" >&2
        exit 1
        ;;
esac

asset="actionlint_${ACTIONLINT_VERSION}_${os}_${arch}.tar.gz"

case "${os}_${arch}" in
    linux_amd64)
        expected_sha256="8aca8db96f1b94770f1b0d72b6dddcb1ebb8123cb3712530b08cc387b349a3d8"
        ;;
    linux_arm64)
        expected_sha256="325e971b6ba9bfa504672e29be93c24981eeb1c07576d730e9f7c8805afff0c6"
        ;;
    darwin_amd64)
        expected_sha256="5b44c3bc2255115c9b69e30efc0fecdf498fdb63c5d58e17084fd5f16324c644"
        ;;
    darwin_arm64)
        expected_sha256="aba9ced2dee8d27fecca3dc7feb1a7f9a52caefa1eb46f3271ea66b6e0e6953f"
        ;;
    *)
        echo "No pinned actionlint checksum for ${os}_${arch}" >&2
        exit 1
        ;;
esac

if [[ ! -x "$ACTIONLINT" ]]; then
    if ! command -v curl >/dev/null 2>&1; then
        echo "curl is required to bootstrap actionlint." >&2
        exit 1
    fi

    tmpdir="$(mktemp -d)"
    trap 'rm -rf "$tmpdir"' EXIT

    archive="$tmpdir/$asset"
    url="https://github.com/rhysd/actionlint/releases/download/v${ACTIONLINT_VERSION}/${asset}"

    echo "Downloading actionlint v${ACTIONLINT_VERSION} for ${os}/${arch}..."

    curl \
        --fail \
        --location \
        --retry 3 \
        --silent \
        --show-error \
        "$url" \
        --output "$archive"

    if command -v sha256sum >/dev/null 2>&1; then
        actual_sha256="$(sha256sum "$archive" | awk '{print $1}')"
    elif command -v shasum >/dev/null 2>&1; then
        actual_sha256="$(shasum -a 256 "$archive" | awk '{print $1}')"
    else
        echo "sha256sum or shasum is required to verify actionlint." >&2
        exit 1
    fi

    if [[ "$actual_sha256" != "$expected_sha256" ]]; then
        echo "actionlint checksum mismatch." >&2
        echo "Expected: $expected_sha256" >&2
        echo "Actual:   $actual_sha256" >&2
        exit 1
    fi

    mkdir -p "$TOOLS_DIR"
    tar -xzf "$archive" -C "$tmpdir" actionlint
    install -m 0755 "$tmpdir/actionlint" "$ACTIONLINT"

    echo "Installed actionlint v${ACTIONLINT_VERSION}."
fi

(
    cd "$ROOT"
    "$ACTIONLINT"
)
