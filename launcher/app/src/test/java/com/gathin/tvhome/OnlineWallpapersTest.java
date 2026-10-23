package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;

public class OnlineWallpapersTest {
    @Test
    public void captionKeepsCredit() {
        assertEquals("Medieval Aqueduct, Perugia, Italy — © Marco Saracco/Getty Images",
                OnlineWallpapers.caption("Medieval Aqueduct, Perugia, Italy (© Marco Saracco/Getty Images)"));
        String longPlace = String.join("", Collections.nCopies(100, "x"));
        String caption = OnlineWallpapers.caption(longPlace + " (© Jan Wlodarczyk/Alamy)");
        assertTrue(caption, caption.length() <= WallpaperCaptions.MAX_LENGTH);
        assertTrue(caption, caption.endsWith("— © Jan Wlodarczyk/Alamy"));
        assertEquals("No credit here", OnlineWallpapers.caption("No credit here"));
    }

    @Test
    public void fileNamesAreThemeFilesNames() {
        String name = OnlineWallpapers.fileName("/th?id=OHR.YellowstoneWolves_ROW0688697251");
        assertEquals("bing-YellowstoneWolves.jpg", name);
        assertTrue(ThemeFiles.collectionImage(name));
        assertEquals("bing-MumblesSwansea2026.jpg", OnlineWallpapers.fileName("/th?id=OHR.MumblesSwansea2026_ROW0047867144"));
        assertEquals("bing-NiagaraOnt.jpg", OnlineWallpapers.fileName("/th?id=OHR.NiagaraOnt_EN-US1418204353"));
    }

    @Test
    public void sizeCoversTheScreen() {
        assertEquals("1280x720", OnlineWallpapers.size(1280));
        assertEquals("1920x1080", OnlineWallpapers.size(1920));
        assertEquals("UHD", OnlineWallpapers.size(3840));
    }
}
