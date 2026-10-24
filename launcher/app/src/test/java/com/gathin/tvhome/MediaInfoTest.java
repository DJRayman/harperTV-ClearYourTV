package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class MediaInfoTest {
    // Hyundai AI PONT, Android 14, Lift playing a 1080p stream.
    private static final List<String> SF14 = Arrays.asList(
            "* Layer 0xb5155000 (Background for SurfaceView[io.lift.app/io.lift.app.player.tv.TvNativePlayerActivity]#238)",
            "      geomBufferSize=[0 0 -1 -1] geomContentCrop=[0 0 -1 -1] geomCrop=[0 0 -1 -1] geomBufferTransform=0 ",
            "      buffer: buffer=0x0 ",
            "      isOpaque=true dataspace=V0_SRGB (142671872) hdr metadata types=0",
            "* Layer 0xb515a000 (SurfaceView[io.lift.app/io.lift.app.player.tv.TvNativePlayerActivity]#236)",
            "      geomBufferSize=[0 0 -1 -1] geomContentCrop=[0 0 -1 -1] geomCrop=[0 0 -1 -1] geomBufferTransform=0 ",
            "      buffer: buffer=0x0 ",
            "* Layer 0xb5158000 (SurfaceView[io.lift.app/io.lift.app.player.tv.TvNativePlayerActivity](BLAST)#237)",
            "      geomBufferSize=[0 0 1920 1088] geomContentCrop=[0 0 1920 1080] geomCrop=[0 0 -1 -1] geomBufferTransform=0 ",
            "      buffer: buffer=0xb61185e0 ",
            "      isOpaque=true hasProtectedContent=false dataspace=V0_BT709 (281083904) hdr metadata types=0",
            "* Layer 0xb5160000 (com.gathin.tvhome/com.gathin.tvhome.MainActivity#90)",
            "      geomBufferSize=[0 0 1920 1080] geomContentCrop=[0 0 1920 1080]",
            "      buffer: buffer=0xb6100000 ");

    private static final List<String> AF14 = Arrays.asList(
            "Output thread 0xaa6ecb80, name AudioOut_D, tid 818, type 0 (MIXER):",
            "  Standby: no",
            "  Sample rate: 48000 Hz",
            "  HAL format: 0x1 (AUDIO_FORMAT_PCM_16_BIT)",
            "  Channel count: 2",
            "  Output devices: 0x2 (AUDIO_DEVICE_OUT_SPEAKER)",
            "  3 Tracks of which 1 are active",
            "Output thread 0xaa700000, name AudioOut_15, tid 900, type 1 (DIRECT):",
            "  Standby: yes",
            "  Sample rate: 48000 Hz",
            "  HAL format: 0xa000000 (AUDIO_FORMAT_E_AC3)",
            "  Channel count: 6",
            "  Output devices: 0x40000 (AUDIO_DEVICE_OUT_HDMI_ARC)",
            "  0 Tracks");

    @Test
    public void videoLayerFromAndroid14Dump() {
        MediaInfo.Video v = MediaInfo.video(SF14);
        assertNotNull(v);
        assertEquals("io.lift.app", v.owner);
        assertEquals(1920, v.width);
        assertEquals(1080, v.height);
        assertEquals("SDR", v.range);
    }

    @Test
    public void contentCropBeatsPaddedBufferOfTheSameLayer() {
        java.util.List<String> both = new java.util.ArrayList<>(SF14);
        both.add("+ Layer (SurfaceView[io.lift.app/io.lift.app.player.tv.TvNativePlayerActivity](BLAST)#237) uid=10084");
        both.add("      layerStack=   0, crop=[  0,   0,  -1,  -1], dataspace=BT709 SMPTE_170M Limited range, defaultPixelFormat=Unknown");
        both.add("      activeBuffer=[1920x1088:1077946624,Unknown 0x000013], tr=[0.00, 0.00][0.00, 0.00]");
        MediaInfo.Video v = MediaInfo.video(both);
        assertEquals(1080, v.height);
        assertEquals(1080, MediaInfo.video(both.subList(14, 17)).height);
        assertEquals("HDR10", MediaInfo.range("BT2020 SMPTE_ST2084 Full range"));
        assertEquals("HLG", MediaInfo.range("BT2020 ARIB_STD_B67 Limited range"));
    }

    @Test
    public void noVideoWhenNoSurfaceView() {
        assertNull(MediaInfo.video(SF14.subList(11, 14)));
    }

    @Test
    public void androidNineLayerUsesActiveBuffer() {
        MediaInfo.Video v = MediaInfo.video(Arrays.asList(
                "+ BufferLayer (SurfaceView - ru.yourok.num/ru.yourok.num.PlayerActivity#0)",
                "  crop=[  0,   0,  -1,  -1], dataspace=BT2020_PQ, activeBuffer=[3840x2160:3840,  23]"));
        assertNotNull(v);
        assertEquals("ru.yourok.num", v.owner);
        assertEquals(3840, v.width);
        assertEquals("HDR10", v.range);
    }

    @Test
    public void audioFromMixerAndDirectThreads() {
        MediaInfo.Audio a = MediaInfo.audio(AF14);
        assertNotNull(a);
        assertEquals("PCM 16 бит", a.format);
        assertEquals(2, a.channels);
        assertEquals(48000, a.sampleRate);
        assertEquals("динамики ТВ", a.devices);
        // The same dump with the Dolby passthrough thread playing: the direct thread wins.
        List<String> playing = new java.util.ArrayList<>(AF14);
        playing.set(8, "  Standby: no");
        playing.set(13, "  1 Track of which 1 is active");
        MediaInfo.Audio d = MediaInfo.audio(playing);
        assertEquals("Dolby Digital Plus (E-AC-3)", d.format);
        assertEquals("HDMI ARC", d.devices);
        assertEquals("5.1", MediaInfo.channelName(d.channels));
    }

    @Test
    public void names() {
        assertEquals("1920×1080 (Full HD)", MediaInfo.resolutionName(1920, 1080));
        assertEquals("3840×2160 (4K UHD)", MediaInfo.resolutionName(3840, 2160));
        assertEquals("1280×720 (HD 720p)", MediaInfo.resolutionName(1280, 720));
        assertTrue(MediaInfo.formatName("E_AC3_JOC").startsWith("Dolby Atmos"));
        assertEquals("Bluetooth", MediaInfo.deviceNames("AUDIO_DEVICE_OUT_BLUETOOTH_A2DP"));
    }
}
