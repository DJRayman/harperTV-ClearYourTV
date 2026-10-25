# Xiaomi Mi TV Stick 4K (MiTV-AYFR0, soul, Android TV 14)

Стик Xiaomi на Amlogic (soul), Google TV, Android 14, 2 ГБ RAM, 4K. Прошивка
UKG3.250107.001 / V816.0.26.6.10. Точный пример в CleanYourTV: `examples/xiaomi-mitv-ayfr0`.

- debloat.txt — 21 пакет (`pm disable-user --user 0`): телеметрия Xiaomi/Google, PatchWall и рекламные
  ряды (`com.mitv.tvhome.*`, `com.xm.webcontent`), рекомендации, автоустановки, служба обновлений,
  Netflix, Prime Video, YouTube Music, Play Игры, встроенные плеер/музыка Xiaomi, печать. Голосовой поиск
  (katniss) и Chromecast (mediashell) оставлены по выбору владельца.
- restore.sh — `TV=<ip>:5555 ./restore.sh`, включает всё обратно и возвращает стандартный лаунчер.

Лаунчер `com.gathin.tvhome` (из `../launcher`), док: Lift, Zona, YouTube, Кинопоиск. Стандартный лаунчер
`com.google.android.tvlauncher` отключён. Стик без HDMI-входа и тюнера, поэтому эфир/источники не используются.
После перезагрузки всё держится, память выросла с ~664 до ~810 МБ свободных.
