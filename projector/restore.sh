#!/usr/bin/env bash
# Возвращает стандартный лаунчер на проекторе Jixin (T950S)
TV="${TV:?укажите адрес: TV=<ip>:<порт беспроводной отладки> ./restore.sh}"
ADB="${ADB:-$HOME/android/adbw}"
$ADB connect "$TV" >/dev/null
$ADB -s "$TV" shell setprop persist.jx.launcher.highpriority com.google.android.tvlauncher
$ADB -s "$TV" shell cmd package set-home-activity --user 0 com.google.android.tvlauncher/.MainActivity
