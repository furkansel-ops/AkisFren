package dev.akisfren.guard;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.os.PowerManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.ArrayDeque;

/**
 * Watches only the chosen target apps. When a continuous viewing session
 * exceeds the configured duration, returns to the launcher and temporarily
 * returns home again on attempted re-entry. User may disable the service in
 * Android settings. No persistent overlays or device administration.
 */
public class FocusMonitorService extends AccessibilityService {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private AppSettings settings;
    private String latestPackage = "";
    private String currentTarget = "";
    private long currentSince = 0;
    private long lastAccountedAt = 0;
    private int lastResetVersion = 0;
    private boolean connected = false;
    private long lastEventAt = 0;
    private long lastHomeAt = 0;
    private WindowManager windowManager;
    private View brakeOverlay;
    private View intentionOverlay;
    private TextView brakeCount;
    private String brakeTarget = "";
    private long lastWidgetUpdate = 0;
    private boolean warnedSession = false;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (!connected) return;
            checkForeground();
            handler.postDelayed(this, 1000);
        }
    };

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        settings = new AppSettings(this);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        lastResetVersion = settings.resetVersion();
        connected = true;
        handler.removeCallbacks(ticker);
        handler.post(ticker);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        CharSequence pkg = event.getPackageName();
        if (pkg != null) {
            latestPackage = pkg.toString();
            lastEventAt = SystemClock.elapsedRealtime();
        }
    }

    private void checkForeground() {
        if (settings == null) return;
        int reset = settings.resetVersion();
        if (reset != lastResetVersion) {
            lastResetVersion = reset;
            settings.reportActivity("", 0L);
            currentTarget = "";
            warnedSession = false;
            currentSince = 0;
            lastAccountedAt = 0;
        }

        long elapsed = SystemClock.elapsedRealtime();
        settings.settleRewards(System.currentTimeMillis());
        if (elapsed - lastWidgetUpdate > 30000) {
            AkisFrenWidget.updateAll(this);
            lastWidgetUpdate = elapsed;
        }
        if (!brakeTarget.isEmpty() && !settings.enabled(brakeTarget)) dismissBrake();
        if (!brakeTarget.isEmpty() && settings.effectiveBlockedUntil(brakeTarget) <= System.currentTimeMillis())
            dismissBrake();
        if (brakeCount != null && !brakeTarget.isEmpty()) {
            long remaining = settings.effectiveBlockedUntil(brakeTarget) - System.currentTimeMillis();
            brakeCount.setText(remaining > 0 ? ((remaining + 999) / 1000) + " sn" : "Mola sona erdi");
        }
        PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
        KeyguardManager keyguard = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        if (settings.isPaused() || (power != null && !power.isInteractive()) ||
                (keyguard != null && keyguard.isKeyguardLocked())) {
            setCurrent("", elapsed);
            dismissBrake();
            dismissIntention();
            return;
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();
        String packageName = latestPackage;
        SurfaceDetector.Snapshot snapshot = null;
        if (root != null) {
            CharSequence pkg = root.getPackageName();
            if (pkg != null) packageName = pkg.toString();
            if ("com.instagram.android".equals(packageName) ||
                    "com.google.android.youtube".equals(packageName)) {
                snapshot = snapshot(root);
            }
        } else if (SystemClock.elapsedRealtime() - lastEventAt > 1500) {
            // Ignore stale foreground package information when no window is visible.
            packageName = "";
        }

        String target = SurfaceDetector.detect(packageName, snapshot);
        if (!target.equals("") && !settings.enabled(target)) target = "";

        if (target.isEmpty()) {
            setCurrent("", elapsed);
            dismissIntention();
            return;
        }

        long nowWall = System.currentTimeMillis();
        if (settings.exceptionUntil(target) > nowWall) {
            // A specific, explicitly granted five-minute exception overrides pause, budget or penalty.
            if (!target.equals(currentTarget)) setCurrent(target, elapsed);
            int passSeconds = (int)Math.max(0L, Math.min(5L, (elapsed - lastAccountedAt) / 1000L));
            if (passSeconds > 0) {
                settings.addUsage(target, passSeconds);
                lastAccountedAt = elapsed;
            }
            return;
        }
        if (settings.budgetExceeded()) settings.noteBudgetExceeded();
        if (settings.targetBudgetExceeded(target)) settings.noteTargetBudgetExceeded(target);
        if (settings.effectiveLimit(target) == 0 && settings.blockedUntil(target) < nowWall) {
            // Sleep-mode blocks are short so turning off the mode works immediately.
            settings.blockUntil(target, nowWall + 2000);
        }
        long remainingMs = settings.effectiveBlockedUntil(target) - nowWall;
        if (remainingMs > 0) {
            setCurrent("", elapsed);
            sendHome(String.format(java.util.Locale.getDefault(),
                    "%s için mola: %d saniye", targetTitle(target), (remainingMs + 999) / 1000));
            showBrake(target);
            return;
        }

        if (settings.shouldPrompt(target, nowWall)) {
            settings.notePrompt(target, nowWall);
            showIntention(target);
        }
        if (!target.equals(currentTarget)) setCurrent(target, elapsed);
        if (!warnedSession && UsagePolicy.shouldWarnSession(elapsed - currentSince,
                settings.effectiveLimit(target), settings.warningEnabled())) {
            warnedSession = true;
            Toast.makeText(getApplicationContext(), targetTitle(target) + ": limitin %80'ine ulaştın.",
                    Toast.LENGTH_SHORT).show();
        }
        // Persist only seconds of confirmed, foreground, interactive viewing.
        // Cap large scheduler gaps to avoid counting a sleeping/background device.
        int wholeSeconds = (int) Math.max(0L, Math.min(5L, (elapsed - lastAccountedAt) / 1000L));
        if (wholeSeconds > 0) {
            settings.addUsage(target, wholeSeconds);
            lastAccountedAt = elapsed;
        }

        // Limit is for a continuous visit, not accumulated daily usage.
        if (settings.budgetExceeded() || settings.targetBudgetExceeded(target)) {
            if (settings.budgetExceeded()) settings.noteBudgetExceeded();
            setCurrent("", elapsed);
            sendHome(settings.targetBudgetExceeded(target)
                    ? targetTitle(target) + ": günlük hedef süresi doldu."
                    : "Günlük akış bütçesi doldu.");
            showBrake(target);
        } else if (elapsed - currentSince >= settings.effectiveLimit(target) * 1000L) {
            long until = System.currentTimeMillis() + settings.effectivePenalty(target) * 1000L;
            settings.blockUntil(target, until);
            settings.recordTargetBreak(target, until);
            setCurrent("", elapsed);
            sendHome(targetTitle(target) + ": izleme limiti doldu. Mola başladı.");
            showBrake(target);
            AkisFrenWidget.updateAll(this);
        }
    }

    private void setCurrent(String target, long elapsed) {
        if (!target.equals(currentTarget)) {
            currentTarget = target;
            warnedSession = false;
            currentSince = target.isEmpty() ? 0L : elapsed;
            lastAccountedAt = target.isEmpty() ? 0L : elapsed;
            settings.reportActivity(currentTarget, currentSince);
        }
    }

    /** Optional one-time intention prompt for positively identified feeds. */
    private void showIntention(String target) {
        if (windowManager == null || intentionOverlay != null || brakeOverlay != null) return;
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(20), dp(18), dp(20), dp(18));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(18, 54, 64)); background.setCornerRadius(dp(22));
        background.setStroke(dp(1), Color.rgb(56, 196, 168));
        panel.setBackground(background);
        TextView heading = new TextView(this);
        heading.setText("AkışFren • Neden açtın?");
        heading.setTextColor(Color.WHITE);heading.setTextSize(19);heading.setTypeface(null, Typeface.BOLD);
        panel.addView(heading);
        TextView subtitle = new TextView(this);
        subtitle.setText(targetTitle(target) + " için amacını seç. Bu seçim erişimi otomatik açmaz.");
        subtitle.setTextColor(Color.rgb(173, 219, 219));subtitle.setTextSize(12);
        panel.addView(subtitle);
        for (String why : new String[]{"Mesaj", "Video", "Paylaşım"}) {
            Button button = overlayButton(why);
            panel.addView(button);
            button.setOnClickListener(v -> { settings.setIntentReason(why); dismissIntention(); });
        }
        Button skip = overlayButton("Şimdilik geç");
        panel.addView(skip);
        skip.setOnClickListener(v -> dismissIntention());
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                getResources().getDisplayMetrics().widthPixels - dp(34),
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                android.graphics.PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.CENTER;
        try { windowManager.addView(panel, p); intentionOverlay = panel; }
        catch (RuntimeException ignored) { intentionOverlay = null; }
    }
    private void dismissIntention() {
        if (intentionOverlay != null && windowManager != null) {
            try { windowManager.removeView(intentionOverlay); } catch (RuntimeException ignored) { }
        }
        intentionOverlay = null;
    }

    /** An accessible, dismissible native brake panel; no SYSTEM_ALERT_WINDOW permission. */
    private void showBrake(String target) {
        dismissIntention();
        if (windowManager == null || !connected) return;
        if (brakeOverlay != null) {
            // Keep original panel visible while the same target is under a penalty.
            if (target.equals(brakeTarget)) return;
            dismissBrake();
        }
        brakeTarget = target;
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(22), dp(23), dp(22), dp(22));
        GradientDrawable surface = new GradientDrawable();
        surface.setColor(Color.rgb(17, 42, 54));
        surface.setCornerRadius(dp(22));
        surface.setStroke(dp(1), Color.rgb(44, 160, 143));
        panel.setBackground(surface);
        TextView heading = new TextView(this);
        heading.setText("AkışFren • Mola zamanı");
        heading.setTextColor(Color.rgb(87, 234, 203));
        heading.setTextSize(18); heading.setTypeface(null, Typeface.BOLD);
        panel.addView(heading);
        TextView info = new TextView(this);
        info.setText(targetTitle(target) + " için kısa bir ara.\nİstersen erişimi açıkça geçici olarak açabilirsin.");
        info.setTextColor(Color.WHITE); info.setTextSize(14);
        info.setPadding(0, dp(8), 0, dp(12)); panel.addView(info);
        brakeCount = new TextView(this);
        brakeCount.setTextColor(Color.rgb(255, 188, 158));
        brakeCount.setTextSize(27); brakeCount.setTypeface(null, Typeface.BOLD);
        brakeCount.setText((Math.max(0, settings.effectiveBlockedUntil(target) - System.currentTimeMillis()) / 1000) + " sn");
        panel.addView(brakeCount);
        Button close = overlayButton("Tamam, ara vereceğim");
        panel.addView(close); close.setOnClickListener(v -> dismissBrake());
        Button exception = overlayButton("Acil 5 dk • " + (settings.usedEmergencyToday() == 0 ? "1 hak" : "hak bitti"));
        panel.addView(exception);
        exception.setEnabled(settings.usedEmergencyToday() == 0);
        exception.setOnClickListener(v -> {
            if (settings.applyEmergency(target)) { dismissBrake();
                Toast.makeText(this, "5 dakikalık istisna başladı.", Toast.LENGTH_SHORT).show(); }
        });
        Button spend = overlayButton("Bankadan 5 dk • " + settings.bankSeconds() / 60 + " dk var");
        panel.addView(spend);
        spend.setEnabled(settings.bankSeconds() >= 300);
        spend.setOnClickListener(v -> {
            if (settings.spendBank(target)) { dismissBrake();
                Toast.makeText(this, "5 dakika kullanıldı.", Toast.LENGTH_SHORT).show(); }
        });
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                getResources().getDisplayMetrics().widthPixels - dp(34),
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                android.graphics.PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.CENTER;
        try { windowManager.addView(panel, p); brakeOverlay = panel; }
        catch (RuntimeException ex) { brakeOverlay = null; brakeCount = null; brakeTarget = ""; }
    }
    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
    private Button overlayButton(String text) {
        Button b = new Button(this);
        b.setText(text); b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(27, 113, 112)));
        return b;
    }
    private void dismissBrake() {
        if (brakeOverlay != null && windowManager != null) {
            try { windowManager.removeView(brakeOverlay); }
            catch (RuntimeException ignored) { }
        }
        brakeOverlay = null; brakeCount = null; brakeTarget = "";
    }
    private void sendHome(String message) {
        long now = SystemClock.elapsedRealtime();
        if (now - lastHomeAt < 1000L) return;
        lastHomeAt = now;
        performGlobalAction(GLOBAL_ACTION_HOME);
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private String targetTitle(String target) {
        for (int i = 0; i < AppSettings.TARGETS.length; i++) {
            if (AppSettings.TARGETS[i].equals(target)) return AppSettings.TITLES[i];
        }
        return target;
    }

    private SurfaceDetector.Snapshot snapshot(AccessibilityNodeInfo root) {
        SurfaceDetector.Snapshot result = new SurfaceDetector.Snapshot();
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);
        int inspected = 0;
        while (!queue.isEmpty() && inspected < 350) {
            AccessibilityNodeInfo n = queue.removeFirst();
            inspected++;
            try {
                CharSequence text = n.getText();
                CharSequence description = n.getContentDescription();
                String id = n.getViewIdResourceName();
                result.add(text == null ? "" : text.toString(),
                        description == null ? "" : description.toString(),
                        id, n.isSelected());
                for (int i = 0; i < n.getChildCount(); i++) {
                    AccessibilityNodeInfo child = n.getChild(i);
                    if (child != null) queue.add(child);
                }
            } catch (IllegalStateException ignored) {
                // Node tree can change while we traverse it.
            }
        }
        return result;
    }

    @Override public void onInterrupt() { }

    @Override public void onDestroy() {
        connected = false;
        handler.removeCallbacks(ticker);
        dismissBrake();
        dismissIntention();
        if (settings != null) settings.reportActivity("", 0);
        super.onDestroy();
    }
}
