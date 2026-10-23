package com.gathin.tvhome;

import java.util.Arrays;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RemoteKeysTest {
    // Key names resolved by a stand-in: android.view.KeyEvent isn't available in JVM tests.
    private static int keyCode(String name) {
        if (name.equals("KEYCODE_BUTTON_3")) return 190;
        if (name.equals("KEYCODE_BUTTON_8")) return 195;
        return 0;
    }

    private static Map<Integer, String> parse(String... entries) {
        return RemoteKeys.parseAppKeys(Arrays.asList(entries), RemoteKeysTest::keyCode);
    }

    private static Map<Integer, String> parseHold(String... entries) {
        return RemoteKeys.parseHoldAppKeys(Arrays.asList(entries), RemoteKeysTest::keyCode);
    }

    @Test public void keyNamesWorkLikeNumbers() {
        Map<Integer, String> keys = parse("KEYCODE_BUTTON_3:com.google.android.youtube.tv", "KEYCODE_NOT_A_KEY:a.b");
        assertEquals(1, keys.size());
        assertEquals("com.google.android.youtube.tv", keys.get(190));
    }

    @Test public void keycodeAndPackage() {
        Map<Integer, String> keys = parse(
                "190:com.google.android.youtube.tv", " 193 : com.netflix.ninja ");
        assertEquals(2, keys.size());
        assertEquals("com.google.android.youtube.tv", keys.get(190));
        assertEquals("com.netflix.ninja", keys.get(193));
    }

    @Test public void badEntriesAreSkipped() {
        Map<Integer, String> keys = parse(
                "com.google.android.youtube.tv", "youtube:com.google.android.youtube.tv", "190:", "");
        assertTrue(keys.isEmpty());
    }

    @Test public void theLastEntryForAKeycodeWins() {
        Map<Integer, String> keys = parse("190:a.b", "190:c.d");
        assertEquals("c.d", keys.get(190));
    }

    @Test public void scancodeEntries() {
        java.util.List<String> entries = Arrays.asList(
                "scan:490:com.wbd.hbomax", " scan : 491 : a.b ", "scan:x:a.b", "scan:12:", "190:c.d", "scan:491:e.f");
        Map<Integer, String> scans = RemoteKeys.parseAppScancodes(entries);
        assertEquals(2, scans.size());
        assertEquals("com.wbd.hbomax", scans.get(490));
        assertEquals("e.f", scans.get(491));
        // and the keycode parser leaves scancode entries alone
        assertEquals(1, parse(entries.toArray(new String[0])).size());
    }
    @Test public void inputTargets() {
        String hdmi = "com.tcl.tvinput/.passthroughinput.TvPassThroughService/HW1413743104";
        Map<Integer, String> keys = parse("4062:input:" + hdmi);
        assertEquals(hdmi, RemoteKeys.inputId(keys.get(4062)));
        assertEquals(hdmi, RemoteKeys.inputId(RemoteKeys.parseAppScancodes(
                java.util.Arrays.asList("scan:586:input:" + hdmi)).get(586)));
        assertEquals(null, RemoteKeys.inputId("com.netflix.ninja"));
        assertEquals(null, RemoteKeys.inputId("input: "));
    }

    @Test public void holdEntriesGetTheirOwnMap() {
        Map<Integer, String> holds = parseHold(
                "hold:KEYCODE_BUTTON_8:org.xbmc.kodi", " hold:190:a.b ", "hold:KEYCODE_NOT_A_KEY:x.y",
                "hold:195:", "190:c.d", "hold:scan:490:d.e", "hold:");
        assertEquals(2, holds.size());
        assertEquals("org.xbmc.kodi", holds.get(195));
        assertEquals("a.b", holds.get(190));
        // the normal parser leaves hold entries alone
        assertTrue(parse("hold:190:a.b").isEmpty());
    }

    @Test public void holdScancodeEntries() {
        Map<Integer, String> scans = RemoteKeys.parseHoldAppScancodes(Arrays.asList(
                "hold:scan:490:com.wbd.hbomax", "hold:scan:491:", "hold:190:a.b", "scan:492:c.d"));
        assertEquals(1, scans.size());
        assertEquals("com.wbd.hbomax", scans.get(490));
    }

    @Test public void holdTimePicksTheHoldApp() {
        assertEquals("hold.app", RemoteKeys.target("app", "hold.app", RemoteKeys.HOLD_MS));
        assertEquals("hold.app", RemoteKeys.target("app", "hold.app", 5000));
        assertEquals("app", RemoteKeys.target("app", "hold.app", RemoteKeys.HOLD_MS - 1));
        assertEquals("app", RemoteKeys.target("app", null, 5000));
        assertNull(RemoteKeys.target(null, "hold.app", 0));
    }
}
