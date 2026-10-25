# Оптимизация телевизоров (по CleanYourTV)

Очистка и настройка нескольких телевизоров по методике
[CleanYourTV](https://github.com/Gathin23/cleanyourtv): отключение рекламы, слежки и лишних сервисов, а на
Android-телевизорах — минималистичный лаунчер в стиле Apple TV вместо штатного.

Главное устройство — телевизор **Harper на Sber OS** (SaluteTV). Для него собран лаунчер `launcher/`
(форк CleanYourTV, MIT, © Gathin; шрифт Outfit — OFL). Тот же лаунчер ставится и на другие Android-телевизоры.

> Проектор Jixin на Amlogic T950S вынесен в **отдельный репозиторий** (своя прошивка, трапеция, автофокус,
> вентиляторы). Приложение «Скрытые настройки» — там же.

## Телевизоры в этом репозитории

| Папка | Телевизор | Система | Что сделано |
|---|---|---|---|
| корень | **Harper / SaluteTV** (MediaTek m5621) | Sber OS (Android 9) | debloat + лаунчер, SmartTube, обои Bing, эфир, энергорежимы, кнопка Info |
| `hyundai/` | **Hyundai AI PONT** (Changhong longshan) | Android TV 14 | debloat + лаунчер, часы по NTP, стандартный лаунчер отключён |
| `haier/` | **Haier Android TV DVB2K** (broadway) | Android TV 11 | debloat + лаунчер, часы по NTP, Кинопоиск из фона |
| `mistick/` | **Xiaomi Mi TV Stick 4K** (soul) | Android TV 14 | debloat + лаунчер, док Lift/Zona/YouTube/Кинопоиск |

У каждого телевизора — свой `README.md`, `debloat.txt` и `restore.sh` в его папке.

> Телевизоры на Tizen (Samsung) и webOS (LG) сюда не входят: ADB там нет, системные приложения не отключить и
> лаунчер не поставить. webOS вынесен в отдельный репозиторий.

ADB вызывается из WSL через обёртку над Windows-adb (`~/android/adbw`). Адрес телевизора подставляется вместо
`TV_IP` (порт 5555). У каждого телевизора `backup/` с серийником и настройками в git **не** попадает.

## Лаунчер (общий для Android-телевизоров)
Форк CleanYourTV с изменениями:
- обои: картинка дня Bing с подписью места и автора (`OnlineWallpapers.java`), затемнение верха под часы;
- русские надписи системным шрифтом (в Outfit нет кириллицы), белые приветствие и дата;
- док под мои приложения, перестановка плиток (кнопка ⇅ или долгое OK → «Переместить»);
- «Источники» → «Эфирное ТВ»: вход с антенны. Лаунчер шлёт `VIEW livetv://playback/channel`; в
  `live_tv_package` можно перечислить несколько приложений через запятую, берётся первое установленное —
  одна сборка работает и на Sber OS (`ru.sberdevices.starlivetv`), и на Android TV (`com.mediatek.wwtv.tvcenter`);
- «Энергопотребление» (долгое OK на «Настройки»): сколько приложений Android держит в фоне — «Экономия» (3),
  «Сбалансированный» (8), «Многозадачность» (16); `PowerMode.java`, нужен грант `WRITE_SECURE_SETTINGS`;
- кнопка **Info**: карточка с разрешением видео, HDR/SDR, форматом звука и выходом (`MediaInfo.java`,
  `InfoKeyService.java`); нужен грант `DUMP`, проверять настоящим пультом.

### Пересобрать лаунчер
Док: `launcher/app/src/main/res/values/config.xml` → `dock_apps`. Затем:

    cd launcher
    export JAVA_HOME=~/android/tools/jdk-21.0.12.1+1 PATH=$JAVA_HOME/bin:$PATH
    ./gradlew assembleDebug
    ~/android/adbw -s TV_IP:5555 install -r app/build/outputs/apk/debug/app-debug.apk

### Обои: картинка дня Bing
Раз в сутки лаунчер берёт картинки дня Bing за последние ~16 дней в 1280×720 и меняет каждые 30 минут.
Настройки в `config.xml`: `online_wallpapers` (сколько дней, 0 = выкл.), `wallpaper_rotation_minutes`,
`wallpaper_dim`, `wallpaper_top_shade`, `wallpaper_captions`. Код: `OnlineWallpapers.java`.

## Harper / SaluteTV
- `debloat.txt` — 12 отключённых пакетов Сбера (`pm disable-user --user 0`); `restore.sh` возвращает всё и заставку.
- Лаунчер Сбера НЕ отключён — через него открываются веб-приложения Салюта (иви и т.п.).
- YouTube: официальный на Sber OS не работает (нет сервисов Google), стоит **SmartTube** (`org.smarttube.stable`),
  урезанный YouTube Сбера отключён.
- Включить одно приложение обратно: `~/android/adbw -s TV_IP:5555 shell pm enable --user 0 ru.sberdevices.videos`.
