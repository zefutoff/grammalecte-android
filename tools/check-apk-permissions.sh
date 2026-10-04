#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APK="${1:-$ROOT/app/build/outputs/apk/debug/app-debug.apk}"

if [[ ! -f "$APK" ]]; then
    echo "APK not found: $APK" >&2
    echo "Build it first with: make assemble" >&2
    exit 1
fi

find_aapt2() {
    if command -v aapt2 >/dev/null 2>&1; then
        command -v aapt2
        return 0
    fi

    local sdk
    local candidate

    for sdk in \
        "${ANDROID_SDK_ROOT:-}" \
        "${ANDROID_HOME:-}" \
        "$HOME/Android/Sdk"
    do
        [[ -n "$sdk" && -d "$sdk/build-tools" ]] || continue

        candidate="$(
            find "$sdk/build-tools" \
                -mindepth 2 \
                -maxdepth 2 \
                -type f \
                -name aapt2 \
                -perm -u+x \
                -print |
                sort -V |
                tail -n 1
        )"

        if [[ -n "$candidate" ]]; then
            printf '%s\n' "$candidate"
            return 0
        fi
    done

    return 1
}

if ! AAPT2="$(find_aapt2)"; then
    echo "Unable to locate aapt2." >&2
    echo "Install Android SDK Build Tools or set ANDROID_SDK_ROOT." >&2
    exit 1
fi

PERMISSIONS="$("$AAPT2" dump permissions "$APK")"

if grep -Fq 'android.permission.INTERNET' <<<"$PERMISSIONS"; then
    echo "Privacy invariant violated: final APK requests android.permission.INTERNET." >&2
    echo "$PERMISSIONS" >&2
    exit 1
fi

echo "APK privacy invariant OK: final APK does not request android.permission.INTERNET."
