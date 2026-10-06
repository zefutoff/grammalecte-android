#!/usr/bin/env bash
set -euo pipefail

if ! command -v adb >/dev/null 2>&1; then
    echo "adb introuvable." >&2
    exit 1
fi

ADB=(adb)

if [[ -n "${ANDROID_SERIAL:-}" ]]; then
    ADB+=(-s "$ANDROID_SERIAL")
else
    mapfile -t DEVICES < <(
        adb devices |
        awk 'NR > 1 && $2 == "device" { print $1 }'
    )

    if (( ${#DEVICES[@]} == 0 )); then
        echo "Aucun appareil Android disponible via ADB." >&2
        exit 1
    fi

    if (( ${#DEVICES[@]} > 1 )); then
        echo "Plusieurs appareils détectés." >&2
        echo "Définis ANDROID_SERIAL avant de relancer." >&2
        adb devices -l >&2
        exit 1
    fi

    ADB+=(-s "${DEVICES[0]}")
fi

prop() {
    "${ADB[@]}" shell getprop "$1" |
        tr -d '\r'
}

package_row() {
    local package="$1"
    local info
    local version_name
    local version_code

    if ! "${ADB[@]}" shell pm path "$package" >/dev/null 2>&1; then
        printf '| %s | not installed | - |\n' "\`$package\`"
        return
    fi

    info="$("${ADB[@]}" shell dumpsys package "$package")"

    version_name="$(
        sed -n \
            's/^[[:space:]]*versionName=//p' \
            <<<"$info" |
            head -n 1
    )"

    version_code="$(
        sed -n \
            's/^[[:space:]]*versionCode=\([0-9]*\).*/\1/p' \
            <<<"$info" |
            head -n 1
    )"

    printf '| %s | %s | %s |\n' \
        "\`$package\`" \
        "${version_name:-unknown}" \
        "${version_code:-unknown}"
}

echo "## Device environment"
echo
echo "| Field | Value |"
echo "| --- | --- |"
printf '| Manufacturer | %s |\n' "$(prop ro.product.manufacturer)"
printf '| Model | %s |\n' "$(prop ro.product.model)"
printf '| Android | %s |\n' "$(prop ro.build.version.release)"
printf '| API | %s |\n' "$(prop ro.build.version.sdk)"
printf '| Build ID | %s |\n' "$(prop ro.build.id)"
printf '| Security patch | %s |\n' "$(prop ro.build.version.security_patch)"

if (( $# > 0 )); then
    echo
    echo "## Application versions"
    echo
    echo "| Package | Version | Version code |"
    echo "| --- | --- | ---: |"

    for package in "$@"; do
        package_row "$package"
    done
fi
