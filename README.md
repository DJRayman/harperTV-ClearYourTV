# Harper / SaluteTV: debloat и лаунчер (по CleanYourTV)

Оптимизация телевизора Harper на Sber OS (SaluteTV: MediaTek m5621, Android 9, 1 ГБ RAM) по методике
[CleanYourTV](https://github.com/Gathin23/cleanyourtv): отключение лишних пакетов Сбера через ADB и
минималистичный лаунчер в стиле Apple TV.

Лаунчер (`launcher/`) — форк лаунчера CleanYourTV (MIT, © Gathin, см. `launcher/LICENSE`; шрифт Outfit —
OFL, `launcher/FONT-LICENSE-OFL.txt`). Изменения в этом форке:
- обои: картинка дня Bing с подписью места и автора (`OnlineWallpapers.java`), затемнение верха под часы;
- русские надписи системным шрифтом (в Outfit нет кириллицы), белые приветствие и дата;
- док под мои приложения, включены перестановка плиток и меню по долгому OK.

ADB вызывается из WSL через обёртку над Windows-adb (`~/android/adbw`); ТВ: 192.168.100.172:5555.

- debloat.txt    — отключённые пакеты (pm disable-user --user 0)
- restore.sh     — включает всё обратно, возвращает заставку и лаунчер Сбера
- launcher/      — лаунчер, сборка ниже

Лаунчер Сбера НЕ отключён — он остаётся запасным, через него открываются веб-приложения Салюта (иви и т.п.).

## Включить одно приложение обратно
    ~/android/adbw -s 192.168.100.172:5555 shell pm enable --user 0 ru.sberdevices.videos

## Поменять док / пересобрать лаунчер
Док: launcher/app/src/main/res/values/config.xml → dock_apps (сейчас zona, NUM, Lift). Затем:

    cd launcher
    export JAVA_HOME=~/android/tools/jdk-21.0.12.1+1 PATH=$JAVA_HOME/bin:$PATH
    ./gradlew assembleDebug
    cp app/build/outputs/apk/debug/app-debug.apk /mnt/c/Users/RomaD/tvhome.apk
    ~/android/adbw -s 192.168.100.172:5555 install -r 'C:\Users\RomaD\tvhome.apk'

Перестановка: кнопка ⇅ в верхней панели (OK — взять, стрелки — двигать, Назад — поставить, ещё раз Назад — готово) или долгое OK на плитке → «Переместить». Порядок хранится в лаунчере и переживает перезагрузку.

Обои и темы: см. themes/README.md в репозитории https://github.com/Gathin23/cleanyourtv

## YouTube
Официальный YouTube для ТВ здесь не работает: на Sber OS нет сервисов Google. Вместо него стоит SmartTube
(org.smarttube.stable, 32.68, armeabi-v7a, apk/), подпись автора: SHA-256 109735aa…a482a.
Обновляется сам из своих настроек. Урезанный YouTube Сбера (ru.sberdevices.startube) отключён.

## Обои: картинка дня Bing (те же, что на рабочем столе Windows)
Лаунчер раз в сутки берёт картинки дня Bing за последние ~16 дней (столько хранит Bing) в 1280×720 и меняет
их на главном экране каждые 30 минут. Подпись под кнопками справа вверху: место и автор снимка.
Снимки принадлежат авторам (Getty и др.), Microsoft раздаёт их как обои для личного использования.
Файлы: /sdcard/Android/data/com.gathin.tvhome/files/wallpapers/bing-*.jpg (около 3 МБ, новые докачиваются, старые удаляются).

Настройки в launcher/app/src/main/res/values/config.xml (после правки пересобрать и установить, см. выше):
- online_wallpapers — сколько дней держать (до 16), 0 = выключить загрузку
- wallpaper_rotation_minutes — смена картинки, минут
- wallpaper_dim, wallpaper_top_shade — общее затемнение и затемнение верхней части, %
- wallpaper_captions — подпись с местом и автором
Код: launcher/app/src/main/java/com/gathin/tvhome/OnlineWallpapers.java
