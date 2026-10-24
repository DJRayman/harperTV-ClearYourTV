# Hyundai AI PONT (Changhong longshan, Android TV 14)

Второй телевизор: Hyundai AI PONT (DVB95ML), Android TV 14 с сервисами Google, 1,8 ГБ RAM, панель 4K
(интерфейс 1080p). Тот же лаунчер из `../launcher`, своя конфигурация debloat.

- debloat.txt — 16 отключённых пакетов: телеметрия Anoki, рекламные ряды, магазин и утилиты Homwee,
  Alexa, Netflix, Prime Video. Оставлены голосовой поиск, Chromecast, YouTube Music, Play Игры, обновления
  прошивки, повтор экрана, файлы с USB.
- restore.sh — включает всё обратно и возвращает стандартный лаунчер: `TV=<ip>:5555 ./restore.sh`.

Особенности:
- Стандартный `tvlauncher` отключён: даже с ролью HOME у нового лаунчера Home открывал его, пока он был включён.
- Часы отставали на 2 месяца: `time.android.com` с этой сети не отвечает, из-за этого падал HTTPS
  (обои Bing: «Chain validation failed»). Исправлено: `settings put global ntp_server
  "ntp://ru.pool.ntp.org|ntp://ntp1.vniiftri.ru"` (на Android 14 нужен формат URI, просто имя хоста не работает).
- После холодной загрузки ТВ открывает последний источник (эфир), кнопка Home ведёт в лаунчер.
- Прошивка включает `com.cltv.fast` обратно при каждой загрузке.
- Лаунчеру выданы разрешения: `pm grant com.gathin.tvhome android.permission.ACCESS_FINE_LOCATION`
  (имя Wi-Fi) и `android.permission.WRITE_SECURE_SETTINGS` (режимы энергопотребления; ограничения
  `max_cached_processes` на этой прошивке нет, менять режим не обязательно).
