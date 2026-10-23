#!/usr/bin/env bash
# Включает обратно всё из debloat.txt
TV="${TV:-192.168.100.172:5555}"
ADB="${ADB:-$HOME/android/adbw}"
$ADB connect "$TV" >/dev/null
grep -vE '^\s*(#|$)' "$(dirname "$0")/debloat.txt" | while read -r p; do
  echo -n "$p: "; $ADB -s "$TV" shell "pm enable --user 0 $p" </dev/null
done
# Заставка Сбера (до debloat была включена)
$ADB -s "$TV" shell settings put secure screensaver_components ru.sberdevices.starscreensaver/ru.sberdevices.starscreensaver.StarScreenSaverService
$ADB -s "$TV" shell settings put secure screensaver_enabled 1
# Вернуть лаунчер Сбера домашним экраном (новый лаунчер можно удалить: pm uninstall com.gathin.tvhome)
$ADB -s "$TV" shell cmd package set-home-activity --user 0 ru.sberdevices.starlauncher/.ui.mainscreen.MainActivity
