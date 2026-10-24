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
- Тормоза: Кинопоиск (`ru.kinopoisk.tv`) стартует при загрузке, и в фоне его поток `KinopoiskAppLog` ест ~95% одного
  ядра, плюс ~60% на сборку мусора. Фон ему запрещён: `cmd appops set ru.kinopoisk.tv RUN_ANY_IN_BACKGROUND ignore`
  и `am set-standby-bucket ru.kinopoisk.tv restricted`; открытый на экране он работает как обычно.
- Load average на этом ТВ всегда около 50 даже в простое: ~50 потоков драйверов MediaTek (AUDIO, CEC, DISP, DMX…)
  спят в состоянии D. Смотреть надо на %CPU в `top`, а не на load average.
