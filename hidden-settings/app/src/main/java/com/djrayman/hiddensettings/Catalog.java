package com.djrayman.hiddensettings;

import java.util.Arrays;
import java.util.List;

/**
 * Every hidden screen this app knows, grouped by where it comes from. Only exported components are listed (an
 * ordinary app cannot start the others), and factory reset and debug screens are left out on purpose. Entries
 * missing on the device in hand are hidden at run time, so one build serves the projector and the TVs.
 * Jixin's BootSourceActivity ("source at power-on") is left out too: merely opening it rewrites
 * persist.jx.launcher.highpriority (empty when boot-to-source is off), and that property picks Home on Jixin firmware,
 * so it threw the replacement launcher out.
 */
final class Catalog {
    private Catalog() { }

    /** One row: an activity to start, or a broadcast to send to a package's exported receiver. */
    static final class Entry {
        final String title;
        final String hint;
        final String pkg;
        /** Activity class name, or null for a broadcast. */
        final String activity;
        /** Broadcast action, or null for an activity. */
        final String action;

        private Entry(String title, String hint, String pkg, String activity, String action) {
            this.title = title;
            this.hint = hint;
            this.pkg = pkg;
            this.activity = activity;
            this.action = action;
        }
    }

    static final class Section {
        final String title;
        final List<Entry> entries;

        Section(String title, Entry... entries) {
            this.title = title;
            this.entries = Arrays.asList(entries);
        }
    }

    private static Entry screen(String title, String hint, String pkg, String activity) {
        return new Entry(title, hint, pkg, activity, null);
    }

    private static Entry dialog(String title, String hint, String pkg, String action) {
        return new Entry(title, hint, pkg, null, action);
    }

    private static final String JIXIN = "com.jixin.settings5";
    private static final String AML = "com.droidlogic.tv.settings";
    private static final String AOSP = "com.android.tv.settings";
    private static final String BT = "com.jixin.btspeaker";

