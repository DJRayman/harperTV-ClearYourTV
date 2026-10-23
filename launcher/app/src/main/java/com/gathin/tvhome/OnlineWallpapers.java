package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Wallpapers from Bing's picture of the day, the same pictures Windows shows on the desktop (Windows Spotlight,
 * Bing Wallpaper); config.xml online_wallpapers. At most once a day, in the background, the recent days' pictures
 * are saved into the theme's wallpapers/ folder as bing-*.jpg, with place and photographer in captions.txt, and
 * older bing-* pictures are deleted. Pictures in the folder without the prefix (a pushed theme's) are left alone.
 * The photos belong to their photographers; Microsoft hands them out as wallpapers for personal use.
 */
final class OnlineWallpapers {
    static final String PREFIX = "bing-";
    private static final String TAG = "OnlineWallpapers";
    /** Bing keeps the last 16 days: idx is days back, n at most 8 a request. */
    private static final String API = "https://www.bing.com/HPImageArchive.aspx?format=js&mkt=ru-RU&n=8&idx=";
    private static final int ARCHIVE_DAYS = 16;
    private static final String HOST = "https://www.bing.com";
    private static final String USER_AGENT =
            "tvhome/1.0 (CleanYourTV launcher; https://github.com/Gathin23/cleanyourtv)";
    private static final String PREFS = "online_wallpapers";
    private static final long DAY_MS = 24 * 60 * 60_000L;
    private static final int MAX_BYTES = 10 * 1024 * 1024;
    private static final AtomicBoolean running = new AtomicBoolean();

    private OnlineWallpapers() { }

    /**
     * Refreshes the collection when the last refresh is a day old. {@code onFirst} runs on the main thread when the
     * folder had no downloaded picture before and has some now; later changes reach Home through the settings
     * revision, at its next resume.
     */
    static void refresh(Context context, int count, Runnable onFirst) {
        Context app = context.getApplicationContext();
        SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        File dir = Config.dir(app);
        if (dir == null) return;
        File folder = new File(dir, ThemeFiles.WALLPAPERS);
        boolean had = !downloaded(folder).isEmpty();
        String size = size(app.getResources().getDisplayMetrics().widthPixels);
        if (had && System.currentTimeMillis() - prefs.getLong("checked", 0) < DAY_MS) return;
        if (!running.compareAndSet(false, true)) return;
        new Thread(() -> {
            try {
                if (update(folder, Math.min(count, ARCHIVE_DAYS), size)) {
                    SharedPreferences choices = app.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
                    choices.edit().putLong(Config.SETTINGS_REVISION_KEY,
                            choices.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1).apply();
                    if (!had && !downloaded(folder).isEmpty()) new Handler(Looper.getMainLooper()).post(onFirst);
                }
                prefs.edit().putLong("checked", System.currentTimeMillis()).apply();
            } catch (Exception e) {
                // No network yet, or Bing did not answer: the next resume tries again.
                Log.w(TAG, "refresh failed: " + e);
            } finally {
                running.set(false);
            }
        }, TAG).start();
    }

    /** The picture size Bing serves that covers a screen this wide. */
    static String size(int screenWidth) {
        if (screenWidth <= 1280) return "1280x720";
        if (screenWidth <= 1920) return "1920x1080";
        return "UHD";
    }

    /** Downloads what is missing and deletes what is no longer wanted; true when the folder changed. */
    private static boolean update(File folder, int count, String size) throws Exception {
        List<Picture> pictures = query(count, size);
        if (pictures.isEmpty()) return false;
        if (!folder.isDirectory() && !folder.mkdirs()) return false;
        boolean changed = false;
        Set<String> keep = new HashSet<>();
        List<String> captions = new ArrayList<>();
        for (Picture picture : pictures) {
            File file = new File(folder, picture.fileName);
            if (!file.isFile() || file.length() == 0) {
                if (!download(picture.url, file)) continue;
                changed = true;
            }
            keep.add(picture.fileName);
            captions.add(picture.fileName + "\t" + picture.caption);
        }
        for (File old : downloaded(folder)) {
            if (!keep.contains(old.getName()) && old.delete()) changed = true;
        }
        writeCaptions(new File(folder, WallpaperCaptions.FILE_NAME), captions);
        return changed;
    }

    private static final class Picture {
        final String fileName, url, caption;

        Picture(String fileName, String url, String caption) {
            this.fileName = fileName;
            this.url = url;
            this.caption = caption;
        }
    }

    /** The newest pictures of the day, up to {@code count}, newest first. */
    private static List<Picture> query(int count, String size) throws Exception {
        List<Picture> result = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (int idx = 0; idx < count; idx += 8) {
            JSONObject json = new JSONObject(new String(get(API + idx, 256 * 1024), StandardCharsets.UTF_8));
            JSONArray images = json.optJSONArray("images");
            if (images == null) break;
            for (int i = 0; i < images.length() && result.size() < count; i++) {
                JSONObject image = images.getJSONObject(i);
                String base = image.optString("urlbase", "");
                if (base.isEmpty()) continue;
                String name = fileName(base);
                // Pages overlap at the edges (idx 8 starts with the day idx 0 ended on).
                if (!names.add(name)) continue;
                result.add(new Picture(name, HOST + base + "_" + size + ".jpg",
                        caption(image.optString("copyright", ""))));
            }
        }
        return result;
    }

    /** "/th?id=OHR.YellowstoneWolves_ROW0688697251" -> "bing-YellowstoneWolves.jpg": the same picture in any market. */
    static String fileName(String urlBase) {
        String id = urlBase.replaceFirst("^.*[?&]id=", "").replaceFirst("^OHR\\.", "")
                .replaceFirst("_[A-Z]{2,3}(-[A-Z]{2})?\\d+$", "");
        id = id.replaceAll("[^A-Za-z0-9_.-]", "_");
        if (id.length() > 60) id = id.substring(0, 60);
        return PREFIX + id + ".jpg";
    }

    /**
     * "Two grey wolves, Yellowstone (© Jim/Getty Images)" -> "Two grey wolves, Yellowstone — © Jim/Getty Images",
     * the place shortened first so the photographer's credit fits WallpaperCaptions.MAX_LENGTH.
     */
    static String caption(String copyright) {
        String text = copyright.replaceAll("\\s+", " ").trim();
        int open = text.lastIndexOf(" (©");
        if (open < 0 || !text.endsWith(")")) return shorten(text, WallpaperCaptions.MAX_LENGTH);
        String title = text.substring(0, open).trim();
        String credit = text.substring(open + 2, text.length() - 1).trim();
        int room = WallpaperCaptions.MAX_LENGTH - credit.length() - 3;
        if (room < 8) return shorten(credit, WallpaperCaptions.MAX_LENGTH);
        return title.isEmpty() ? credit : shorten(title, room) + " — " + credit;
    }

    private static String shorten(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1).trim() + "…";
    }

    private static List<File> downloaded(File folder) {
        List<File> result = new ArrayList<>();
        File[] files = folder.listFiles();
        if (files == null) return result;
        for (File file : files) {
            if (file.isFile() && file.getName().startsWith(PREFIX) && !file.getName().endsWith(".part")) result.add(file);
        }
        return result;
    }

    /** Rewrites captions.txt with this collection's lines, keeping the theme's own lines for its pictures. */
    private static void writeCaptions(File file, List<String> ours) throws Exception {
        List<String> lines = new ArrayList<>();
        if (file.isFile()) {
            for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                if (!line.startsWith(PREFIX)) lines.add(line);
            }
        }
        lines.addAll(ours);
        Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
    }

    private static boolean download(String url, File target) {
        File partial = new File(target.getPath() + ".part");
        try {
            byte[] data = get(url, MAX_BYTES);
            try (OutputStream out = new FileOutputStream(partial)) {
                out.write(data);
            }
            return partial.renameTo(target);
        } catch (Exception e) {
            Log.w(TAG, "download failed: " + url + ": " + e);
            partial.delete();
            return false;
        }
    }

    private static byte[] get(String url, int limit) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setConnectTimeout(15_000);
        connection.setReadTimeout(30_000);
        try {
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException("HTTP " + connection.getResponseCode());
            }
            try (InputStream in = connection.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[16 * 1024];
                for (int n; (n = in.read(buffer)) > 0; ) {
                    if (out.size() + n > limit) throw new IllegalStateException("over " + limit + " bytes");
                    out.write(buffer, 0, n);
                }
                return out.toByteArray();
            }
        } finally {
            connection.disconnect();
        }
    }
}
