package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What is playing right now, read from the system's own dumps (InfoKeyService runs dumpsys with the DUMP permission
 * granted over adb): the video layer's size and colour space from SurfaceFlinger, the active audio output's format,
 * channels, rate and device from AudioFlinger. No public API tells an app another app's video size or audio format.
 * Pure parsing, no Android types, so it is tested on the JVM against real dumps (Android 9 and 14 layouts).
 */
final class MediaInfo {
    private MediaInfo() { }

    /** The video on screen: its layer's owner, frame size and dynamic range; sideband = a TV input (HDMI, tuner). */
    static final class Video {
        final String owner;
        final int width, height;
        final String range;
        final boolean sideband;
        /** The size is the visible frame (content crop), not the decoder's padded buffer. */
        final boolean exact;

        Video(String owner, int width, int height, String range, boolean sideband, boolean exact) {
            this.owner = owner;
            this.width = width;
            this.height = height;
            this.range = range;
            this.sideband = sideband;
            this.exact = exact;
        }
    }

    /** The audio output that is playing: format name, channel count, sample rate and output devices. */
    static final class Audio {
        final String format;
        final int channels;
        final int sampleRate;
        final String devices;

        Audio(String format, int channels, int sampleRate, String devices) {
            this.format = format;
            this.channels = channels;
            this.sampleRate = sampleRate;
            this.devices = devices;
        }
    }

    private static final Pattern LAYER_START =
            Pattern.compile("^\\s*(?:\\* Layer 0x[0-9a-f]+|\\+ \\w*Layer) \\((.*)\\)");
    private static final Pattern CONTENT_CROP = Pattern.compile("geomContentCrop=\\[(-?\\d+) (-?\\d+) (-?\\d+) (-?\\d+)\\]");
    private static final Pattern BUFFER_SIZE = Pattern.compile("geomBufferSize=\\[0 0 (\\d+) (\\d+)\\]");
    private static final Pattern ACTIVE_BUFFER = Pattern.compile("activeBuffer=\\[\\s*(\\d+)x\\s*(\\d+)");
    private static final Pattern DATASPACE = Pattern.compile("dataspace=([^,(]+)");
    private static final Pattern NULL_BUFFER = Pattern.compile("buffer: buffer=0x0\\b|activeBuffer=\\[\\s*0x\\s*0");
    private static final Pattern SIDEBAND = Pattern.compile("sidebandStream=0x[1-9a-f]|sideband=(?:0x)?[1-9a-f]");

    /** The largest video layer in a SurfaceFlinger dump, or null when nothing plays in a SurfaceView. */
    static Video video(List<String> dump) {
        Video best = null;
        String name = null;
        List<String> block = new ArrayList<>();
        for (String line : dump) {
            Matcher start = LAYER_START.matcher(line);
            if (start.find()) {
                best = better(best, layer(name, block));
                name = start.group(1);
                block.clear();
            } else if (name != null) {
                block.add(line);
            }
        }
        return better(best, layer(name, block));
    }

    private static Video better(Video a, Video b) {
        if (a == null) return b;
        if (b == null) return a;
        // Android 14 prints every layer twice: once with the content crop, once with only the padded buffer.
        if (a.owner.equals(b.owner) && a.exact != b.exact) return a.exact ? a : b;
        return (long) b.width * b.height > (long) a.width * a.height ? b : a;
    }

    private static Video layer(String name, List<String> lines) {
        if (name == null || !name.contains("SurfaceView") || name.startsWith("Background")) return null;
        String text = String.join("\n", lines);
        boolean sideband = SIDEBAND.matcher(text).find();
        int width = 0, height = 0;
        Matcher crop = CONTENT_CROP.matcher(text);
        if (crop.find()) {
            width = Integer.parseInt(crop.group(3)) - Integer.parseInt(crop.group(1));
            height = Integer.parseInt(crop.group(4)) - Integer.parseInt(crop.group(2));
        }
        boolean exact = width > 0 && height > 0;
        if (!exact) {
            Matcher size = BUFFER_SIZE.matcher(text);
            if (!size.find()) {
                size = ACTIVE_BUFFER.matcher(text);
                if (!size.find()) size = null;
            }
            if (size != null) {
                width = Integer.parseInt(size.group(1));
                height = unpadded(Integer.parseInt(size.group(2)));
            }
        }
        boolean noBuffer = NULL_BUFFER.matcher(text).find();
        if (!sideband && (noBuffer || width <= 0 || height <= 0)) return null;
        Matcher ds = DATASPACE.matcher(text);
        String range = ds.find() ? range(ds.group(1)) : "";
        return new Video(owner(name), Math.max(0, width), Math.max(0, height), range, sideband, exact);
    }

