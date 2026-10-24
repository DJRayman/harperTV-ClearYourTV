# Проектор Jixin на Amlogic T950S (t950s_be311, Android TV 13)

Droidlogic `t950s_be311`, платформа `txhd2`, Android TV 13 (сборка `userdebug`, test-keys, **SELinux выключен**),
~1 ГБ RAM, интерфейс 1280×720 (приложения Jixin рисуют 1920×1080). Прошивка Jixin
(`persist.jx.ota2.flavor=t950s_lps1_atvjlauncher2_ts9_with_miracast_Emotn`). ADB — беспроводная отладка,
порт меняется после каждого отключения Wi-Fi. Похожий пример в CleanYourTV: `examples/stellamore-t950s`
(тот же чип, другой вендор).

## Лаунчер
Стоит `../launcher`. Обычных `set-home-activity` и роли HOME мало: Jixin добавил в PackageManager свой фильтр
(«Match highpriority launcher app»), который выбирает Home по свойству
`persist.jx.launcher.highpriority` (было `com.google.android.tvlauncher`). Поставлено
`setprop persist.jx.launcher.highpriority com.gathin.tvhome` (shell может, SELinux выключен); стандартные лаунчеры отключены
(см. ниже). Вернуть: `TV=<ip>:<порт> ./restore.sh`.
**Внимание:** экран Jixin «Источник при включении» (`BootSourceActivity`, он же пункт в меню источников) при
каждом открытии перезаписывает `persist.jx.launcher.highpriority`: ставит `com.jixin.settings` при включённом
автозапуске источника или пустую строку при выключенном — и Home возвращается к стандартному лаунчеру. Так и
случилось 24.10 при проверке «Скрытых настроек». **Исправлено:** стандартные лаунчеры `com.google.android.tvlauncher`
и `com.jixin.launcher2` отключены, поэтому при пустом или чужом значении свойства Home всё равно ведёт в
`com.gathin.tvhome` (проверено: открыть «Источник при включении» → свойство пустое → Home открывает наш лаунчер).
Ограничение: «автозапуск источника» (сразу HDMI при включении) с нашим лаунчером не совместим. Логика выбора Home — в
`ComputerEngine.applyPostResolutionFilter` (services.jar): при HOME оставляет только пакет из этого свойства
(или из `persist.jx.app.boot.guide`, если задан). Тот же ключ умеет писать `jx.device.provider`
(`replaceHighpriorityLauncher`, дублирует в `/mnt/vendor/tee/launcher.txt`) — не используем. Кнопка «Настройки» лаунчера открывает настройки Jixin
(они зарегистрированы как `android.settings.SETTINGS`). Источники: HDMI1, AV1 (тюнера нет).

## Настройки проектора (com.jixin.settings5)
Экраны (открываются `am start -n com.jixin.settings5/.<Activity>`):
- `SettingsActivity` — главное меню: Сеть, Bluetooth, Источник, Аккаунт; Дисплей: режим проекции, настройка
  трапеции/фокуса, четырёхугловая коррекция, масштаб; Язык, Время, Обновление, Сброс, О проекторе.
- `ProjectionTypeActivity` — обычная / задняя / потолок-передняя / потолок-задняя (сейчас обычная).
- `FocusSettingsActivity` — автофокус, автотрапеция, автофокус при запуске, авто-трапеция при запуске (всё вкл.).
- `BrightnessControlActivity` — высокая / стандартная (выбрана) / экономия. **Не переведён: показывается на тайском.**
  Тот же выбор есть в диалоге `am broadcast -a jixin.intent.action.BRIGHNESS_MODE_DIALOG -p com.jixin.settings5`
  (на русском; подсказки про время работы 1–3,5 ч от батареи — общий текст прошивки, Android батареи не видит).
- `AspectRatioCaleActivity` — масштаб изображения стрелками (100), Меню — сброс.
- Также есть: `InputSourceSelectActivity`, `DeskClockActivity` (часы, они же заставка), `NetflixControlSwitchActivity`,
  `com.jixin.starlight.AmbientLightControlActivity` и `StarProjectorActivity` (подсветка / «звёздное небо», вероятно
  для других моделей), `FactoryResetActivity` (не открывать).
Скрытые диалоги (кнопки пульта), `am broadcast -a jixin.intent.action.<X> -p com.jixin.settings5`:
- `PQ` — режим изображения (стандартный, яркий, живой, фильм, спорт, пользовательский) и режим звука; вёрстка
  наезжает друг на друга при 1280×720.
- `SETTINGS_DIALOG` — быстрое меню: режим изображения, таймер, фокус, Bluetooth.

## Железо (по свойствам)
- 6-осевой датчик положения (`jx.sensor.exist=6-axis`) — автотрапеция; калибровка в `persist.jx.gsensor.calibratedata`.
- Лазерный датчик расстояния ToF (`da0103`, службы `tof_*`) — автофокус; `persist.jx.tof.focus.steps1=-200`.
- Камера 640×480 (`persist.jx.camera.img_*`).
- Аппаратная трапеция в HWC: `persist.vendor.hwc.keystone` = 4 угла (`0,72 1883,48 1734,917 159,965`).
  Служба `com.droidlogic.keystone` — НЕ отключать (см. пример stellamore: ломается геометрия).
- Пульт Bluetooth `RMP5` (HID), Wi-Fi/BT Seekwave, звук HDMI ARC, температура SoC ~46 °C.

