#!/usr/bin/env bash
# Включает обратно всё из debloat.txt и возвращает стандартный лаунчер Google TV (Haier)
TV="${TV:?укажите адрес ТВ: TV=<ip>:5555 ./restore.sh}"
ADB="${ADB:-$HOME/android/adbw}"
$ADB connect "$TV" >/dev/null
grep -vE '^\s*(#|$)' "$(dirname "$0")/debloat.txt" | while read -r p; do
  echo -n "$p: "; $ADB -s "$TV" shell "pm enable --user 0 $p" </dev/null
done
$ADB -s "$TV" shell pm enable --user 0 com.google.android.tvlauncher
$ADB -s "$TV" shell cmd package set-home-activity --user 0 com.google.android.tvlauncher/.MainActivity
# NTP-сервер по умолчанию (time.android.com с этой сети не отвечал, часы стояли на 10 января)
$ADB -s "$TV" shell settings delete global ntp_server
