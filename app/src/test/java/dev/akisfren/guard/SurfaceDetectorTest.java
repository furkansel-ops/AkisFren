package dev.akisfren.guard;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class SurfaceDetectorTest {
    @Test public void tiktokIsRecognized() {
        assertEquals(AppSettings.TIKTOK,
            SurfaceDetector.detect("com.zhiliaoapp.musically", null));
    }
    @Test public void instagramFeedDoesNotConfuseUnselectedReelsTab() {
        SurfaceDetector.Snapshot snapshot = new SurfaceDetector.Snapshot();
        snapshot.add("Reels", "", "instagram:id/reels_tab", false);
        snapshot.add("Ana Sayfa", "", "instagram:id/home_tab", true);
        assertEquals(AppSettings.FEED,
            SurfaceDetector.detect("com.instagram.android", snapshot));
    }
    @Test public void instagramMessagesAreNotCountedAsFeed() {
        SurfaceDetector.Snapshot snapshot = new SurfaceDetector.Snapshot();
        snapshot.add("Mesajlar", "", "instagram:id/inbox", true);
        assertEquals("", SurfaceDetector.detect("com.instagram.android", snapshot));
    }
    @Test public void instagramUnknownScreenIsNotCounted() {
        assertEquals("", SurfaceDetector.detect("com.instagram.android", null));
    }
    @Test public void instagramReelsSelectedIsRecognized() {
        SurfaceDetector.Snapshot snapshot = new SurfaceDetector.Snapshot();
        snapshot.add("Reels", "", "instagram:id/tab", true);
        assertEquals(AppSettings.REELS,
            SurfaceDetector.detect("com.instagram.android", snapshot));
    }
    @Test public void youtubeNormalVideoNotBlocked() {
        SurfaceDetector.Snapshot snapshot = new SurfaceDetector.Snapshot();
        snapshot.add("Shorts", "", "youtube:id/nav_tab", false);
        assertEquals("", SurfaceDetector.detect("com.google.android.youtube", snapshot));
    }
    @Test public void youtubeShortsSelectedIsRecognized() {
        SurfaceDetector.Snapshot snapshot = new SurfaceDetector.Snapshot();
        snapshot.add("Shorts", "", "youtube:id/nav_tab", true);
        assertEquals(AppSettings.SHORTS, SurfaceDetector.detect("com.google.android.youtube", snapshot));
    }
}
