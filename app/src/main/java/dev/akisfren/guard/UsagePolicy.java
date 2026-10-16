package dev.akisfren.guard;

/** Pure, deterministic decision helpers, independently unit-testable without Android. */
public final class UsagePolicy {
    private UsagePolicy() { }
    public static int penaltySeconds(int configured, int previousBreaks, boolean escalating) {
        int factor = escalating ? Math.min(3, Math.max(0, previousBreaks) + 1) : 1;
        return Math.min(1800, Math.max(30, configured) * factor);
    }
    public static boolean budgetExceeded(int totalSeconds, int limitMinutes, boolean enabled) {
        return enabled && totalSeconds >= Math.max(1, limitMinutes) * 60;
    }
    /** Separate per-feed daily cap, opted into independently of the shared budget. */
    public static boolean targetBudgetExceeded(int targetSeconds, int limitMinutes, boolean enabled) {
        return budgetExceeded(targetSeconds, limitMinutes, enabled);
    }
    /** One gentle warning per continuous visit, before a regular limit. */
    public static boolean shouldWarnSession(long elapsedMs, int limitSeconds, boolean enabled) {
        return enabled && limitSeconds > 0 && elapsedMs >= (limitSeconds * 1000L * 80L + 99L) / 100L
                && elapsedMs < limitSeconds * 1000L;
    }
    public static String activeMode(boolean schedule, int currentHour,
                                    int sleepHour, int studyHour, String manual) {
        if (schedule) {
            // A sleeping window that crosses midnight and finishes at 07:00.
            boolean sleeping = sleepHour >= 7
                    ? currentHour >= sleepHour || currentHour < 7
                    : currentHour >= sleepHour && currentHour < 7;
            if (sleeping) return "uyku";
            if (currentHour >= studyHour && currentHour < studyHour + 3) return "ders";
        }
        return manual;
    }
    public static int limitSeconds(int configured, String mode) {
        if ("uyku".equals(mode)) return 0;
        if ("ders".equals(mode)) return Math.min(configured, 20);
        if ("is".equals(mode)) return Math.min(configured, 60);
        return configured;
    }
}
