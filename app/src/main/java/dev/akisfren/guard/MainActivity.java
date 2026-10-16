package dev.akisfren.guard;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.time.LocalDate;

/** Native, offline dashboard. Browser preview is separate and never blocks apps. */
public class MainActivity extends Activity {
    private static final int REQUEST_CSV = 740;
    private static final int BG = Color.rgb(8, 17, 27);
    private static final int CARD = Color.rgb(22, 40, 55);
    private static final int BORDER = Color.rgb(43, 72, 87);
    private static final int WHITE = Color.rgb(240, 248, 252);
    private static final int MUTED = Color.rgb(154, 181, 191);
    private static final int TEAL = Color.rgb(42, 213, 191);
    private static final int BLUE = Color.rgb(44, 128, 214);
    private static final int RED = Color.rgb(255, 150, 123);
    private static final int GREEN = Color.rgb(66, 219, 164);

    private AppSettings settings;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<TextView> statusViews = new ArrayList<>();
    private final List<TextView> usageViews = new ArrayList<>();
    private final List<ProgressBar> progressViews = new ArrayList<>();
    private final List<TextView> targetBudgetViews = new ArrayList<>();
    private TextView serviceStatus;
    private TextView serviceExplanation;
    private TextView summaryUsage;
    private TextView summaryBreaks;
    private Button settingsButton;
    private Button pauseButton;
    private TextView budgetLabel, xpLabel, badgeLabel, reportLabel, suggestionLabel;
    private TextView weeklyGraph, focusLabel, bankLabel, modeLabel, intentLabel;
    private Button focusClaimButton;
    private LinearLayout controlPanel, habitPanel, analyticsPanel;
    private final List<TextView> navItems = new ArrayList<>();
    private boolean visible;

    private final Runnable updater = new Runnable() {
        @Override public void run() {
            if (!visible) return;
            refreshStatuses();
            handler.postDelayed(this, 1000L);
        }
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = new AppSettings(this);
        buildScreen();
    }
    @Override public void onResume() {
        super.onResume();
        visible = true;
        handler.removeCallbacks(updater);
        handler.post(updater);
    }
    @Override public void onPause() {
        visible = false;
        handler.removeCallbacks(updater);
        super.onPause();
    }