    /** A decoder pads the height to a multiple of 16: 1088 -> 1080, 544 -> 540, 368 -> 360, 2176 -> 2160. */
    static int unpadded(int height) {
        for (int real : new int[]{360, 540, 1080, 2160, 4320}) {
            if (height > real && height == (real + 15) / 16 * 16) return real;
        }
        return height;
    }

    /** "SurfaceView[io.lift.app/io.lift.app.player.Activity](BLAST)#237" or "SurfaceView - io.lift.app/..." -> package. */
    static String owner(String layerName) {
        Matcher m = Pattern.compile("SurfaceView(?:\\[| - )([A-Za-z0-9_.]+)/").matcher(layerName);
        return m.find() ? m.group(1) : "";
    }

    /** SDR, HDR10 or HLG from a SurfaceFlinger dataspace name. */
    static String range(String dataspace) {
        String d = dataspace.toUpperCase(Locale.ROOT);
        if (d.contains("HLG") || d.contains("STD_B67")) return "HLG";
        if (d.contains("PQ") || d.contains("ST2084")) return "HDR10";
        if (d.contains("BT2020")) return "BT.2020";
        return "SDR";
    }

    private static final Pattern THREAD_START = Pattern.compile("^Output thread 0x[0-9a-f]+.*type \\d+ \\((\\w+)\\)");
    private static final Pattern STANDBY = Pattern.compile("^\\s*Standby: (yes|no)");
    private static final Pattern RATE = Pattern.compile("^\\s*Sample rate: (\\d+)");
    private static final Pattern HAL_FORMAT = Pattern.compile("^\\s*HAL format: 0x[0-9a-f]+ \\(AUDIO_FORMAT_(\\w+)\\)");
    private static final Pattern CHANNELS = Pattern.compile("^\\s*Channel count: (\\d+)");
    private static final Pattern DEVICES = Pattern.compile("^\\s*Output devices?: 0x[0-9a-f]+ \\((.*)\\)");
    private static final Pattern ACTIVE_TRACKS = Pattern.compile("(\\d+) Tracks? of which (\\d+) (?:is|are) active");

    /**
     * The playing output of an AudioFlinger dump: a thread out of standby with active tracks. A direct, offload or
     * bitstream thread wins over the mixer, since that is where an undecoded Dolby or DTS stream goes.
     */
    static Audio audio(List<String> dump) {
        Audio best = null;
        boolean bestDirect = false;
        String type = null, format = null, devices = "";
        int rate = 0, channels = 0, active = 0;
        boolean awake = false;
        List<String> lines = new ArrayList<>(dump);
        lines.add("Output thread 0x0, type 0 (END)");
        for (String line : lines) {
            Matcher start = THREAD_START.matcher(line);
            if (start.find()) {
                if (type != null && awake && active > 0 && format != null) {
                    boolean direct = !"MIXER".equals(type) && !"DUPLICATING".equals(type);
                    if (best == null || (direct && !bestDirect)) {
                        best = new Audio(formatName(format), channels, rate, deviceNames(devices));
                        bestDirect = direct;
                    }
                }
                type = start.group(1);
                format = null;
                devices = "";
                rate = channels = active = 0;
                awake = false;
                continue;
            }
            if (type == null) continue;
            Matcher m;
            if ((m = STANDBY.matcher(line)).find()) awake = "no".equals(m.group(1));
            else if ((m = RATE.matcher(line)).find()) rate = Integer.parseInt(m.group(1));
            else if ((m = HAL_FORMAT.matcher(line)).find()) format = m.group(1);
            else if ((m = CHANNELS.matcher(line)).find()) channels = Integer.parseInt(m.group(1));
            else if ((m = DEVICES.matcher(line)).find()) devices = m.group(1);
            else if ((m = ACTIVE_TRACKS.matcher(line)).find()) active = Integer.parseInt(m.group(2));
        }
        return best;
    }

