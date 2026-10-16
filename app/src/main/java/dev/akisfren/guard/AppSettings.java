package dev.akisfren.guard;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.LocalDate;
import java.time.LocalTime;
import android.os.SystemClock;

/** On-device settings and daily counters. No network permissions or telemetry. */
public final class AppSettings {
    private static final String FILE = "akisfren_preferences";
    public static final String REELS = "reels";
    public static final String FEED = "feed";
    public static final String SHORTS = "shorts";
    public static final String TIKTOK = "tiktok";
    public static final String[] TARGETS = { REELS, FEED, SHORTS, TIKTOK };
    public static final String[] TITLES = { "Instagram Reels", "Instagram Ana Akış", "YouTube Shorts", "TikTok" };
    public static final int[] DEFAULT_LIMIT = { 20, 20, 60, 60 };
    public static final int[] DEFAULT_PENALTY = { 300, 300, 60, 60 };

    private final SharedPreferences prefs;

    public AppSettings(Context context) {
        prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    private int index(String target) {
        for (int i = 0; i < TARGETS.length; i++) {
            if (TARGETS[i].equals(target)) return i;
        }
        throw new IllegalArgumentException("Unrecognized target: " + target);
    }

    public boolean enabled(String target) {
        index(target);
        return prefs.getBoolean(target + "_enabled", true);
    }

    public void setEnabled(String target, boolean value) {
        index(target);
        prefs.edit().putBoolean(target + "_enabled", value).apply();
    }

    public int limit(String target) { return prefs.getInt(target + "_limit", DEFAULT_LIMIT[index(target)]); }
    public void setLimit(String target, int seconds) {
        index(target);
        prefs.edit().putInt(target + "_limit", Math.max(5, Math.min(300, seconds))).apply();
    }

    public int penalty(String target) { return prefs.getInt(target + "_penalty", DEFAULT_PENALTY[index(target)]); }
    public void setPenalty(String target, int seconds) {
        index(target);
        prefs.edit().putInt(target + "_penalty", Math.max(30, Math.min(1800, seconds))).apply();
    }

    public long blockedUntil(String target) {
        index(target);
        return prefs.getLong(target + "_until", 0L);
    }
    public void blockUntil(String target, long timestampMs) {
        index(target);
        prefs.edit().putLong(target + "_until", timestampMs).apply();
    }

    /** A temporary user-controlled pause; original target settings remain unchanged. */
    public long pausedUntil() { return prefs.getLong("paused_until", 0L); }
    public boolean isPaused() { return pausedUntil() > System.currentTimeMillis(); }
    public void pauseForMinutes(int minutes) {
        prefs.edit().putLong("paused_until", System.currentTimeMillis() + Math.max(1, minutes) * 60000L).apply();
    }
    public void resumeProtection() { prefs.edit().remove("paused_until").apply(); }

    public void reportActivity(String target, long startElapsedMs) {
        prefs.edit().putString("active_target", target)
                .putLong("active_since_elapsed", startElapsedMs).apply();
    }
    public String activeTarget() { return prefs.getString("active_target", ""); }
    public long activeSince() { return prefs.getLong("active_since_elapsed", 0L); }

    /** Daily statistics follow the phone's local date; old counters are automatically discarded. */
    private void ensureToday() {
        String today = LocalDate.now().toString();
        if (today.equals(prefs.getString("usage_day", ""))) return;
        SharedPreferences.Editor e = prefs.edit();
        e.putString("usage_day", today).putInt("today_breaks", 0);
        e.putInt("emergency_today", 0);
        for (String target : TARGETS) {
            e.putInt(target + "_today_seconds", 0);
            e.putInt("today_break_" + target, 0);
        }
        e.apply();
    }
    public int todaySeconds(String target) {
        index(target);
        ensureToday();
        return prefs.getInt(target + "_today_seconds", 0);
    }
    public int todayTotalSeconds() {
        int total = 0;
        for (String target : TARGETS) total += todaySeconds(target);
        return total;
    }
    public int todayBreaks() {
        ensureToday();
        return prefs.getInt("today_breaks", 0);
    }
    public void addUsage(String target, int seconds) {
        index(target);
        if (seconds <= 0) return;
        ensureToday();
        int delta = Math.min(seconds, 60);
        String date = LocalDate.now().toString();
        int hour = LocalTime.now().getHour();
        prefs.edit().putInt(target + "_today_seconds", todaySeconds(target) + delta)
                .putInt("history_" + date + "_" + target, daySeconds(date, target) + delta)
                .putInt("hour_" + date + "_" + hour, hourSeconds(date, hour) + delta)
                .apply();
    }
    public void recordBreak() {
        ensureToday();
        prefs.edit().putInt("today_breaks", todayBreaks() + 1)
                .putInt("break_" + LocalDate.now(), dayBreaks(LocalDate.now().toString()) + 1)
                .apply();
    }

    // --- 2: Local history, hourly summaries and weekly trends (no tracking SDK) ---
    public int daySeconds(String date, String target) { index(target); return prefs.getInt("history_" + date + "_" + target, 0); }
    public int dayTotal(String date) {
        int total = 0;
        for (String t : TARGETS) total += daySeconds(date, t);
        return total;
    }
    public int dayBreaks(String date) { return prefs.getInt("break_" + date, 0); }
    public int hourSeconds(String date, int hour) { return prefs.getInt("hour_" + date + "_" + hour, 0); }
    public int periodSeconds(int fromDaysAgo, int days) {
        int sum = 0;
        for (int d = fromDaysAgo; d < fromDaysAgo + days; d++)
            sum += dayTotal(LocalDate.now().minusDays(d).toString());
        return sum;
    }
    public String peakHourSuggestion() {
        int[] hours = new int[24];
        for (int d = 0; d < 7; d++) {
            String date = LocalDate.now().minusDays(d).toString();
            for (int h = 0; h < 24; h++) hours[h] += hourSeconds(date, h);
        }
        int best = -1, maximum = 0;
        for (int h = 0; h < 24; h++) if (hours[h] > maximum) { maximum = hours[h]; best = h; }
        if (best < 0 || maximum < 600) return "Öneri oluşturmak için en az 10 dakika tanınan akış kullanımı gerekiyor.";
        return String.format(java.util.Locale.getDefault(),
                "Son 7 günde %02d.00–%02d.00 arasında %d dk kullanım var. Bu saatlere odak modu planlayabilirsin.",
                best, (best + 1) % 24, maximum / 60);
    }

    /** CSV export includes only local aggregates, never text scanned by Accessibility. */
    public String exportCsv(int days) {
        int length = Math.max(1, Math.min(365, days));
        StringBuilder csv = new StringBuilder("Tarih,Instagram Reels (sn),Instagram Ana Akış (sn),YouTube Shorts (sn),TikTok (sn),Toplam (sn),Mola sayısı\n");
        for (int d = length - 1; d >= 0; d--) {
            String date = LocalDate.now().minusDays(d).toString();
            csv.append(date);
            for (String target : TARGETS) csv.append(',').append(daySeconds(date, target));
            csv.append(',').append(dayTotal(date)).append(',').append(dayBreaks(date)).append('\n');
        }
        return csv.toString();
    }
    /** Explicit data wipe. Increases reset version so a running service drops its session. */
    public boolean eraseAllLocalData() {
        int generation = resetVersion() + 1;
        return prefs.edit().clear().putInt("reset_version", generation).commit();
    }

    // --- 3: XP is awarded once for COMPLETING a penalty, not for screen time ---
    public int xp() { return prefs.getInt("xp_total", 0); }
    public int level() { return 1 + xp() / 100; }
    public int remainingLevelXp() { return 100 - xp() % 100; }
    public int completedBreaks() { return prefs.getInt("completed_breaks", 0); }
    public void pendingBreakReward(String target, long untilMs) {
        index(target);
        prefs.edit().putLong("reward_pending_" + target, untilMs).apply();
    }
    public void settleRewards(long wallTimeMs) {
        SharedPreferences.Editor editor = prefs.edit();
        int gained = 0;
        int count = 0;
        for (String t : TARGETS) {
            long pending = prefs.getLong("reward_pending_" + t, 0L);
            if (pending > 0 && wallTimeMs >= pending) {
                editor.remove("reward_pending_" + t);
                gained += 10;
                count++;
            }
        }
        if (count > 0) {
            editor.putInt("xp_total", xp() + gained)
                    .putInt("completed_breaks", completedBreaks() + count)
                    .putInt("completed_" + LocalDate.now(), completedOn(LocalDate.now().toString()) + count);
        }
        if (count > 0) editor.apply();
    }
    public int completedOn(String day) { return prefs.getInt("completed_" + day, 0); }
    public int completionStreak() {
        int streak = 0;
        LocalDate date = LocalDate.now();
        if (completedOn(date.toString()) == 0) date = date.minusDays(1);
        while (streak < 90 && completedOn(date.toString()) > 0) {
            streak++; date = date.minusDays(1);
        }
        return streak;
    }

    // --- 4: Voluntary intention before opening an app ---
    public void setIntentReason(String reason) { prefs.edit().putString("intent_reason", reason).apply(); }
    public String intentReason() { return prefs.getString("intent_reason", "Belirlenmedi"); }
    public boolean intentPromptEnabled() { return prefs.getBoolean("intent_prompt_enabled", false); }
    public void setIntentPromptEnabled(boolean enabled) { prefs.edit().putBoolean("intent_prompt_enabled", enabled).apply(); }
    public boolean shouldPrompt(String target, long now) {
        return intentPromptEnabled() && (now - prefs.getLong("intent_prompt_time", 0L) > 15*60_000L
                || !target.equals(prefs.getString("intent_prompt_target", "")));
    }
    public void notePrompt(String target, long now) {
        prefs.edit().putString("intent_prompt_target", target).putLong("intent_prompt_time", now).apply();
    }

    // --- 5: Shared daily budget, OPT-IN. Budget overrun locks tracked feeds to midnight ---
    public boolean budgetEnabled() { return prefs.getBoolean("daily_budget_enabled", false); }
    public void setBudgetEnabled(boolean enabled) { prefs.edit().putBoolean("daily_budget_enabled", enabled).apply(); }
    /** Optional daily budget for ONE recognized feed. Off by default for existing installs. */
    public boolean targetBudgetEnabled(String target) {
        index(target);
        return prefs.getBoolean("target_budget_enabled_" + target, false);
    }
    public void setTargetBudgetEnabled(String target, boolean enabled) {
        index(target);
        prefs.edit().putBoolean("target_budget_enabled_" + target, enabled).apply();
    }
    public int targetBudgetMinutes(String target) {
        index(target);
        return prefs.getInt("target_budget_minutes_" + target, 15);
    }
    public void setTargetBudgetMinutes(String target, int minutes) {
        index(target);
        prefs.edit().putInt("target_budget_minutes_" + target, Math.max(5, Math.min(180, minutes))).apply();
    }
    public boolean targetBudgetExceeded(String target) {
        return UsagePolicy.targetBudgetExceeded(todaySeconds(target), targetBudgetMinutes(target), targetBudgetEnabled(target));
    }
    /** Records at most one daily-cap transition per target and local day. */
    public void noteTargetBudgetExceeded(String target) {
        index(target);
        if (!targetBudgetExceeded(target)) return;
        String key = "target_budget_notice_day_" + target;
        String today = LocalDate.now().toString();
        if (today.equals(prefs.getString(key, ""))) return;
        prefs.edit().putString(key, today).apply();
        recordBreak();
    }
    public boolean warningEnabled() { return prefs.getBoolean("gentle_warning_enabled", true); }
    public void setWarningEnabled(boolean enabled) { prefs.edit().putBoolean("gentle_warning_enabled", enabled).apply(); }
    public int budgetMinutes() { return prefs.getInt("daily_budget_min", 30); }
    public void setBudgetMinutes(int minutes) { prefs.edit().putInt("daily_budget_min", Math.max(5, Math.min(180, minutes))).apply(); }
    public boolean budgetExceeded() { return UsagePolicy.budgetExceeded(todayTotalSeconds(), budgetMinutes(), budgetEnabled()); }
    public boolean firstBudgetNoticeToday() {
        return !LocalDate.now().toString().equals(prefs.getString("budget_notice_day", ""));
    }
    public void noteBudgetExceeded() {
        if (!firstBudgetNoticeToday()) return;
        prefs.edit().putString("budget_notice_day", LocalDate.now().toString()).apply();
        recordBreak();
    }
    public long effectiveBlockedUntil(String target) {
        long regular = blockedUntil(target);
        return (budgetExceeded() || targetBudgetExceeded(target))
                ? Math.max(regular, nextDayMs()) : regular;
    }
    public long nextDayMs() {
        return LocalDate.now().plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    // --- 6: Graduated penalties ---
    public boolean escalate() { return prefs.getBoolean("escalate", false); }
    public void setEscalate(boolean enabled) { prefs.edit().putBoolean("escalate", enabled).apply(); }
    public int effectivePenalty(String target) {
        return UsagePolicy.penaltySeconds(penalty(target), dayTargetBreaks(target), escalate());
    }
    public int dayTargetBreaks(String target) { index(target); ensureToday(); return prefs.getInt("today_break_" + target, 0); }
    public void recordTargetBreak(String target, long untilMs) {
        index(target);
        recordBreak();
        prefs.edit().putInt("today_break_" + target, dayTargetBreaks(target) + 1).apply();
        pendingBreakReward(target, untilMs);
    }

    // --- 7: Manual modes + user-enabled sleep/study schedules ---
    public String manualMode() { return prefs.getString("manual_mode", "serbest"); }
    public void setMode(String mode) {
        if (!"ders".equals(mode) && !"uyku".equals(mode) && !"is".equals(mode) && !"serbest".equals(mode)) return;
        prefs.edit().putString("manual_mode", mode).apply();
    }
    public boolean scheduleEnabled() { return prefs.getBoolean("schedule_enabled", false); }
    public void setScheduleEnabled(boolean b) { prefs.edit().putBoolean("schedule_enabled", b).apply(); }
    public int sleepStartHour() { return prefs.getInt("sleep_hour", 23); }
    public void setSleepStartHour(int h) { prefs.edit().putInt("sleep_hour", Math.max(0, Math.min(23,h))).apply(); }
    public int studyStartHour() { return prefs.getInt("study_hour", 9); }
    public void setStudyStartHour(int h) { prefs.edit().putInt("study_hour", Math.max(0, Math.min(20,h))).apply(); }
    public String activeMode() {
        return UsagePolicy.activeMode(scheduleEnabled(), LocalTime.now().getHour(),
                sleepStartHour(), studyStartHour(), manualMode());
    }
    public int effectiveLimit(String target) {
        return UsagePolicy.limitSeconds(limit(target), activeMode());
    }

    // --- 8: Focus timer / time bank. Completion must be claimed manually. ---
    public int bankSeconds() { return prefs.getInt("bank_seconds", 0); }
    public boolean focusRunning() { return prefs.getLong("focus_start_elapsed", 0L) > 0; }
    public int focusDurationMinutes() { return prefs.getInt("focus_minutes", 25); }
    public long focusRemainingSeconds() {
        long start = prefs.getLong("focus_start_elapsed", 0L);
        if (start == 0L) return 0;
        long diff = SystemClock.elapsedRealtime() - start;
        if (diff < 0) return -1; // reboot: monotonic clock restarted
        return Math.max(0, focusDurationMinutes() * 60L - diff / 1000L);
    }
    public void startFocus(int minutes) {
        prefs.edit().putInt("focus_minutes", minutes)
                .putLong("focus_start_elapsed", SystemClock.elapsedRealtime()).apply();
    }
    public void cancelFocus() { prefs.edit().remove("focus_start_elapsed").apply(); }
    public boolean claimFocus() {
        if (!focusRunning() || focusRemainingSeconds() != 0) return false;
        int bonus = focusDurationMinutes() >= 25 ? 5 * 60 : 3 * 60;
        prefs.edit().remove("focus_start_elapsed")
                .putInt("bank_seconds", Math.min(3600, bankSeconds() + bonus))
                .putInt("focus_sessions", focusSessions() + 1)
                .putInt("xp_total", xp() + 15).apply();
        return true;
    }
    public int focusSessions() { return prefs.getInt("focus_sessions", 0); }

    // --- 9: Explicit, narrow emergency and bank exceptions ---
    public int usedEmergencyToday() { ensureToday(); return prefs.getInt("emergency_today", 0); }
    public long exceptionUntil(String target) { index(target); return prefs.getLong("except_" + target, 0L); }
    public boolean applyEmergency(String target) {
        index(target); ensureToday();
        if (usedEmergencyToday() >= 1) return false;
        prefs.edit().putInt("emergency_today", usedEmergencyToday() + 1)
                .putLong("except_" + target, System.currentTimeMillis() + 5*60_000L)
                .remove("reward_pending_" + target).apply();
        return true;
    }
    public boolean spendBank(String target) {
        index(target);
        if (bankSeconds() < 300) return false;
        prefs.edit().putInt("bank_seconds", bankSeconds() - 300)
                .putLong("except_" + target, System.currentTimeMillis() + 300_000L)
                .remove("reward_pending_" + target).apply();
        return true;
    }

    // --- Settings reset now respects new daily metrics ---
    public int resetVersion() { return prefs.getInt("reset_version", 0); }
    public void resetCounters() {
        SharedPreferences.Editor editor = prefs.edit();
        for (String target : TARGETS) {
            editor.remove(target + "_until");
            editor.remove("reward_pending_" + target);
        }
        editor.putString("active_target", "")
                .putLong("active_since_elapsed", 0L)
                .putInt("reset_version", resetVersion() + 1)
                .apply();
    }
}
