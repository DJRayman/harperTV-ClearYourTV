# Xiaomi Mi TV Stick 4K (MiTV-AYFR0, soul, Android TV 14)

Стик Xiaomi на Amlogic (soul), Google TV, Android 14, 2 ГБ RAM, 4K. Прошивка
UKG3.250107.001 / V816.0.26.6.10. Точный пример в CleanYourTV: `examples/xiaomi-mitv-ayfr0`.

- debloat.txt — 21 пакет (`pm disable-user --user 0`): телеметрия Xiaomi/Google, PatchWall и рекламные
  ряды (`com.mitv.tvhome.*`, `com.xm.webcontent`), рекомендации, автоустановки, служба обновлений,
  Netflix, Prime Video, YouTube Music, Play Игры, встроенные плеер/музыка Xiaomi, печать. Голосовой поиск
  (katniss) и Chromecast (mediashell) оставлены по выбору владельца.
- restore.sh — `TV=<ip>:5555 ./restore.sh`, включает всё обратно и возвращает стандартный лаунчер.

Лаунчер `com.gathin.tvhome` (из `../launcher`). Настройки стика — в `config.json`, который кладётся на устройство
(`/sdcard/Android/data/com.gathin.tvhome/files/config.json`) и перекрывает сборку:
- `dock_apps` — док: Lift, Zona, YouTube, Кинопоиск;
- `app_keys` — переназначение кнопок пульта (отключённые Netflix и Prime Video):
  - **Netflix** → YouTube (`scan:416`), **Prime Video** → Lift (`scan:207`).

Коды кнопок сняты с этого пульта (Xiaomi RC Consumer Control): у Netflix Android видит scancode 416 (не 215,
который показывает `getevent` на уровне evdev — проверять нужно тем, что приходит в сервис). Нужна включённая
служба спец-возможностей `com.gathin.tvhome/.InputKeyService`:

    adb -s <ip>:5555 shell settings put secure enabled_accessibility_services com.gathin.tvhome/com.gathin.tvhome.InputKeyService
    adb -s <ip>:5555 shell settings put secure accessibility_enabled 1
    adb -s <ip>:5555 push config.json /sdcard/Android/data/com.gathin.tvhome/files/config.json

Переназначение проверено настоящим пультом и переживает перезагрузку (инъекция клавиш через `adb shell input`
до фильтра не доходит). Стандартный лаунчер `com.google.android.tvlauncher` отключён. Стик без HDMI-входа и
тюнера, эфир/источники не используются. Память выросла с ~664 до ~810 МБ свободных.
