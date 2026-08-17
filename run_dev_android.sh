#!/usr/bin/env bash

set -euo pipefail

project_directory="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$project_directory"

android_sdk="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if command -v adb >/dev/null 2>&1; then
    adb_command="$(command -v adb)"
elif [[ -n "$android_sdk" && -x "$android_sdk/platform-tools/adb" ]]; then
    adb_command="$android_sdk/platform-tools/adb"
else
    echo "adb is required. Install Android platform tools and add adb to PATH." >&2
    exit 1
fi

"$adb_command" start-server >/dev/null

device_count="$("$adb_command" devices | awk 'NR > 1 && $2 == "device" { count++ } END { print count + 0 }')"
if [[ "$device_count" -eq 0 ]]; then
    if command -v emulator >/dev/null 2>&1; then
        emulator_command="$(command -v emulator)"
    elif [[ -n "$android_sdk" && -x "$android_sdk/emulator/emulator" ]]; then
        emulator_command="$android_sdk/emulator/emulator"
    else
        echo "No ready device was found, and the Android emulator is not installed." >&2
        exit 1
    fi

    avd_name="${TRESTLE_ANDROID_AVD:-$("$emulator_command" -list-avds | sed -n '1p')}"
    if [[ -z "$avd_name" ]]; then
        echo "No Android virtual device is configured." >&2
        echo "Create an AVD or set TRESTLE_ANDROID_AVD to the AVD name, then try again." >&2
        exit 1
    fi

    emulator_log="${TMPDIR:-/tmp}/trestle-android-emulator.log"
    echo "Starting Android virtual device: $avd_name"
    nohup "$emulator_command" -avd "$avd_name" >"$emulator_log" 2>&1 &
    emulator_pid="$!"

    deadline=$((SECONDS + 180))
    emulator_serial=""
    while [[ -z "$emulator_serial" && "$SECONDS" -lt "$deadline" ]]; do
        emulator_serial="$("$adb_command" devices | awk 'NR > 1 && $1 ~ /^emulator-/ && $2 == "device" { print $1; exit }')"
        if [[ -z "$emulator_serial" ]]; then
            if ! kill -0 "$emulator_pid" 2>/dev/null; then
                echo "The Android emulator stopped before it became ready. See $emulator_log." >&2
                exit 1
            fi
            sleep 2
        fi
    done

    if [[ -z "$emulator_serial" ]]; then
        echo "Timed out while waiting for the Android emulator. See $emulator_log." >&2
        exit 1
    fi

    export ANDROID_SERIAL="$emulator_serial"
    while [[ "$("$adb_command" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != "1" ]]; do
        if [[ "$SECONDS" -ge "$deadline" ]]; then
            echo "Timed out while waiting for Android to finish booting. See $emulator_log." >&2
            exit 1
        fi
        sleep 2
    done
fi

if [[ "$device_count" -gt 1 && -z "${ANDROID_SERIAL:-}" ]]; then
    echo "More than one Android device is ready. Set ANDROID_SERIAL to select one." >&2
    "$adb_command" devices -l >&2
    exit 1
fi

./gradlew :androidApp:installDebug "$@"
"$adb_command" shell am start -n net.blockhost.trestle/.MainActivity
