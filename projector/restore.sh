#!/usr/bin/env bash
# Включает обратно всё из debloat.txt и возвращает стандартный лаунчер на проекторе Jixin (T950S)
TV="${TV:?укажите адрес: TV=<ip>:<порт беспроводной отладки> ./restore.sh}"
ADB="${ADB:-$HOME/android/adbw}"
$ADB connect "$TV" >/dev/null
grep -vE '^\s*(#|$)' "$(dirname "$0")/debloat.txt" | awk '{print $1}' | while read -r p; do
  echo -n "$p: "; $ADB -s "$TV" shell "pm enable --user 0 $p" </dev/null
done
$ADB -s "$TV" shell pm enable --user 0 com.google.android.tvlauncher
$ADB -s "$TV" shell pm enable --user 0 com.jixin.launcher2
$ADB -s "$TV" shell setprop persist.jx.launcher.highpriority com.google.android.tvlauncher
$ADB -s "$TV" shell cmd package set-home-activity --user 0 com.google.android.tvlauncher/.MainActivity