    static final List<Section> SECTIONS = Arrays.asList(
            new Section("Проектор",
                    screen("Настройки проектора", "Главное меню Jixin", JIXIN, JIXIN + ".SettingsActivity"),
                    screen("Режим проекции", "Обычная, задняя, с потолка", JIXIN, JIXIN + ".ProjectionTypeActivity"),
                    screen("Трапеция и фокус", "Автофокус и автотрапеция, в том числе при включении",
                            JIXIN, JIXIN + ".FocusSettingsActivity"),
                    screen("Коррекция по 4 углам", "Ручная трапеция", JIXIN, JIXIN + ".FourPointTrapezoidActivity"),
                    screen("Коррекция по 4 углам, вариант 2", "Второй экран ручной трапеции из прошивки",
                            JIXIN, JIXIN + ".FourPointTrapezoid2Activity"),
                    screen("Масштаб изображения", "Стрелками; Меню — сброс", JIXIN, JIXIN + ".AspectRatioCaleActivity"),
                    dialog("Режим яркости", "Высокая, стандартная, экономия", JIXIN,
                            "jixin.intent.action.BRIGHNESS_MODE_DIALOG"),
                    screen("Режим яркости (экран)", "Тот же выбор; в прошивке не переведён",
                            JIXIN, JIXIN + ".BrightnessControlActivity"),
                    screen("Режимы изображения", "Стандартный, яркий, кино, спорт…", JIXIN, JIXIN + ".PQModesActivity"),
                    dialog("Изображение и звук", "Быстрый выбор режимов, как с кнопки пульта", JIXIN,
                            "jixin.intent.action.PQ"),
                    dialog("Быстрое меню", "Изображение, таймер, фокус, Bluetooth", JIXIN,
                            "jixin.intent.action.SETTINGS_DIALOG"),
                    screen("Выбор источника", "HDMI, AV", JIXIN, JIXIN + ".InputSourceSelectActivity"),
                    screen("Голосовое управление", "Настройки голоса Jixin", JIXIN, JIXIN + ".VoiceActivity"),
                    screen("Все приложения", "Список приложений Jixin", JIXIN, "com.jixin.settings.AllAppActivity"),
                    screen("Часы", "Полноэкранные часы", JIXIN, JIXIN + ".DeskClockActivity"),
                    screen("Заставка", "Настройки заставки Jixin", JIXIN, "com.jixin.screensaver.MainActivity"),
                    screen("Подсветка", "Может не поддерживаться этой моделью", JIXIN,
                            "com.jixin.starlight.AmbientLightControlActivity"),
                    screen("Звёздное небо", "Может не поддерживаться этой моделью", JIXIN,
                            "com.jixin.starlight.StarProjectorActivity")),
            new Section("Bluetooth",
                    screen("Наушники и колонки", "Подключить Bluetooth-наушники или колонку к проектору",
                            BT, BT + ".SourceModeActivity"),
                    screen("Проектор как Bluetooth-колонка", "Звук с телефона через проектор",
                            BT, BT + ".MainActivity"),
                    screen("Сопряжение пульта", "Инструкция: зажать Влево и Вправо на пульте",
                            BT, BT + ".BluetoothPairNormalActivity")),
            new Section("Изображение и звук (Amlogic)",
                    screen("Режим изображения", "Подробные настройки картинки", AML,
                            AML + ".pqsettings.PictureModeActivity"),
                    screen("AI-улучшение изображения", "", AML, AML + ".AiPqFragmentActivity"),
                    screen("Звук", "", AML, AML + ".SoundActivity"),
                    screen("Режим звука", "", AML, AML + ".soundeffect.SoundModeActivity"),
                    screen("DTS Virtual:X", "", AML, AML + ".soundeffect.DtsVirualXSettingActivity"),
                    screen("Тембр", "Высокие и низкие частоты", AML, AML + ".soundeffect.TrebleBassSeekBarActivity"),
                    screen("Баланс", "", AML, AML + ".soundeffect.BalanceSeekBarActivity"),
                    screen("Громкость: дополнительно", "", AML, AML + ".soundeffect.AdvancedVolumeActivity"),
                    screen("HDMI-CEC", "Управление устройствами по HDMI", AML, AML + ".tvoption.HdmiCecActivity"),
                    screen("Разрешение вывода", "", AML, AML + ".sliceprovider.dialog.AdjustResolutionDialogActivity"),
                    screen("Быстрые настройки Amlogic", "", AML, AML + ".QuickSettingActivity"),
                    screen("Ещё", "Прочие настройки Amlogic", AML, AML + ".more.MorePrefFragmentActivity"),
                    screen("Точка доступа Wi-Fi", "", AML, AML + ".wifi.HotSpotActivity")),
            new Section("Android TV",
                    screen("Стандартные настройки", "Меню Android TV, которое прошивка заменила своим",
                            AOSP, AOSP + ".MainSettings"),
                    screen("Для разработчиков", "", AOSP, AOSP + ".system.development.DevelopmentActivity"),
                    screen("Экран и звук", "", AOSP, AOSP + ".device.displaysound.DisplaySoundActivity"),
                    screen("Приложения", "", AOSP, AOSP + ".device.apps.AppsActivity"),
                    screen("Установка из неизвестных источников", "", AOSP,
                            AOSP + ".device.apps.specialaccess.ExternalSourcesActivity"),
                    screen("Заставка", "", AOSP, AOSP + ".device.display.daydream.DaydreamActivity"),
                    screen("Дата и время", "", AOSP, AOSP + ".system.DateTimeActivity"),
                    screen("Язык", "", AOSP, AOSP + ".system.LanguageActivity"),
                    screen("Клавиатура", "", AOSP, AOSP + ".inputmethod.KeyboardActivity"),
                    screen("Входы", "Названия и CEC входов", AOSP, AOSP + ".system.InputsActivity"),
                    screen("Сеть", "", AOSP, AOSP + ".connectivity.NetworkActivity"),
                    screen("Безопасность", "", AOSP, AOSP + ".system.SecurityActivity"),
                    screen("Об устройстве", "", AOSP, AOSP + ".about.AboutActivity"),
                    screen("Состояние", "IP, MAC, время работы", AOSP, AOSP + ".about.StatusActivity")));
}
