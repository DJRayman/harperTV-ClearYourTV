# Haier Android TV DVB2K (broadway, Android TV 11)

Третий телевизор: Haier Android TV DVB2K (HaierATVT56DVB), Android TV 11, ~0,9 ГБ RAM, 720p, прошивка
RTMA.250416.179 — та же, что в примере CleanYourTV `examples/haier-android-tv-dvb2k`. Лаунчер из `../launcher`
(без кнопки Info: служба InfoKeyService не включена, DUMP не выдан).

- debloat.txt — 17 отключённых пакетов: Anoki ACR, демо-режим, телеметрия и шаринг Haier, очистка, менеджер
  приложений, рекомендации, автоустановка, Netflix, Prime Video, Haier Evo. Голосовой поиск, Chromecast,
  YouTube Music и Play Игры оставлены (как на Hyundai).
- restore.sh — `TV=<ip>:5555 ./restore.sh`.

Особенности:
- Часы стояли на 10 января 2026 и Wi-Fi писал «нет доступа к Интернету» (HTTPS-проверки падали). Исправлено:
  `settings put global ntp_server ntp1.vniiftri.ru` (на Android 11 — имя хоста, не URI).
- Стандартный `tvlauncher` отключён, Home ведёт в новый лаунчер (и после перезагрузки).
- Прошивка при каждой загрузке включает обратно Netflix и `tvrecommendations`.
- Lift не установлен, поэтому док дополняется первыми приложениями из сетки.
