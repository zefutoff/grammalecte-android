#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

APK="app/build/outputs/apk/debug/app-debug.apk"
PACKAGE="fr.grammalecteandroid.unofficial"
ACTIVITY=".MainActivity"

# Android SDK
if ! command -v adb >/dev/null 2>&1; then
    if [[ -x "$HOME/Android/Sdk/platform-tools/adb" ]]; then
        export ANDROID_HOME="$HOME/Android/Sdk"
        export ANDROID_SDK_ROOT="$ANDROID_HOME"
        export PATH="$ANDROID_HOME/platform-tools:$PATH"
    else
        echo "Erreur : adb introuvable."
        exit 1
    fi
fi

echo "=== Compilation ==="

if ! ./gradlew :app:assembleDebug \
    --no-daemon \
    --no-configuration-cache
then
    echo
    echo "BUILD FAILED : arrêt."
    exit 1
fi

echo
echo "BUILD SUCCESSFUL"

if [[ ! -f "$APK" ]]; then
    echo "Erreur : APK introuvable : $APK"
    exit 1
fi

echo
echo "=== Recherche des appareils ADB ==="

mapfile -t DEVICES < <(
    adb devices |
    awk 'NR > 1 && $2 == "device" { print $1 }'
)

if (( ${#DEVICES[@]} == 0 )); then
    echo "Aucun appareil ADB disponible."
    echo
    adb devices -l
    exit 1
fi

echo

if (( ${#DEVICES[@]} == 1 )); then
    DEVICE="${DEVICES[0]}"

    MODEL="$(adb -s "$DEVICE" shell getprop ro.product.model 2>/dev/null | tr -d '\r')"

    echo "Appareil détecté :"
    echo "  $DEVICE  $MODEL"
else
    echo "Choisis l'appareil :"
    echo

    for i in "${!DEVICES[@]}"; do
        SERIAL="${DEVICES[$i]}"
        MODEL="$(adb -s "$SERIAL" shell getprop ro.product.model 2>/dev/null | tr -d '\r')"

        printf '  %d) %s  %s\n' "$((i + 1))" "$SERIAL" "$MODEL"
    done

    echo

    while true; do
        read -rp "Numéro : " CHOICE

        if [[ "$CHOICE" =~ ^[0-9]+$ ]] &&
           (( CHOICE >= 1 && CHOICE <= ${#DEVICES[@]} )); then
            DEVICE="${DEVICES[$((CHOICE - 1))]}"
            break
        fi

        echo "Choix invalide."
    done
fi

echo
echo "=== Installation sur $DEVICE ==="

adb -s "$DEVICE" install -r "$APK"

echo
echo "=== Redémarrage de l'application ==="

adb -s "$DEVICE" shell am force-stop "$PACKAGE"

adb -s "$DEVICE" shell am start \
    -n "$PACKAGE/$ACTIVITY"

echo
echo "Terminé."
