package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PowerModeTest {
    @Test
    public void economyRestoresFirmwareValue() {
        assertEquals("max_cached_processes=3", PowerMode.value(PowerMode.ECONOMY, "max_cached_processes=3"));
        assertEquals("", PowerMode.value(PowerMode.ECONOMY, ""));
    }

    @Test
    public void higherModesRaiseCachedLimit() {
        assertEquals("max_cached_processes=8", PowerMode.value(PowerMode.BALANCED, "max_cached_processes=3"));
        assertEquals("max_cached_processes=16", PowerMode.value(PowerMode.MULTITASKING, ""));
    }
}
