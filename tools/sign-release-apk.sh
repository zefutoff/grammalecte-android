#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

INPUT_APK="${1:-$ROOT/app/build/outputs/apk/release/app-release-unsigned.apk}"
OUTPUT_APK="${2:-$ROOT/app/build/outputs/apk/release/app-release-signed.apk}"

: "${GRAMMALECTE_SIGNING_KEYSTORE:?GRAMMALECTE_SIGNING_KEYSTORE is required}"
: "${GRAMMALECTE_SIGNING_KEY_ALIAS:?GRAMMALECTE_SIGNING_KEY_ALIAS is required}"
: "${GRAMMALECTE_SIGNING_STORE_PASSWORD:?GRAMMALECTE_SIGNING_STORE_PASSWORD is required}"
: "${GRAMMALECTE_SIGNING_KEY_PASSWORD:?GRAMMALECTE_SIGNING_KEY_PASSWORD is required}"

if [[ ! -f "$INPUT_APK" ]]; then
    echo "Unsigned APK not found: $INPUT_APK" >&2
    exit 1
fi

if [[ ! -f "$GRAMMALECTE_SIGNING_KEYSTORE" ]]; then
    echo "Signing keystore not found: $GRAMMALECTE_SIGNING_KEYSTORE" >&2
    exit 1
fi

if [[ "$INPUT_APK" == "$OUTPUT_APK" ]]; then
    echo "Input and output APK paths must be different." >&2
    exit 1
fi

find_android_apksigner() {
    local sdk
    local candidate

    if [[ -n "${GRAMMALECTE_APKSIGNER:-}" ]]; then
        if [[ ! -x "$GRAMMALECTE_APKSIGNER" ]]; then
            echo "GRAMMALECTE_APKSIGNER is not executable: $GRAMMALECTE_APKSIGNER" >&2
            return 1
        fi

        printf '%s\n' "$GRAMMALECTE_APKSIGNER"
        return 0
    fi

    for sdk in \
        "${ANDROID_SDK_ROOT:-}" \
        "${ANDROID_HOME:-}" \
        "$HOME/Android/Sdk"
    do
        [[ -n "$sdk" ]] || continue

        candidate="$sdk/build-tools/34.0.0/apksigner"

        if [[ -x "$candidate" ]]; then
            printf '%s\n' "$candidate"
            return 0
        fi
    done

    return 1
}

if ! APKSIGNER="$(find_android_apksigner)"; then
    echo "Unable to locate Android Build Tools 34.0.0 apksigner." >&2
    echo "Install build-tools;34.0.0 or set GRAMMALECTE_APKSIGNER." >&2
    exit 1
fi

mkdir -p "$(dirname "$OUTPUT_APK")"
rm -f "$OUTPUT_APK"

echo "Using apksigner:"
echo "$APKSIGNER"
echo

"$APKSIGNER" sign \
    --ks "$GRAMMALECTE_SIGNING_KEYSTORE" \
    --ks-key-alias "$GRAMMALECTE_SIGNING_KEY_ALIAS" \
    --ks-pass env:GRAMMALECTE_SIGNING_STORE_PASSWORD \
    --key-pass env:GRAMMALECTE_SIGNING_KEY_PASSWORD \
    --out "$OUTPUT_APK" \
    "$INPUT_APK"

VERIFY_OUTPUT="$(
    "$APKSIGNER" verify \
        --verbose \
        --print-certs \
        "$OUTPUT_APK"
)"

printf '%s\n' "$VERIFY_OUTPUT"

EXPECTED_CERT_FILE="$ROOT/config/release-signing-cert.sha256"

if [[ ! -f "$EXPECTED_CERT_FILE" ]]; then
    echo "Expected signing certificate fingerprint not found: $EXPECTED_CERT_FILE" >&2
    exit 1
fi

EXPECTED_CERT_SHA256="$(
    tr -d '[:space:]:' < "$EXPECTED_CERT_FILE" |
        tr '[:upper:]' '[:lower:]'
)"

SIGNER_COUNT="$(
    awk '/Number of signers:/ { print $NF; exit }' <<< "$VERIFY_OUTPUT"
)"

if [[ "$SIGNER_COUNT" != "1" ]]; then
    echo "Expected exactly one APK signer, found: ${SIGNER_COUNT:-unknown}" >&2
    exit 1
fi

ACTUAL_CERT_SHA256="$(
    awk '/certificate SHA-256 digest:/ { print tolower($NF); exit }' <<< "$VERIFY_OUTPUT"
)"

if [[ -z "$ACTUAL_CERT_SHA256" ]]; then
    echo "Unable to read signer certificate SHA-256 from signed APK." >&2
    exit 1
fi

if [[ "$ACTUAL_CERT_SHA256" != "$EXPECTED_CERT_SHA256" ]]; then
    echo "Unexpected signing certificate." >&2
    echo "Expected: $EXPECTED_CERT_SHA256" >&2
    echo "Actual:   $ACTUAL_CERT_SHA256" >&2
    exit 1
fi

echo
echo "Release signing certificate fingerprint verified."

echo
echo "Signed APK created:"
echo "$OUTPUT_APK"