    private int dp(float n) { return (int) (n * getResources().getDisplayMetrics().density + .5f); }
    private GradientDrawable bg(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        if (stroke != Color.TRANSPARENT) d.setStroke(dp(1), stroke);
        return d;
    }
    private GradientDrawable gradient(int from, int to, int radius) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{from, to});
        d.setCornerRadius(dp(radius));
        return d;
    }
    private TextView label(String value, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }
    private LinearLayout vertical() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }
    private LinearLayout horizontal() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }
    private LinearLayout.LayoutParams lp(int width, int height) {
        return new LinearLayout.LayoutParams(width == -1 ? -1 : dp(width), height == -2 ? -2 : dp(height));
    }
    private void addTop(LinearLayout parent, View child, int top) {
        LinearLayout.LayoutParams p = lp(-1, -2);
        p.topMargin = dp(top);
        parent.addView(child, p);
    }
    private Button button(String title, int color) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(title);
        b.setTextColor(WHITE);
        b.setTextSize(13);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(bg(color, 16, Color.TRANSPARENT));
        b.setMinHeight(dp(48));
        return b;
    }
    private TextView miniHeading(String text) {
        TextView t = label(text, 11, TEAL, true);
        t.setLetterSpacing(.14f);
        return t;
    }

    private void buildScreen() {
        statusViews.clear(); usageViews.clear(); progressViews.clear(); targetBudgetViews.clear(); navItems.clear();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setFitsSystemWindows(true);
        scroll.setBackgroundColor(BG);
        scroll.setVerticalScrollBarEnabled(false);
        setContentView(scroll);
        LinearLayout container = vertical();
        container.setPadding(dp(20), dp(26), dp(20), dp(38));
        int widthDp = (int)(getResources().getDisplayMetrics().widthPixels /
                getResources().getDisplayMetrics().density);
        if (widthDp > 680) {
            LinearLayout outer = vertical();
            outer.setGravity(Gravity.CENTER_HORIZONTAL);
            scroll.addView(outer);
            outer.addView(container, lp(620, -2));
        } else scroll.addView(container);

        container.addView(miniHeading("EKRAN SÜRESİ · KONTROL PANELİ"));
        addTop(container, label("AkışFren", 37, WHITE, true), 5);
        addTop(container, label("Daha az kaydır, daha çok yaşa.", 14, MUTED, false), 0);

        LinearLayout hero = vertical();
        hero.setPadding(dp(17), dp(18), dp(17), dp(18));
        hero.setBackground(gradient(Color.rgb(12, 67, 65), Color.rgb(13, 42, 53), 22));
        addTop(container, hero, 23);
        hero.setContentDescription("Koruma durumu ve geçici duraklatma");
        LinearLayout head = horizontal();
        TextView shield = label("◎", 36, TEAL, true);
        head.addView(shield, lp(44, -2));
        LinearLayout detail = vertical();
        head.addView(detail, new LinearLayout.LayoutParams(0, -2, 1f));
        serviceStatus = label("Koruma durumuna bakılıyor", 18, WHITE, true);
        detail.addView(serviceStatus);
        serviceExplanation = label("Her akış için ayrı limit ve mola.", 12, MUTED, false);
        addTop(detail, serviceExplanation, 3);
        hero.addView(head);
        pauseButton = button("15 dk duraklat", Color.rgb(30, 100, 101));
        addTop(hero, pauseButton, 14);
        pauseButton.setOnClickListener(v -> {
            if (settings.isPaused()) {
                settings.resumeProtection();
                refreshStatuses();
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle("Korumayı geçici duraklat")
                    .setItems(new String[]{"15 dakika", "30 dakika", "60 dakika"}, (dialog, which) -> {
                        settings.pauseForMinutes(new int[]{15, 30, 60}[which]);
                        refreshStatuses();
                    }).setNegativeButton("Vazgeç", null).show();
        });

        addTop(container, miniHeading("BUGÜNÜN ÖZETİ"), 24);
        LinearLayout statsRow = horizontal();
        LinearLayout.LayoutParams rowParams = lp(-1, -2);
        rowParams.topMargin = dp(10);
        container.addView(statsRow, rowParams);
        summaryUsage = addSummary(statsRow, "Takip edilen süre", "0 sn");
        summaryBreaks = addSummary(statsRow, "Verilen molalar", "0");
        addTop(container, label("Yalnızca tanınan hedef ekranların süresi sayılır.", 11, MUTED, false), 9);

        buildNavigation(container);
        controlPanel = vertical(); addTop(container, controlPanel, 8);
        habitPanel = vertical(); addTop(container, habitPanel, 8);
        analyticsPanel = vertical(); addTop(container, analyticsPanel, 8);
        addTop(controlPanel, miniHeading("HEDEFLER VE SINIRLAR"), 19);
        for (int i = 0; i < AppSettings.TARGETS.length; i++)
            addTargetCard(controlPanel, AppSettings.TARGETS[i], AppSettings.TITLES[i], i);

        addNewFeaturePanels(habitPanel, analyticsPanel);
        Button reset = button("Molaları ve oturumu sıfırla", BLUE);
        addTop(controlPanel, reset, 21);
        reset.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Molalar sıfırlansın mı?")
                .setMessage("Aktif molalar ve oturum sıfırlanacak. Günlük istatistikler ve limit ayarları korunacak.")
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("Sıfırla", (dialog, which) -> {
                    settings.resetCounters(); refreshStatuses();
                }).show());

        LinearLayout privacy = vertical();
        privacy.setPadding(dp(17), dp(17), dp(17), dp(17));
        privacy.setBackground(bg(CARD, 19, BORDER));
        addTop(controlPanel, privacy, 26);
        privacy.addView(label("Gizlilik ve izin", 17, WHITE, true));
        addTop(privacy, label("Erişilebilirlik izni yalnızca hedef ekranları yerel olarak tanımak ve limit aşımında ana ekrana dönmek için kullanılır. Ekran metinleri kaydedilmez, sunucuya gönderilmez. İzni Android Ayarlarından kaldırabilirsin.", 12, MUTED, false), 7);
        settingsButton = button("Koruma iznini yönet", Color.rgb(22, 114, 107));
        addTop(privacy, settingsButton, 12);
        settingsButton.setOnClickListener(v -> {
            try { startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); }
            catch (Exception e) {
                new AlertDialog.Builder(this).setMessage("Android Ayarlar > Erişilebilirlik bölümünden AkışFren'i yönet.")
                        .setPositiveButton("Tamam", null).show();
            }
        });
        addTop(controlPanel, label("Not: Instagram ve YouTube ekran algılama, uygulama sürümlerine göre değişebilir. Bilinmeyen ekranlar güvenlik için engellenmez. Android ayarlarından koruma kapatılabilir.", 12, MUTED, false), 15);
        showPanel(0);
    }

    private TextView addSummary(LinearLayout root, String caption, String initial) {
        LinearLayout tile = vertical();
        tile.setPadding(dp(13), dp(14), dp(13), dp(14));
        tile.setBackground(bg(CARD, 17, BORDER));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1f);
        params.rightMargin = dp(7);
        root.addView(tile, params);
        tile.addView(label(caption, 11, MUTED, false));
        TextView number = label(initial, 24, WHITE, true);
        addTop(tile, number, 6);
        return number;
    }

    private void addTargetCard(LinearLayout root, String key, String title, int index) {
        LinearLayout card = vertical();
        card.setPadding(dp(16), dp(16), dp(16), dp(18));
        card.setBackground(bg(CARD, 20, BORDER));
        addTop(root, card, 13);

        LinearLayout top = horizontal();
        TextView glyph = label(new String[]{"◎", "▤", "▶", "♫"}[index], 26,
                new int[]{TEAL, TEAL, RED, GREEN}[index], true);
        top.addView(glyph, lp(40, -2));
        LinearLayout nameArea = vertical();
        top.addView(nameArea, new LinearLayout.LayoutParams(0, -2, 1f));
        nameArea.addView(label(title, 16, WHITE, true));
        TextView status = label("● Beklemede", 12, MUTED, true);
        addTop(nameArea, status, 4);
        statusViews.add(status);
        Switch toggle = new Switch(this);
        toggle.setContentDescription(title + " koruması");
        toggle.setChecked(settings.enabled(key));
        toggle.setShowText(false);
        toggle.setThumbTintList(ColorStateList.valueOf(WHITE));
        toggle.setTrackTintList(new ColorStateList(
                new int[][] {new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{TEAL, Color.rgb(87, 109, 118)}));
        top.addView(toggle);
        toggle.setOnCheckedChangeListener((CompoundButton view, boolean checked) -> {
            settings.setEnabled(key, checked); refreshStatuses();
        });
        card.addView(top);

        TextView usage = label("Bugün: 0 sn", 11, MUTED, false);
        addTop(card, usage, 12);
        usageViews.add(usage);
        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(false);
        progress.setMax(100);
        progress.setProgress(0);
        progress.setProgressTintList(ColorStateList.valueOf(TEAL));
        progress.setProgressBackgroundTintList(ColorStateList.valueOf(Color.rgb(47, 68, 81)));
        LinearLayout.LayoutParams progressParams = lp(-1, 6);
        progressParams.topMargin = dp(7);
        card.addView(progress, progressParams);
        progressViews.add(progress);

        LinearLayout presets = horizontal();
        addTop(card, presets, 12);
        presets.addView(label("Hızlı limit:", 11, MUTED, true),
                new LinearLayout.LayoutParams(0, -2, 1f));
        int[] times = {20, 60, 180};
        String[] names = {"20 sn", "1 dk", "3 dk"};
        TextView[] labels = new TextView[1];
        SeekBar limitSeek = addSlider(card, key, index == 1 ? "Akış limiti" : "İzleme limiti", true, labels);
        // Keep quick selections above the slider; they update the real device setting.
        for (int i = 0; i < times.length; i++) {
            final int seconds = times[i];
            TextView chip = label(names[i], 11, WHITE, true);
            chip.setGravity(Gravity.CENTER);
            chip.setBackground(bg(Color.rgb(37, 69, 81), 10, BORDER));
            chip.setPadding(dp(7), dp(7), dp(7), dp(7));
            LinearLayout.LayoutParams p = lp(-2, -2);
            p.leftMargin = dp(5);
            presets.addView(chip, p);
            chip.setClickable(true);
            chip.setFocusable(true);
            chip.setContentDescription("İzleme limitini " + names[i] + " yap");
            chip.setOnClickListener(v -> {
                settings.setLimit(key, seconds);
                limitSeek.setProgress((seconds - 5) / 5);
                labels[0].setText(formatDuration(seconds));
                refreshStatuses();
            });
        }
        addSlider(card, key, "Mola süresi", false, null);
        LinearLayout daily = vertical();
        daily.setPadding(dp(12), dp(12), dp(12), dp(12));
        daily.setBackground(bg(Color.rgb(18, 52, 61), 13, BORDER));
        addTop(card, daily, 13);
        TextView dailyStatus = label("", 12, TEAL, true);
        targetBudgetViews.add(dailyStatus);
        daily.addView(dailyStatus);
        toggleRow(daily, "Bu akışa günlük limit koy", settings.targetBudgetEnabled(key),
                value -> settings.setTargetBudgetEnabled(key, value));
        LinearLayout budgetPresets = horizontal(); addTop(daily, budgetPresets, 8);
        for (int minutes : new int[]{5, 15, 30, 60}) {
            chip(minutes + " dk", budgetPresets, () -> {
                settings.setTargetBudgetMinutes(key, minutes);
                refreshStatuses();
            });
        }
    }

    private SeekBar addSlider(LinearLayout card, String key, String title, boolean isLimit, TextView[] outLabel) {
        LinearLayout line = horizontal();
        addTop(card, line, 17);
        line.addView(label(title, 12, MUTED, false), new LinearLayout.LayoutParams(0, -2, 1f));
        int current = isLimit ? settings.limit(key) : settings.penalty(key);
        TextView value = label(formatDuration(current), 13, isLimit ? TEAL : RED, true);
        line.addView(value);
        if (outLabel != null) outLabel[0] = value;

        SeekBar seek = new SeekBar(this);
        seek.setMax(59);
        seek.setProgress(isLimit ? (current - 5) / 5 : (current - 30) / 30);
        seek.setProgressTintList(ColorStateList.valueOf(TEAL));
        seek.setProgressBackgroundTintList(ColorStateList.valueOf(BLUE));
        seek.setThumbTintList(ColorStateList.valueOf(TEAL));
        LinearLayout.LayoutParams p = lp(-1, 37);
        p.topMargin = dp(1);
        card.addView(seek, p);
        seek.setContentDescription(title + ": " + formatDuration(current));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (!fromUser) return;
                int seconds = isLimit ? 5 + 5 * progress : 30 + 30 * progress;
                value.setText(formatDuration(seconds));
                bar.setContentDescription(title + ": " + formatDuration(seconds));
                if (isLimit) settings.setLimit(key, seconds);
                else settings.setPenalty(key, seconds);
                refreshStatuses();
            }
            @Override public void onStartTrackingTouch(SeekBar b) { }
            @Override public void onStopTrackingTouch(SeekBar b) { }
        });
        return seek;
    }


    // Progressive feature set. All controls persist on the device (SharedPreferences).

    private void buildNavigation(LinearLayout root) {
        LinearLayout nav = horizontal();
        nav.setBackground(bg(Color.rgb(15, 38, 50), 15, BORDER));
        nav.setPadding(dp(4), dp(4), dp(4), dp(4));
        addTop(root, nav, 23);
        String[] captions = {"Kontrol", "Alışkanlıklar", "Analiz"};
        for (int i = 0; i < captions.length; i++) {
            final int panel = i;
            TextView item = label(captions[i], 12, MUTED, true);
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(5), dp(12), dp(5), dp(12));
            nav.addView(item, new LinearLayout.LayoutParams(0, -2, 1f));
            item.setClickable(true); item.setFocusable(true);
            item.setOnClickListener(v -> showPanel(panel));
            navItems.add(item);
        }
    }
    private void showPanel(int panel) {
        if (controlPanel == null || habitPanel == null || analyticsPanel == null) return;
        controlPanel.setVisibility(panel == 0 ? View.VISIBLE : View.GONE);
        habitPanel.setVisibility(panel == 1 ? View.VISIBLE : View.GONE);
        analyticsPanel.setVisibility(panel == 2 ? View.VISIBLE : View.GONE);
        for (int i = 0; i < navItems.size(); i++) {
            TextView item = navItems.get(i);
            boolean selected = i == panel;
            item.setTextColor(selected ? WHITE : MUTED);
            item.setBackground(bg(selected ? Color.rgb(35, 85, 87) : Color.TRANSPARENT, 12, Color.TRANSPARENT));
            item.setSelected(selected);
        }
    }

    private LinearLayout featureCard(LinearLayout root, String title, String description) {
        LinearLayout card = vertical();
        card.setPadding(dp(16), dp(17), dp(16), dp(16));
        card.setBackground(bg(CARD, 20, BORDER));
        addTop(root, card, 12);
        card.addView(label(title, 17, WHITE, true));
        addTop(card, label(description, 12, MUTED, false), 6);
        return card;
    }
    private TextView chip(String label, LinearLayout row, Runnable task) {
        TextView text = label(label, 12, WHITE, true);
        text.setGravity(Gravity.CENTER);
        text.setPadding(dp(9), dp(11), dp(9), dp(11));
        text.setBackground(bg(Color.rgb(30, 75, 81), 12, Color.rgb(55, 100, 105)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1f);
        params.leftMargin = dp(3);
        row.addView(text, params);
        text.setClickable(true);
        text.setFocusable(true);
        text.setOnClickListener(v -> task.run());
        return text;
    }
    private void toggleRow(LinearLayout parent, String name, boolean selected, java.util.function.Consumer<Boolean> callback) {
        LinearLayout line = horizontal();
        addTop(parent, line, 12);
        line.addView(label(name, 13, WHITE, false), new LinearLayout.LayoutParams(0, -2, 1f));
        Switch toggle = new Switch(this);
        toggle.setContentDescription(name);
        toggle.setShowText(false);
        toggle.setChecked(selected);
        toggle.setThumbTintList(ColorStateList.valueOf(WHITE));
        toggle.setTrackTintList(new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{TEAL, Color.rgb(87, 109, 118)}));
        line.addView(toggle);
        toggle.setOnCheckedChangeListener((c, value) -> { callback.accept(value); refreshStatuses(); });
    }
    private void addNewFeaturePanels(LinearLayout root, LinearLayout reports) {
        addTop(root, miniHeading("KİŞİSEL KONTROL"), 25);

        // Feature 4: user-initiated intention, not an invasive always-on overlay.
        LinearLayout intentions = featureCard(root, "Neden açıyorsun?", "Niyetini uygulamadan seçebilir veya girişte sorulmasını açabilirsin. Mesaj içeriği tutulmaz, bloklama kuralı değişmez.");
        intentLabel = label("Seçim: " + settings.intentReason(), 12, TEAL, true);
        addTop(intentions, intentLabel, 10);
        LinearLayout intentButtons = horizontal(); addTop(intentions, intentButtons, 10);
        for (String why : new String[]{"Mesaj", "Video", "Paylaşım"})
            chip(why, intentButtons, () -> { settings.setIntentReason(why); refreshStatuses(); });
        toggleRow(intentions, "Hedef akışa girişte neden açtığını sor", settings.intentPromptEnabled(), settings::setIntentPromptEnabled);

        // Features 5/6: opt-in budget and graded penalties.
        LinearLayout budget = featureCard(root, "Günlük akış bütçesi", "Dört hedefin toplamı; sınır dolunca gece yarısına kadar mola. Varsayılan olarak kapalı.");
        toggleRow(budget, "Günlük bütçe aktif", settings.budgetEnabled(), settings::setBudgetEnabled);
        budgetLabel = label("", 12, TEAL, true); addTop(budget, budgetLabel, 10);
        LinearLayout budgetRow = horizontal(); addTop(budget, budgetRow, 9);
        for (int minutes : new int[]{15, 30, 60})
            chip(minutes + " dk", budgetRow, () -> { settings.setBudgetMinutes(minutes); refreshStatuses(); });
        toggleRow(budget, "Artan molalar (1× / 2× / 3×)", settings.escalate(), settings::setEscalate);
        toggleRow(budget, "İzleme limitinin %80'inde nazik uyarı", settings.warningEnabled(), settings::setWarningEnabled);

        // Feature 7: mode change and optional schedule presets.
        LinearLayout mode = featureCard(root, "Odak modları", "Ders: en fazla 20 sn. İş: en fazla 60 sn. Uyku: hedef akışlara geçiş engellenir. Serbest: normal ayarlar.");
        modeLabel = label("", 12, TEAL, true); addTop(mode, modeLabel, 8);
        LinearLayout modes = horizontal(); addTop(mode, modes, 10);
        String[] modeNames = {"Ders", "Uyku", "İş", "Serbest"};
        String[] modeIds = {"ders", "uyku", "is", "serbest"};
        for (int i = 0; i < modeIds.length; i++) {
            String id = modeIds[i];
            chip(modeNames[i], modes, () -> { settings.setMode(id); refreshStatuses(); });
        }
        toggleRow(mode, "Otomatik zamanlama (isteğe bağlı)", settings.scheduleEnabled(), settings::setScheduleEnabled);
        addTop(mode, label("Uyku: seçilen saat – 07.00 · Ders: seçilen saatten itibaren 3 saat", 11, MUTED, false), 9);
        LinearLayout modeTimes = horizontal(); addTop(mode, modeTimes, 8);
        chip("Uyku saati", modeTimes, () -> chooseHour(true));
        chip("Ders saati", modeTimes, () -> chooseHour(false));

        // Features 3/8: focus timer + credit bank + achievement progression.
        LinearLayout rewards = featureCard(root, "Zaman Bankası & başarımlar", "25 dk odak oturumu bitirince 5 dk banka kredisi ve 15 XP kazanırsın. Mola tamamlamak +10 XP.");
        xpLabel = label("", 19, WHITE, true); addTop(rewards, xpLabel, 11);
        badgeLabel = label("", 12, TEAL, false); addTop(rewards, badgeLabel, 6);
        focusLabel = label("", 13, WHITE, true); addTop(rewards, focusLabel, 12);
        bankLabel = label("", 12, MUTED, false); addTop(rewards, bankLabel, 5);
        LinearLayout focusButtons = horizontal(); addTop(rewards, focusButtons, 13);
        chip("15 dk", focusButtons, () -> startOrRestartFocus(15));
        chip("25 dk", focusButtons, () -> startOrRestartFocus(25));
        chip("İptal", focusButtons, () -> { settings.cancelFocus(); refreshStatuses(); });
        focusClaimButton = button("Oturumu tamamla ve ödülü al", Color.rgb(25, 107, 93));
        addTop(rewards, focusClaimButton, 11);
        focusClaimButton.setOnClickListener(v -> {
            boolean success = settings.claimFocus();
            if (success) new AlertDialog.Builder(this).setTitle("Odak tamamlandı")
                    .setMessage("Tebrikler! Zaman Bankası'na kredi ve XP eklendi.")
                    .setPositiveButton("Tamam", null).show();
            refreshStatuses();
        });

        // Feature 9: explicit exception or bank credit for exactly one selected target.
        LinearLayout exceptions = featureCard(root, "Acil geçiş ve istisnalar", "Günde 1 kez ücretsiz 5 dk istisna; ayrıca Zaman Bankası'ndan seçtiğin akışa 5 dk ayırabilirsin.");
        LinearLayout exceptionRow = horizontal(); addTop(exceptions, exceptionRow, 11);
        chip("Acil 5 dk", exceptionRow, () -> chooseException(false));
        chip("Bankadan 5 dk", exceptionRow, () -> chooseException(true));

        // Features 2 and 11: all figures use actual tracked screen exposure only.
        addTop(reports, miniHeading("ANALİZ VE RAPORLAR"), 27);
        LinearLayout analytics = featureCard(reports, "Son 7 günün kullanımı", "Yalnızca tanınan akışlarda geçen süre. Telefonun tamamının ekran süresi değildir.");
        weeklyGraph = label("", 12, WHITE, false); addTop(analytics, weeklyGraph, 13);
        reportLabel = label("", 12, TEAL, true); addTop(analytics, reportLabel, 12);
        Button share = button("Haftalık raporu paylaş", Color.rgb(27, 96, 108));
        addTop(analytics, share, 10);
        share.setOnClickListener(v -> {
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_TEXT, weeklyReportText());
            startActivity(Intent.createChooser(send, "AkışFren raporunu paylaş"));
        });

        // Feature 12: locally computed, evidence-threshold recommendation.
        LinearLayout suggestions = featureCard(reports, "Akıllı kullanım önerisi", "Öneriler son 7 günün saatlik kullanımına bakar; internete veri gönderilmez.");
        suggestionLabel = label("", 13, WHITE, false); addTop(suggestions, suggestionLabel, 12);
        addTop(suggestions, label("Öneriyi uygulamak için Odak Modları bölümünden zamanlama açıp ders saatini seçebilirsin.", 11, MUTED, false), 7);
        LinearLayout privacyTools = featureCard(reports, "Veriler senin kontrolünde", "Son 90 günlük tanınan akış sürelerini CSV olarak dışa aktar. Dosya sadece sen seçersen oluşturulur. Dilersen bütün AkışFren verilerini kaldırabilirsin.");
        Button export = button("90 günlük CSV raporunu kaydet", Color.rgb(27, 96, 108));
        addTop(privacyTools, export, 12);
        export.setOnClickListener(v -> exportData());
        Button erase = button("Tüm yerel verilerimi sil", Color.rgb(109, 51, 60));
        addTop(privacyTools, erase, 9);
        erase.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Tüm AkışFren verileri silinsin mi?")
                .setMessage("Kullanım geçmişi, XP, odak kredileri, ayarlar ve mola durumları kalıcı olarak silinecek. Geri alınamaz. Uygulama başlangıç ayarlarına döner. Erişilebilirlik izni açık kalabilir; Android Ayarlarından kapatabilirsin.")
                .setNegativeButton("İptal", null)
                .setPositiveButton("Tüm verileri sil", (d, w) -> {
                    if (settings.eraseAllLocalData()) {
                        buildScreen();
                        AkisFrenWidget.updateAll(this);
                        Toast.makeText(this, "Yerel kayıtlar silindi", Toast.LENGTH_LONG).show();
                    } else infoDialog("Silme işlemi tamamlanamadı.");
                }).show());
        AkisFrenWidget.updateAll(this);
    }

    private void exportData() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, "akisfren_90_gun_" + LocalDate.now() + ".csv");
        try { startActivityForResult(intent, REQUEST_CSV); }
        catch (Exception e) { infoDialog("CSV dosyası kaydedilecek uygulama bulunamadı."); }
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CSV || resultCode != RESULT_OK || data == null) return;
        Uri uri = data.getData();
        if (uri == null) return;
        try (OutputStream output = getContentResolver().openOutputStream(uri)) {
            if (output == null) throw new java.io.IOException("Dosya açılamadı");
            output.write(("\uFEFF" + settings.exportCsv(90)).getBytes(StandardCharsets.UTF_8));
            output.flush();
            Toast.makeText(this, "CSV kaydedildi", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            infoDialog("CSV kaydedilemedi. Farklı bir konum deneyebilirsin.");
        }
    }
    private void chooseHour(boolean sleep) {
        int count = sleep ? 24 : 21;
        String[] hours = new String[count];
        for (int i = 0; i < count; i++) hours[i] = String.format(Locale.getDefault(), "%02d.00", i);
        new AlertDialog.Builder(this).setTitle(sleep ? "Uyku modu başlama saati" : "Ders modu başlama saati")
                .setItems(hours, (dialog, index) -> {
                    if (sleep) settings.setSleepStartHour(index);
                    else settings.setStudyStartHour(index);
                    refreshStatuses();
                }).setNegativeButton("Vazgeç", null).show();
    }
    private void chooseException(boolean useBank) {
        if (useBank && settings.bankSeconds() < 300) { infoDialog("Bankada en az 5 dakika bulunmalı."); return; }
        if (!useBank && settings.usedEmergencyToday() >= 1) { infoDialog("Bugünkü acil istisna hakkı kullanıldı."); return; }
        new AlertDialog.Builder(this).setTitle(useBank ? "Bankayı hangi akışta kullanacaksın?" : "Acil istisna hangi akış için?")
                .setItems(AppSettings.TITLES, (dialog, which) -> {
                    String target = AppSettings.TARGETS[which];
                    boolean success = useBank ? settings.spendBank(target) : settings.applyEmergency(target);
                    infoDialog(success ? "Seçilen hedef için 5 dakikalık geçici erişim açıldı." : "İşlem gerçekleştirilemedi.");
                    refreshStatuses();
                }).setNegativeButton("Vazgeç", null).show();
    }
    private void infoDialog(String message) {
        new AlertDialog.Builder(this).setMessage(message).setPositiveButton("Tamam", null).show();
    }
    private void startOrRestartFocus(int minutes) {
        if (settings.focusRunning()) {
            new AlertDialog.Builder(this).setTitle("Odak oturumunu değiştir")
                    .setMessage("Mevcut oturum silinecek. Yeni bir " + minutes + " dakikalık oturum başlasın mı?")
                    .setNegativeButton("Vazgeç", null)
                    .setPositiveButton("Başlat", (d, w) -> { settings.startFocus(minutes); refreshStatuses(); }).show();
        } else { settings.startFocus(minutes); refreshStatuses(); }
    }
    private String weeklyReportText() {
        return "AkışFren • Son 7 gün: " + formatDuration(settings.periodSeconds(0,7))
                + " · Önceki 7 gün: " + formatDuration(settings.periodSeconds(7,7))
                + "\nBugünkü mola: " + settings.todayBreaks()
                + " · Toplam tamamlanan molalar: " + settings.completedBreaks()
                + "\nNot: Sadece uygulamanın tanıyabildiği akış süreleri dahil.";
    }
    private void refreshFeatures() {
        if (budgetLabel == null) return;
        budgetLabel.setText("Bugün: " + formatDuration(settings.todayTotalSeconds())
                + " / " + settings.budgetMinutes() + " dk"
                + (settings.budgetEnabled() ? (settings.budgetExceeded() ? " · BÜTÇE DOLDU" : "") : " · Kapalı"));
        modeLabel.setText("Etkin mod: " + settings.activeMode().toUpperCase(new Locale("tr", "TR"))
                + " · Uyku " + settings.sleepStartHour() + ".00 · Ders " + settings.studyStartHour() + ".00");
        intentLabel.setText("Seçim: " + settings.intentReason());
        xpLabel.setText("Seviye " + settings.level() + " · " + settings.xp() + " XP");
        badgeLabel.setText("Rozetler: " + (settings.completedBreaks() >= 1 ? "● İlk Mola  " : "○ İlk Mola  ")
                + (settings.focusSessions() >= 1 ? "● Odak Ustası  " : "○ Odak Ustası  ")
                + (settings.completionStreak() >= 3 ? "● 3 Gün Seri" : "○ 3 Gün Seri"));
        int balance = settings.bankSeconds() / 60;
        bankLabel.setText("Banka bakiyesi: " + balance + " dk · Tamamlanan odak: " + settings.focusSessions()
                + " · Mola serisi: " + settings.completionStreak() + " gün");
        if (!settings.focusRunning()) focusLabel.setText("Aktif odak oturumu yok");
        else if (settings.focusRemainingSeconds() < 0) focusLabel.setText("Cihaz yeniden başladı; oturumu iptal edip tekrar başlat.");
        else focusLabel.setText("Odak: " + formatDuration(settings.focusRemainingSeconds()) + " kaldı");
        focusClaimButton.setEnabled(settings.focusRunning() && settings.focusRemainingSeconds() == 0);
        int max = 1;
        for (int d = 0; d < 7; d++) max = Math.max(max, settings.dayTotal(LocalDate.now().minusDays(d).toString()));
        StringBuilder graph = new StringBuilder();
        for (int d = 6; d >= 0; d--) {
            LocalDate day = LocalDate.now().minusDays(d);
            int sec = settings.dayTotal(day.toString());
            int bars = sec == 0 ? 0 : Math.max(1, 10 * sec / max);
            graph.append(day.getDayOfMonth()).append('/').append(day.getMonthValue()).append("   ");
            for (int b = 0; b < bars; b++) graph.append('▰');
            for (int b = bars; b < 10; b++) graph.append('·');
            graph.append("  ").append(formatDuration(sec)).append('\n');
        }
        weeklyGraph.setText(graph.toString());
        int current = settings.periodSeconds(0,7), previous = settings.periodSeconds(7,7);
        reportLabel.setText("Son 7 gün " + formatDuration(current)
                + " · Önceki 7 gün " + formatDuration(previous)
                + (previous == 0 ? " · Karşılaştırma için önceki veri yok" : " · Fark " + formatDuration(Math.abs(current - previous))));
        suggestionLabel.setText(settings.peakHourSuggestion());
    }

    private String formatDuration(long secs) {
        if (secs < 60) return secs + " sn";
        if (secs % 60 == 0) return (secs / 60) + " dk";
        if (secs < 3600) return (secs / 60) + " dk " + (secs % 60) + " sn";
        return String.format(Locale.getDefault(), "%d sa %d dk", secs / 3600, (secs % 3600) / 60);
    }
    private boolean serviceEnabled() {
        AccessibilityManager manager = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        if (manager == null) return false;
        String id = new ComponentName(this, FocusMonitorService.class).flattenToString();
        List<AccessibilityServiceInfo> services = manager.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for (AccessibilityServiceInfo service : services)
            if (id.equals(service.getId())) return true;
        return false;
    }
    private void refreshStatuses() {
        if (serviceStatus == null || settings == null) return;
        boolean on = serviceEnabled();
        boolean paused = settings.isPaused();
        if (!on) {
            serviceStatus.setText("Koruma kapalı");
            serviceExplanation.setText("Başlamak için erişilebilirlik iznini aç.");
            serviceStatus.setTextColor(WHITE);
        } else if (paused) {
            long remaining = (settings.pausedUntil() - System.currentTimeMillis() + 999) / 1000;
            serviceStatus.setText("Koruma duraklatıldı");
            serviceExplanation.setText("Kalan: " + formatDuration(remaining));
            serviceStatus.setTextColor(RED);
        } else {
            serviceStatus.setText("Koruma aktif");
            serviceExplanation.setText("Seçtiğin akışlar izleniyor.");
            serviceStatus.setTextColor(GREEN);
        }
        pauseButton.setText(paused ? "Korumaya devam et" : "Korumayı geçici duraklat");
        settingsButton.setText(on ? "Erişilebilirlik iznini yönet" : "Koruma iznini aç");
        summaryUsage.setText(formatDuration(settings.todayTotalSeconds()));
        summaryBreaks.setText(String.valueOf(settings.todayBreaks()));
        refreshFeatures();
        long now = System.currentTimeMillis();
        String current = settings.activeTarget();
        for (int i = 0; i < AppSettings.TARGETS.length; i++) {
            String key = AppSettings.TARGETS[i];
            TextView status = statusViews.get(i);
            usageViews.get(i).setText("Bugün: " + formatDuration(settings.todaySeconds(key)));
            String daily = settings.targetBudgetEnabled(key)
                    ? (formatDuration(settings.todaySeconds(key)) + " / " + settings.targetBudgetMinutes(key) + " dk"
                    + (settings.targetBudgetExceeded(key) ? " · GÜNLÜK LİMİT DOLDU" : ""))
                    : "Günlük sınır kapalı";
            targetBudgetViews.get(i).setText(daily);
            if (!settings.enabled(key)) {
                status.setText("○ Kapalı"); status.setTextColor(MUTED);
            } else if (!on) {
                status.setText("○ İzin gerekli"); status.setTextColor(MUTED);
            } else if (paused) {
                status.setText("◷ Duraklatıldı"); status.setTextColor(RED);
            } else if (settings.effectiveBlockedUntil(key) > now) {
                long left = (settings.effectiveBlockedUntil(key) - now + 999) / 1000;
                status.setText("● Mola: " + formatDuration(left)); status.setTextColor(RED);
            } else if (key.equals(current) && settings.activeSince() > 0) {
                long secs = Math.max(0, (SystemClock.elapsedRealtime() - settings.activeSince()) / 1000);
                status.setText("● İzleniyor: " + formatDuration(secs)); status.setTextColor(GREEN);
            } else {
                status.setText("● Beklemede"); status.setTextColor(MUTED);
            }
            long viewed = (on && !paused && key.equals(current) && settings.activeSince() > 0)
                    ? Math.max(0L, (SystemClock.elapsedRealtime() - settings.activeSince()) / 1000) : 0;
            progressViews.get(i).setProgress((int) Math.min(100L, viewed * 100L / Math.max(1, settings.effectiveLimit(key))));
        }
    }
}