    /** AUDIO_FORMAT_ suffix -> what the stream is, e.g. "E_AC3_JOC" -> "Dolby Atmos (E-AC-3 JOC)". */
    static String formatName(String f) {
        if (f.startsWith("PCM_16")) return "PCM 16 бит";
        if (f.startsWith("PCM_24") || f.startsWith("PCM_8_24")) return "PCM 24 бит";
        if (f.startsWith("PCM_32")) return "PCM 32 бит";
        if (f.startsWith("PCM_FLOAT")) return "PCM float";
        if (f.startsWith("E_AC3_JOC")) return "Dolby Atmos (E-AC-3 JOC)";
        if (f.startsWith("E_AC3")) return "Dolby Digital Plus (E-AC-3)";
        if (f.startsWith("AC3")) return "Dolby Digital (AC-3)";
        if (f.startsWith("AC4")) return "Dolby AC-4";
        if (f.startsWith("DOLBY_TRUEHD")) return "Dolby TrueHD";
        if (f.startsWith("MAT")) return "Dolby MAT (TrueHD/Atmos)";
        if (f.startsWith("DTS_HD")) return "DTS-HD";
        if (f.startsWith("DTS")) return "DTS";
        if (f.startsWith("IEC61937")) return "битстрим IEC 61937";
        if (f.startsWith("AAC")) return "AAC";
        return f;
    }

    /** "AUDIO_DEVICE_OUT_SPEAKER, AUDIO_DEVICE_OUT_HDMI_ARC" -> "динамики ТВ, HDMI ARC". */
    static String deviceNames(String devices) {
        List<String> names = new ArrayList<>();
        for (String d : devices.split("[,|]")) {
            String n = d.trim().replace("AUDIO_DEVICE_OUT_", "");
            if (n.isEmpty()) continue;
            String name;
            if (n.equals("SPEAKER") || n.equals("SPEAKER_SAFE")) name = "динамики ТВ";
            else if (n.equals("HDMI_EARC")) name = "HDMI eARC";
            else if (n.equals("HDMI_ARC")) name = "HDMI ARC";
            else if (n.startsWith("HDMI") || n.equals("AUX_DIGITAL")) name = "HDMI";
            else if (n.equals("SPDIF")) name = "оптика (S/PDIF)";
            else if (n.startsWith("LINE")) name = "линейный выход";
            else if (n.startsWith("BLUETOOTH") || n.startsWith("BLE")) name = "Bluetooth";
            else if (n.startsWith("WIRED")) name = "наушники";
            else if (n.startsWith("USB")) name = "USB-аудио";
            else name = n;
            if (!names.contains(name)) names.add(name);
        }
        return String.join(", ", names);
    }

    /** 2 -> "2.0 (стерео)", 6 -> "5.1", 8 -> "7.1". */
    static String channelName(int channels) {
        switch (channels) {
            case 1: return "1.0 (моно)";
            case 2: return "2.0 (стерео)";
            case 6: return "5.1";
            case 8: return "7.1";
            default: return channels > 0 ? channels + " кан." : "";
        }
    }

    /** 1920x1080 -> "1920×1080 (Full HD)". */
    static String resolutionName(int width, int height) {
        int h = Math.min(width, height), w = Math.max(width, height);
        String label;
        if (w >= 7680 || h >= 4320) label = "8K";
        else if (w >= 3840 || h >= 2160) label = "4K UHD";
        else if (w >= 2560 || h >= 1440) label = "1440p";
        else if (w >= 1920 || h >= 1080) label = "Full HD";
        else if (w >= 1280 || h >= 720) label = "HD 720p";
        else if (h >= 576) label = "576p";
        else if (h >= 480) label = "480p";
        else label = h + "p";
        return width + "×" + height + " (" + label + ")";
    }
}