## Отключено (debloat.txt)
11 пакетов: `com.whale.display` (Zeasn, реклама + служба спец. возможностей, поставлен магазином сам),
`com.jixin.apppush`, `com.jixin.analytics`, `com.jixin.appstore2` (с плагинами Baidu), рекомендации Google,
мастер `com.jixin.guide`, рекламные фреймворки Android (`adservices`, `ondevicepersonalization`), печать, Netflix
(`com.netflix.mediaclient`, `com.jixin.netflix`). Освободилось ~55 МБ (из памяти ушли магазин с OTA-плагином,
рекомендации, аналитика). Вернуть: `TV=<ip>:<порт> ./restore.sh`.
- Оставлены: обновления (`com.jixin.ota`), повтор экрана (EShare, Emotn, Miracast), файлы, музыка, BT-колонка.
- НЕ трогать: `com.droidlogic.keystone`, `com.jixin.settings5`, `com.jixin.dispatcher` (в нём провайдеры
  вентилятора, трапеции, ручного фокуса и источников), `com.jixin.factorytest` (не открывать), `com.droidlogic.*`.

## Обновление прошивки (проверено 24.10.2026)
Текущая: `2025.06.28.115349` (сборка 28.06.2025), Android 13, патч безопасности 2023-04-05, userdebug, SELinux off.
Онлайн-обновление (`com.jixin.ota/.OnlineOtaActivity`, OtaService 2.0.1) ищет
`ota2/t950s_lps1_atvjlauncher2_ts9_with_miracast_Emotn/ota_cfg.json` на трёх серверах:
`www.chasecolor.com:8080` — таймаут, `www.ji-xintech.com` — 404, `192.168.50.24` — заводская локальная сеть.
Надпись «Это уже последняя версия» показывается при любой ошибке, т.е. канала обновлений для этой прошивки
просто нет. Ставить чужие образы T950S не стоит: калибровка трапеции, ToF и датчик положения привязаны к этой сборке.
Обновление с флешки — `com.jixin.ota/.LocalOtaActivity`, только если продавец пришлёт официальный образ.

## Яркость и вентиляторы (проверено 24.10.2026)
- «Режим яркости» Jixin на этой модели не работает: пишет в `/sys/class/jixin-misc/highlight_mode` и
  `energy_saving_mode`, а таких файлов в драйвере нет. Узел подсветки Amlogic (`backlight-txhd2`) в device tree
  `status = disabled`, `/sys/class/backlight` пуст, `jixin-pwm/jixin-backlight` (упомянут в init) отсутствует —
  яркость лампы программно не регулируется ни системной яркостью, ни PQ.
- Вентиляторы: `/sys/class/jixin-pwm/jixin-fan-in` и `jixin-fan-out` (0–100, `chmod 666` в init), всегда 100:
  `FanProvider` в `com.jixin.dispatcher` ищет `/sys/class/jixin-fan/`, которого нет. Запись работает (проверено 50%).
- Датчика температуры лампы нет: 4 мин на 55% оборотов не сдвинули ни один канал SAR ADC (a0 шум, a1=337 постоянный,
  a2=4094 пустой, a3/a4 — кнопки), SoC 45–50 °C не зависит от оборотов (лог `backup/fanprobe.log`). На I²C-2:
  `sc7a20h`, `sh_imu`, `da0103` (ToF), `lsm6dsl` (в нём есть термометр, но `/dev/i2c-2` только для root).
- Реально распаянный датчик положения — Senodia **SH3001** (`sh_imu`, I²C-2 адрес 0x37, CHIP_ID 0x61); `lsm6dsl` и
  `sc7a20h` в device tree — альтернативы, на шине их нет. В SH3001 есть термометр платы: регистры 0x0C/0x0D (12 бит),
  калибровка 0x20/0x21, `t = (raw - room)/16 + 25 °C` (~40 °C в работе). Читается только с root (`adb root`,
  `i2cget -f -y 2 0x37 0x0c`). Эксперимент с оборотами остановлен по решению владельца (лог `backup/imuprobe.log`):
  **вентиляторы всегда на 100%**, автоматику не делаем. После `adb root`/`unroot` открыт порт 5555
  (`service.adb.tcp.port`, не persist — закроется после перезагрузки).

## Bluetooth
- `com.jixin.btspeaker`: `SourceModeActivity` — наушники/колонки (`jixin.intent.action.BLUETOOTH_MODE_SOURCE`, его
  шлёт плитка Bluetooth в настройках Jixin), режим колонки — приватные `DefaultSinkModeActivity`/`RsdSinkModeActivity`,
  сопряжение пульта — `BluetoothPairNormalActivity` (зажать Влево+Вправо). Поддерживаются только аудио-устройства.
- Ошибка прошивки: «выбор режима» `btspeaker/.MainActivity` (`BLUETOOTH_MODE_SELECTER`) в `onCreate` сразу
  запускает режим колонки и закрывается — выбрать наушники с него нельзя.
- Стандартный `android.settings.BLUETOOTH_SETTINGS` на проекторе не обрабатывал никто.
- Исправлено приложением «Скрытые настройки»: `BluetoothActivity` отвечает на оба действия и предлагает
  наушники/колонки, проектор как колонку, сопряжение пульта; для `BLUETOOTH_MODE_SELECTER` выбрано «Всегда».
