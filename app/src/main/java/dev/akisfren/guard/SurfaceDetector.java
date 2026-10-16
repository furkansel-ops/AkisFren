package dev.akisfren.guard;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Best-effort classification of public accessibility metadata.
 * Caution: Instagram and YouTube can change their UI labels without notice.
 * Intentionally avoid treating the mere presence of a bottom navigation label
 * 'Shorts' or 'Reels' as proof that the short-video player is open.
 */
public final class SurfaceDetector {
    private SurfaceDetector() { }

    public static final class Snapshot {
        private final List<String> labels = new ArrayList<>();
        private final List<String> ids = new ArrayList<>();
        private final List<String> selectedLabels = new ArrayList<>();

        public void add(String text, String description, String viewId, boolean selected) {
            String label = normalize(text) + " " + normalize(description);
            if (!label.trim().isEmpty()) labels.add(label);
            if (viewId != null) ids.add(normalize(viewId));
            if (selected) selectedLabels.add(label);
        }

        public boolean selectedContains(String needle) {
            String q = normalize(needle);
            for (String label : selectedLabels) if (label.contains(q)) return true;
            return false;
        }

        public boolean labelContains(String needle) {
            String q = normalize(needle);
            for (String label : labels) if (label.contains(q)) return true;
            return false;
        }

        public boolean idContains(String needle) {
            String q = normalize(needle);
            for (String id : ids) if (id.contains(q)) return true;
            return false;
        }
    }

    private static String normalize(String input) {
        return input == null ? "" : input.toLowerCase(Locale.ROOT);
    }

    /** Returns one of the AppSettings targets, or empty for unsupported screens. */
    public static String detect(String packageName, Snapshot s) {
        if (packageName == null) return "";
        if (packageName.equals("com.zhiliaoapp.musically") ||
                packageName.equals("com.ss.android.ugc.trill")) {
            return AppSettings.TIKTOK;
        }
        if (packageName.equals("com.instagram.android")) {
            boolean reels = s != null && (
                    s.selectedContains("reels") ||
                    s.idContains("clips_viewer") ||
                    s.idContains("clips_video") ||
                    s.idContains("reels_viewer") ||
                    s.labelContains("original audio") ||
                    s.labelContains("orijinal ses") ||
                    s.labelContains("reels oynatıcısı")
            );
            // Safety first: unknown screens (including DMs and profile) are never
            // interpreted as the feed by default. Missing UI metadata may cause
            // undercounting, but prevents accidental blocking of messaging.
            boolean feed = s != null && (
                    s.selectedContains("home") ||
                    s.selectedContains("ana sayfa") ||
                    s.selectedContains("ana akış") ||
                    s.idContains("main_feed") ||
                    s.idContains("feed_view") ||
                    s.idContains("feed_list")
            );
            return reels ? AppSettings.REELS : (feed ? AppSettings.FEED : "");
        }
        if (packageName.equals("com.google.android.youtube")) {
            boolean shorts = s != null && (
                    s.selectedContains("shorts") ||
                    s.idContains("reel_watch") ||
                    s.idContains("shorts_player") ||
                    s.idContains("shorts_video") ||
                    s.labelContains("shorts player") ||
                    s.labelContains("shorts oynatıcı")
            );
            // Do not accidentally block standard long-form YouTube videos.
            return shorts ? AppSettings.SHORTS : "";
        }
        return "";
    }
}
