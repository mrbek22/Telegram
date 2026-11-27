package org.telegram.ui.Components;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AlfaCyrillic;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LiteMode;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BottomSheet;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * AlfaGram Wrapped — oy yakuni: nechta xabar, eng ko'p yozishgan odamlar, faol soat va kun, sevimli emojilar.
 * Sonlar Telegram serveridan aniq olinadi (messages.search count — faqat sanash, hech narsa o'qilgan deb belgilanmaydi),
 * shuning uchun telefondagi kesh qancha bo'lishidan qat'i nazar to'g'ri chiqadi. Natija 2 soat keshlanadi.
 * Oyning 1–5 kunlari o'tgan oy, qolgan kunlarda joriy oy (hozirgacha) ko'rsatiladi.
 */
public class AlfaWrappedSheet extends BottomSheet {

    private static final String[] MONTHS = {"Yanvar", "Fevral", "Mart", "Aprel", "May", "Iyun",
            "Iyul", "Avgust", "Sentabr", "Oktabr", "Noyabr", "Dekabr"};
    private static final int BG_TOP = 0xFF4C1D95, BG_MID = 0xFF1D4ED8, BG_BOTTOM = 0xFF0B1B4D;
    private static final int ACCENT = 0xFF7DD3FC, GOLD = 0xFFFACC15;
    private static final int MAX_USERS = 30, MAX_GROUPS = 12, SAMPLE = 100;
    private static final long CACHE_MS = 2 * 60 * 60 * 1000L;

    public static class Data {
        public String month;
        public int sent, received, chats, scanned;
        public final int[] hours = new int[24];
        public final ArrayList<long[]> top = new ArrayList<>(); // {userId, count}
        public final ArrayList<String> emojis = new ArrayList<>();
        public int activeHour = -1;
        public long busiestDay; // ms, 0 — yo'q
        public int busiestDayCount;
        public boolean failed;
    }

    private final int account;
    private final LinearLayout content;
    private Data data;
    private PodiumView podium;
    private Loader loader;
    private TextView progressText;
    private View progressBar;
    private int cardIndex;

    public AlfaWrappedSheet(Context context, int account) {
        super(context, false);
        this.account = account;
        fixNavigationBar(BG_BOTTOM);
        ScrollView scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(18), dp(16), dp(16));
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{BG_TOP, BG_MID, BG_BOTTOM});
        bg.setCornerRadii(new float[]{dp(22), dp(22), dp(22), dp(22), 0, 0, 0, 0});
        content.setBackground(bg);
        scroll.addView(content);
        setCustomView(scroll);
        start(context, false);
    }

    private void start(Context context, boolean force) {
        Data cached = force ? null : readCache(account);
        if (cached != null) {
            data = cached;
            build(context);
            return;
        }
        buildLoading(context);
        loader = new Loader(account, percent -> {
            if (progressText != null) {
                progressText.setText(AlfaCyrillic.uiFormat("Xabarlaringiz sanalmoqda: %d%%", percent));
            }
            if (progressBar != null && progressBar.getParent() instanceof View) {
                int w = ((View) progressBar.getParent()).getWidth();
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) progressBar.getLayoutParams();
                lp.width = Math.max(dp(6), w * percent / 100);
                progressBar.setLayoutParams(lp);
            }
        }, d -> {
            loader = null;
            data = d;
            if (!d.failed) {
                writeCache(account, d);
            }
            build(context);
        });
        loader.start();
    }

    @Override
    public void dismiss() {
        if (loader != null) {
            loader.cancelled = true;
            loader = null;
        }
        super.dismiss();
    }

    // ---------------- ma'lumot ----------------

    /** Hisob davri: [from, to) soniyalarda + oy nomi. */
    private static long[] period(String[] monthOut) {
        Calendar c = Calendar.getInstance();
        int day = c.get(Calendar.DAY_OF_MONTH);
        long to = c.getTimeInMillis() / 1000;
        if (day <= 5) {
            c.add(Calendar.MONTH, -1);
        }
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long from = c.getTimeInMillis() / 1000;
        monthOut[0] = MONTHS[c.get(Calendar.MONTH)];
        if (day <= 5) {
            c.add(Calendar.MONTH, 1);
            to = c.getTimeInMillis() / 1000;
        }
        return new long[]{from, to};
    }

    /** Serverdan ketma-ket so'rovlar (har biri ~0,1 s oraliq bilan) — FLOOD_WAIT bo'lsa to'plangani bilan yakunlanadi. */
    private static class Loader {
        private static final int SENT = 0, RECEIVED = 1, GROUP_SENT = 2;
        final int account;
        final Utilities.Callback<Integer> onProgress;
        final Utilities.Callback<Data> onDone;
        final Data d = new Data();
        final ArrayList<long[]> tasks = new ArrayList<>(); // {dialogId, type}
        final HashMap<Long, Integer> perUser = new HashMap<>();
        final HashMap<String, Integer> emojiCount = new HashMap<>();
        final HashMap<Long, Integer> perDay = new HashMap<>();
        final Calendar cal = Calendar.getInstance();
        long from, to, self;
        int index, errors;
        volatile boolean cancelled;

        Loader(int account, Utilities.Callback<Integer> onProgress, Utilities.Callback<Data> onDone) {
            this.account = account;
            this.onProgress = onProgress;
            this.onDone = onDone;
        }

        void start() {
            String[] month = new String[1];
            long[] range = period(month);
            d.month = month[0];
            from = range[0];
            to = range[1];
            self = UserConfig.getInstance(account).getClientUserId();
            MessagesController mc = MessagesController.getInstance(account);
            ArrayList<TLRPC.Dialog> dialogs = new ArrayList<>(mc.getAllDialogs());
            Collections.sort(dialogs, (a, b) -> Integer.compare(b.last_message_date, a.last_message_date));
            int users = 0, groups = 0;
            for (TLRPC.Dialog dialog : dialogs) {
                if (dialog == null || dialog.last_message_date < from) {
                    continue;
                }
                long did = dialog.id;
                if (DialogObject.isUserDialog(did)) {
                    if (did == self || did == 777000 || users >= MAX_USERS) continue;
                    TLRPC.User u = mc.getUser(did);
                    if (u == null || u.bot || u.deleted || UserObject.isReplyUser(u)) continue;
                    tasks.add(new long[]{did, SENT});
                    tasks.add(new long[]{did, RECEIVED});
                    users++;
                } else if (DialogObject.isChatDialog(did)) {
                    if (groups >= MAX_GROUPS) continue;
                    TLRPC.Chat ch = mc.getChat(-did);
                    if (ch == null || ChatObject.isChannelAndNotMegaGroup(ch) || ChatObject.isNotInChat(ch)) continue;
                    tasks.add(new long[]{did, GROUP_SENT});
                    groups++;
                }
            }
            d.scanned = users + groups;
            next();
        }

        void next() {
            if (cancelled) {
                return;
            }
            if (index >= tasks.size()) {
                finish();
                return;
            }
            onProgress.run(tasks.isEmpty() ? 100 : index * 100 / tasks.size());
            final long[] task = tasks.get(index++);
            final long did = task[0];
            final int type = (int) task[1];
            MessagesController mc = MessagesController.getInstance(account);
            TLRPC.TL_messages_search req = new TLRPC.TL_messages_search();
            req.peer = mc.getInputPeer(did);
            if (req.peer == null) {
                AndroidUtilities.runOnUIThread(this::next);
                return;
            }
            req.q = "";
            req.filter = new TLRPC.TL_inputMessagesFilterEmpty();
            req.min_date = (int) from;
            req.max_date = (int) to;
            req.limit = type == RECEIVED ? 1 : SAMPLE;
            req.from_id = type == RECEIVED ? mc.getInputPeer(did) : new TLRPC.TL_inputPeerSelf();
            req.flags |= 1;
            ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                if (cancelled) {
                    return;
                }
                if (error != null) {
                    if (error.text != null && error.text.startsWith("FLOOD_WAIT")) {
                        finish(); // to'plangan ma'lumot bilan yakunlaymiz
                        return;
                    }
                    errors++;
                } else if (response instanceof TLRPC.messages_Messages) {
                    TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
                    int count = res instanceof TLRPC.TL_messages_messages ? res.messages.size() : Math.max(res.count, res.messages.size());
                    if (type == RECEIVED) {
                        d.received += count;
                        add(did, count);
                    } else {
                        d.sent += count;
                        if (type == SENT) {
                            add(did, count);
                        }
                        for (TLRPC.Message m : res.messages) {
                            sample(m);
                        }
                    }
                }
                AndroidUtilities.runOnUIThread(this::next, 90);
            }));
        }

        private void add(long did, int count) {
            Integer n = perUser.get(did);
            perUser.put(did, (n == null ? 0 : n) + count);
        }

        private void sample(TLRPC.Message m) {
            if (m == null || m.date <= 0) {
                return;
            }
            cal.setTimeInMillis(m.date * 1000L);
            d.hours[cal.get(Calendar.HOUR_OF_DAY)]++;
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long day = cal.getTimeInMillis();
            Integer n = perDay.get(day);
            perDay.put(day, (n == null ? 0 : n) + 1);
            if (!TextUtils.isEmpty(m.message)) {
                countEmojis(m.message, emojiCount);
            }
        }

        private void finish() {
            if (cancelled) {
                return;
            }
            cancelled = true;
            d.failed = d.sent == 0 && d.received == 0 && errors > 0;
            ArrayList<Map.Entry<Long, Integer>> people = new ArrayList<>();
            for (Map.Entry<Long, Integer> e : perUser.entrySet()) {
                if (e.getValue() > 0) people.add(e);
            }
            d.chats = people.size();
            Collections.sort(people, (a, b) -> b.getValue() - a.getValue());
            for (int i = 0; i < Math.min(3, people.size()); i++) {
                d.top.add(new long[]{people.get(i).getKey(), people.get(i).getValue()});
            }
            ArrayList<Map.Entry<String, Integer>> em = new ArrayList<>(emojiCount.entrySet());
            Collections.sort(em, (a, b) -> b.getValue() - a.getValue());
            for (int i = 0; i < Math.min(5, em.size()); i++) {
                d.emojis.add(em.get(i).getKey());
            }
            int best = -1;
            for (int h = 0; h < 24; h++) {
                if (d.hours[h] > 0 && (best < 0 || d.hours[h] > d.hours[best])) best = h;
            }
            d.activeHour = best;
            for (Map.Entry<Long, Integer> e : perDay.entrySet()) {
                if (e.getValue() > d.busiestDayCount) {
                    d.busiestDayCount = e.getValue();
                    d.busiestDay = e.getKey();
                }
            }
            onProgress.run(100);
            onDone.run(d);
        }
    }

    private static void countEmojis(String s, HashMap<String, Integer> out) {
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            int len = Character.charCount(cp);
            boolean emoji = (cp >= 0x1F300 && cp <= 0x1FAFF && !(cp >= 0x1F3FB && cp <= 0x1F3FF)) || (cp >= 0x2600 && cp <= 0x27BF);
            if (emoji) {
                String e = new String(Character.toChars(cp));
                Integer n = out.get(e);
                out.put(e, n == null ? 1 : n + 1);
            }
            i += len;
        }
    }

    // ---------------- kesh ----------------

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("alfa_wrapped", Context.MODE_PRIVATE);
    }

    private static String cacheKey(int account) {
        String[] m = new String[1];
        long[] r = period(m);
        return "w_" + UserConfig.getInstance(account).getClientUserId() + "_" + r[0];
    }

    private static Data readCache(int account) {
        try {
            String s = prefs().getString(cacheKey(account), null);
            if (s == null) return null;
            JSONObject o = new JSONObject(s);
            if (System.currentTimeMillis() - o.optLong("at") > CACHE_MS) return null;
            Data d = new Data();
            d.month = o.optString("month");
            d.sent = o.optInt("sent");
            d.received = o.optInt("received");
            d.chats = o.optInt("chats");
            d.scanned = o.optInt("scanned");
            d.activeHour = o.optInt("hour", -1);
            d.busiestDay = o.optLong("day");
            d.busiestDayCount = o.optInt("dayCount");
            JSONArray h = o.optJSONArray("hours");
            for (int i = 0; h != null && i < 24 && i < h.length(); i++) d.hours[i] = h.optInt(i);
            JSONArray t = o.optJSONArray("top");
            for (int i = 0; t != null && i < t.length(); i++) {
                JSONArray p = t.optJSONArray(i);
                if (p != null) d.top.add(new long[]{p.optLong(0), p.optLong(1)});
            }
            JSONArray e = o.optJSONArray("emojis");
            for (int i = 0; e != null && i < e.length(); i++) d.emojis.add(e.optString(i));
            return d;
        } catch (Throwable e) {
            return null;
        }
    }

    private static void writeCache(int account, Data d) {
        try {
            JSONObject o = new JSONObject();
            o.put("at", System.currentTimeMillis());
            o.put("month", d.month);
            o.put("sent", d.sent);
            o.put("received", d.received);
            o.put("chats", d.chats);
            o.put("scanned", d.scanned);
            o.put("hour", d.activeHour);
            o.put("day", d.busiestDay);
            o.put("dayCount", d.busiestDayCount);
            JSONArray h = new JSONArray();
            for (int v : d.hours) h.put(v);
            o.put("hours", h);
            JSONArray t = new JSONArray();
            for (long[] p : d.top) t.put(new JSONArray().put(p[0]).put(p[1]));
            o.put("top", t);
            JSONArray e = new JSONArray();
            for (String s : d.emojis) e.put(s);
            o.put("emojis", e);
            prefs().edit().putString(cacheKey(account), o.toString()).apply();
        } catch (Throwable ignore) {
        }
    }

    // ---------------- ko'rinish ----------------

    private static TextView text(Context ctx, CharSequence s, int sp, boolean bold, int color) {
        TextView t = new TextView(ctx);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, sp);
        t.setTextColor(color);
        if (bold) {
            t.setTypeface(AndroidUtilities.bold());
        }
        return t;
    }

    /** Telegram'ning animatsiyali emojisi (qimirlaydi); to'plam yuklanmagan bo'lsa — oddiy rangli emoji. */
    private View emojiView(Context ctx, String emoji, int sizeDp) {
        try {
            TLRPC.Document doc = MediaDataController.getInstance(account).getEmojiAnimatedSticker(emoji);
            if (doc != null) {
                BackupImageView v = new BackupImageView(ctx);
                boolean animate = LiteMode.isEnabled(LiteMode.FLAGS_ANIMATED_STICKERS);
                String filter = sizeDp + "_" + sizeDp + (animate ? "" : "_firstframe");
                v.getImageReceiver().setAutoRepeat(1);
                // animatsiya yuklanguncha oddiy rangli emoji turadi (bo'sh doira emas)
                android.graphics.drawable.Drawable thumb = null;
                try {
                    thumb = Emoji.getEmojiBigDrawable(emoji);
                } catch (Throwable ignore) {
                }
                v.setImage(ImageLocation.getForDocument(doc), filter, "tgs", thumb, doc);
                return v;
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
        TextView t = new TextView(ctx);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, sizeDp * 0.72f);
        t.setGravity(Gravity.CENTER);
        t.setText(Emoji.replaceEmoji(emoji, t.getPaint().getFontMetricsInt(), false));
        return t;
    }

    private LinearLayout card(Context ctx) {
        LinearLayout l = new LinearLayout(ctx);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(18), dp(16), dp(18), dp(16));
        GradientDrawable g = new GradientDrawable();
        g.setColor(0x22FFFFFF);
        g.setStroke(dp(1), 0x1AFFFFFF);
        g.setCornerRadius(dp(20));
        l.setBackground(g);
        content.addView(l, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));
        appear(l);
        return l;
    }

    /** Kartalar navbat bilan pastdan suzib chiqadi. */
    private void appear(View v) {
        int i = cardIndex++;
        if (!LiteMode.isEnabled(LiteMode.FLAGS_CHAT)) {
            return;
        }
        v.setAlpha(0f);
        v.setTranslationY(dp(28));
        v.animate().alpha(1f).translationY(0).setStartDelay(60L + i * 90L).setDuration(380)
                .setInterpolator(new DecelerateInterpolator(1.6f)).start();
    }

    /** Sarlavhali qator: chapda animatsiyali emoji, o'ngda matn. */
    private LinearLayout titleRow(Context ctx, LinearLayout parent, String emoji, CharSequence title) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(emojiView(ctx, emoji, 30), LayoutHelper.createLinear(30, 30, 0, 0, 10, 0));
        row.addView(text(ctx, title, 16, true, 0xFFFFFFFF));
        parent.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));
        return row;
    }

    private void header(Context ctx, String emoji, CharSequence title, CharSequence subtitle) {
        LinearLayout h = new LinearLayout(ctx);
        h.setOrientation(LinearLayout.VERTICAL);
        h.setGravity(Gravity.CENTER_HORIZONTAL);
        h.addView(emojiView(ctx, emoji, 84), LayoutHelper.createLinear(84, 84, Gravity.CENTER_HORIZONTAL));
        TextView t = text(ctx, title, 26, true, 0xFFFFFFFF);
        t.setGravity(Gravity.CENTER);
        h.addView(t, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 8, 0, 0));
        if (subtitle != null) {
            TextView s = text(ctx, subtitle, 13, false, 0xB3FFFFFF);
            s.setGravity(Gravity.CENTER);
            h.addView(s, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 4, 0, 0));
        }
        content.addView(h, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));
        appear(h);
    }

    private void buildLoading(Context ctx) {
        content.removeAllViews();
        cardIndex = 0;
        String[] m = new String[1];
        period(m);
        header(ctx, "⏳", AlfaCyrillic.uiFormat("%s yakuni", AlfaCyrillic.ui(m[0])), AlfaCyrillic.ui("Hisoblanmoqda…"));
        LinearLayout c = card(ctx);
        progressText = text(ctx, AlfaCyrillic.uiFormat("Xabarlaringiz sanalmoqda: %d%%", 0), 15, true, 0xFFFFFFFF);
        c.addView(progressText);
        FrameLayout track = new FrameLayout(ctx);
        GradientDrawable tb = new GradientDrawable();
        tb.setColor(0x26FFFFFF);
        tb.setCornerRadius(dp(4));
        track.setBackground(tb);
        progressBar = new View(ctx);
        GradientDrawable pb = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{ACCENT, GOLD});
        pb.setCornerRadius(dp(4));
        progressBar.setBackground(pb);
        track.addView(progressBar, new FrameLayout.LayoutParams(dp(6), dp(8)));
        c.addView(track, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 8, 0, 12, 0, 4));
        content.addView(new View(ctx), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 160));
    }

    private void build(Context ctx) {
        content.removeAllViews();
        cardIndex = 0;
        progressText = null;
        progressBar = null;
        Data d = data;
        header(ctx, "🎉", AlfaCyrillic.uiFormat("%s yakuni", AlfaCyrillic.ui(d.month)),
                AlfaCyrillic.ui("Telegram serveridagi xabarlaringiz bo'yicha"));

        if (d.failed) {
            LinearLayout c = card(ctx);
            titleRow(ctx, c, "😔", AlfaCyrillic.ui("Hisoblab bo'lmadi. Internetni tekshirib, qayta urinib ko'ring."));
            addButton(ctx, AlfaCyrillic.ui("Qayta urinish"), v -> start(ctx, true));
            return;
        }
        if (d.sent == 0 && d.received == 0) {
            LinearLayout c = card(ctx);
            titleRow(ctx, c, "📭", AlfaCyrillic.ui("Bu oy uchun hali ma'lumot yo'q. Ko'proq yozishing — oy oxirida yakun shu yerda chiqadi."));
            return;
        }

        // asosiy son — sanab chiqadigan animatsiya
        LinearLayout hero = card(ctx);
        LinearLayout heroRow = new LinearLayout(ctx);
        heroRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout heroText = new LinearLayout(ctx);
        heroText.setOrientation(LinearLayout.VERTICAL);
        TextView big = text(ctx, "0", 52, true, ACCENT);
        countUp(big, d.sent);
        heroText.addView(big);
        heroText.addView(text(ctx, AlfaCyrillic.ui("ta xabar yubordingiz"), 16, true, 0xFFFFFFFF));
        heroRow.addView(heroText, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));
        heroRow.addView(emojiView(ctx, "✍", 72), LayoutHelper.createLinear(72, 72));
        hero.addView(heroRow);
        hero.addView(text(ctx, AlfaCyrillic.uiFormat("%d ta xabar oldingiz · %d kishi bilan yozishdingiz", d.received, d.chats), 13, false, 0xCCFFFFFF),
                LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 0));

        if (!d.top.isEmpty()) {
            LinearLayout c = card(ctx);
            titleRow(ctx, c, "🏆", AlfaCyrillic.ui("Eng ko'p yozishganlar"));
            podium = new PodiumView(ctx, account, d.top, true);
            c.addView(podium, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, PodiumView.HEIGHT_DP));
        }

        if (d.activeHour >= 0) {
            LinearLayout c = card(ctx);
            titleRow(ctx, c, "⏰", AlfaCyrillic.ui("Eng faol soatingiz"));
            c.addView(text(ctx, String.format("%02d:00 – %02d:00", d.activeHour, (d.activeHour + 1) % 24), 30, true, ACCENT),
                    LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));
            c.addView(new HoursView(ctx, d.hours, d.activeHour), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 70));
        }

        if (d.busiestDay > 0 && d.busiestDayCount > 1) {
            LinearLayout c = card(ctx);
            titleRow(ctx, c, "🔥", AlfaCyrillic.ui("Eng faol kuningiz"));
            String day;
            try {
                day = LocaleController.getInstance().getFormatterDayMonth().format(d.busiestDay);
            } catch (Throwable e) {
                day = "";
            }
            c.addView(text(ctx, day, 26, true, GOLD));
            c.addView(text(ctx, AlfaCyrillic.uiFormat("%s ta xabar", String.valueOf(d.busiestDayCount)), 13, false, 0xCCFFFFFF),
                    LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
        }

        if (!d.emojis.isEmpty()) {
            LinearLayout c = card(ctx);
            titleRow(ctx, c, "😍", AlfaCyrillic.ui("Sevimli emojilaringiz"));
            LinearLayout row = new LinearLayout(ctx);
            row.setGravity(Gravity.CENTER_VERTICAL);
            for (String e : d.emojis) {
                row.addView(emojiView(ctx, e, 48), LayoutHelper.createLinear(48, 48, 0, 0, 10, 0));
            }
            c.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 0));
        }

        addButton(ctx, AlfaCyrillic.ui("Ulashish"), v -> shareImage());
        TextView note = text(ctx, AlfaCyrillic.uiFormat("%d ta faol chat tekshirildi · natija faqat telefoningizda", d.scanned), 12, false, 0x99FFFFFF);
        note.setGravity(Gravity.CENTER);
        content.addView(note, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 10, 0, 0));
    }

    private void addButton(Context ctx, CharSequence label, View.OnClickListener l) {
        TextView b = text(ctx, label, 16, true, BG_BOTTOM);
        b.setGravity(Gravity.CENTER);
        GradientDrawable sb = new GradientDrawable();
        sb.setColor(0xFFFFFFFF);
        sb.setCornerRadius(dp(16));
        b.setBackground(sb);
        b.setOnClickListener(l);
        content.addView(b, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 52, 0, 6, 0, 0));
        appear(b);
    }

    private static void countUp(TextView v, int target) {
        if (target <= 0 || !LiteMode.isEnabled(LiteMode.FLAGS_CHAT)) {
            v.setText(String.valueOf(Math.max(0, target)));
            return;
        }
        ValueAnimator a = ValueAnimator.ofInt(0, target);
        a.setDuration(Math.min(1400, 500 + target / 4));
        a.setStartDelay(250);
        a.setInterpolator(new DecelerateInterpolator(1.8f));
        a.addUpdateListener(an -> v.setText(String.valueOf((int) an.getAnimatedValue())));
        a.start();
    }

    /**
     * Shohsupa: o'rtada 1-o'rin (oltin, toj), chapda 2-o'rin (kumush), o'ngda 3-o'rin (bronza).
     * Ilova ichida ham, ulashiladigan rasmda ham shu View chiziladi. Pog'onalar pastdan o'sib chiqadi.
     */
    private static class PodiumView extends View {
        static final int HEIGHT_DP = 262;
        private static final int[] RING = {0xFFFACC15, 0xFFCBD5E1, 0xFFD97706};
        private static final int[][] STEP = {{0xFFFDE68A, 0xFFEAB308}, {0xFFE2E8F0, 0xFF94A3B8}, {0xFFFBBF77, 0xFFC2410C}};
        private static final int[] STEP_TEXT = {0xFF713F12, 0xFF334155, 0xFF431407};
        private static final int[] STEP_H = {100, 70, 50};
        private static final int[] AVATAR = {80, 62, 56};

        private final int n;
        private final String[] names = new String[3];
        private final long[] counts = new long[3];
        private final ImageReceiver[] avatars = new ImageReceiver[3];
        private final boolean showCounts;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final android.text.TextPaint tp = new android.text.TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();
        private float grow = 1f;

        PodiumView(Context ctx, int account, ArrayList<long[]> top, boolean showCounts) {
            super(ctx);
            this.showCounts = showCounts;
            n = Math.min(3, top.size());
            MessagesController mc = MessagesController.getInstance(account);
            for (int i = 0; i < n; i++) {
                TLRPC.User u = mc.getUser(top.get(i)[0]);
                names[i] = u != null ? UserObject.getForcedFirstName(u) : "—";
                counts[i] = top.get(i)[1];
                avatars[i] = new ImageReceiver(this);
                avatars[i].setRoundRadius(dp(AVATAR[i]) / 2);
                if (u != null) {
                    avatars[i].setForUserOrChat(u, new AvatarDrawable(u));
                }
            }
            if (LiteMode.isEnabled(LiteMode.FLAGS_CHAT)) {
                grow = 0f;
                ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
                a.setStartDelay(450);
                a.setDuration(700);
                a.setInterpolator(new DecelerateInterpolator(2f));
                a.addUpdateListener(an -> {
                    grow = (float) an.getAnimatedValue();
                    invalidate();
                });
                a.start();
            }
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            for (int i = 0; i < n; i++) avatars[i].onAttachedToWindow();
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            for (int i = 0; i < n; i++) avatars[i].onDetachedFromWindow();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            final float colW = Math.min(dp(112), (getWidth() - dp(20)) / 3f);
            final float gap = dp(10);
            final float left = (getWidth() - colW * 3 - gap * 2) / 2f;
            final float bottom = getHeight();
            final int[] slotOf = {1, 0, 2};
            for (int rank = 0; rank < n; rank++) {
                float x = left + slotOf[rank] * (colW + gap);
                float cx = x + colW / 2f;
                float stepTop = bottom - dp(STEP_H[rank]) * grow;
                r.set(x, stepTop, x + colW, bottom);
                p.setShader(new LinearGradient(0, stepTop, 0, bottom, STEP[rank][0], STEP[rank][1], Shader.TileMode.CLAMP));
                canvas.drawRoundRect(r, dp(12), dp(12), p);
                p.setShader(null);
                if (grow > 0.6f) {
                    tp.setTypeface(AndroidUtilities.bold());
                    tp.setTextAlign(Paint.Align.CENTER);
                    tp.setTextSize(dp(22));
                    tp.setColor(STEP_TEXT[rank]);
                    tp.setAlpha((int) (255 * Math.min(1f, (grow - 0.6f) / 0.4f)));
                    canvas.drawText(String.valueOf(rank + 1), cx, stepTop + dp(28), tp);
                    tp.setAlpha(255);
                }
                float y = stepTop - dp(8);
                tp.setTextAlign(Paint.Align.CENTER);
                if (showCounts) {
                    tp.setTypeface(Typeface.DEFAULT);
                    tp.setTextSize(dp(11.5f));
                    tp.setColor(ACCENT);
                    canvas.drawText(AlfaCyrillic.uiFormat("%d ta", counts[rank]), cx, y, tp);
                    y -= dp(16);
                }
                tp.setTypeface(AndroidUtilities.bold());
                tp.setTextSize(dp(13));
                tp.setColor(0xFFFFFFFF);
                CharSequence nm = TextUtils.ellipsize(names[rank], tp, colW - dp(4), TextUtils.TruncateAt.END);
                canvas.drawText(nm, 0, nm.length(), cx, y, tp);
                float size = dp(AVATAR[rank]);
                float acy = y - dp(16) - size / 2f;
                p.setColor(RING[rank]);
                if (rank == 0) {
                    p.setShadowLayer(dp(12), 0, 0, 0x88FACC15);
                }
                canvas.drawCircle(cx, acy, size / 2f + dp(rank == 0 ? 4 : 3), p);
                p.clearShadowLayer();
                avatars[rank].setImageCoords(cx - size / 2f, acy - size / 2f, size, size);
                avatars[rank].draw(canvas);
                if (rank == 0) {
                    tp.setTextSize(dp(24));
                    canvas.drawText("👑", cx, acy - size / 2f - dp(4), tp);
                }
            }
        }
    }

    private static class HoursView extends View {
        private final int[] hours;
        private final int best;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();
        private float grow = 1f;

        HoursView(Context c, int[] hours, int best) {
            super(c);
            this.hours = hours;
            this.best = best;
            if (LiteMode.isEnabled(LiteMode.FLAGS_CHAT)) {
                grow = 0f;
                ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
                a.setStartDelay(500);
                a.setDuration(650);
                a.setInterpolator(new DecelerateInterpolator(2f));
                a.addUpdateListener(an -> {
                    grow = (float) an.getAnimatedValue();
                    invalidate();
                });
                a.start();
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int mx = 1;
            for (int h : hours) mx = Math.max(mx, h);
            float w = getWidth() / 24f;
            for (int i = 0; i < 24; i++) {
                float bh = Math.max(dp(2), (getHeight() - dp(4)) * hours[i] / (float) mx * grow);
                r.set(i * w + dp(1.5f), getHeight() - bh, (i + 1) * w - dp(1.5f), getHeight());
                p.setColor(i == best ? ACCENT : 0x55FFFFFF);
                canvas.drawRoundRect(r, dp(2), dp(2), p);
            }
        }
    }

    // ---------------- ulashish uchun rasm ----------------

    private void shareImage() {
        Activity activity = AndroidUtilities.findActivity(getContext());
        if (activity == null || data == null) {
            return;
        }
        try {
            Bitmap bmp = renderCard(data);
            File dir = new File(ApplicationLoader.applicationContext.getFilesDir(), "cache");
            dir.mkdirs();
            File f = new File(dir, "alfagram_wrapped.png");
            FileOutputStream os = new FileOutputStream(f);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
            os.close();
            bmp.recycle();
            Uri uri = FileProvider.getUriForFile(activity, ApplicationLoader.getApplicationId() + ".provider", f);
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("image/png");
            i.putExtra(Intent.EXTRA_STREAM, uri);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(Intent.createChooser(i, AlfaCyrillic.ui("Ulashish")));
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private Bitmap renderCard(Data d) {
        final int W = 1080, H = 1920;
        Bitmap b = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint bg = new Paint();
        bg.setShader(new LinearGradient(0, 0, W * 0.3f, H, new int[]{BG_TOP, BG_MID, BG_BOTTOM}, null, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, W, H, bg);
        Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
        glow.setColor(0x14FFFFFF);
        c.drawCircle(W - 120, 260, 340, glow);
        c.drawCircle(140, H - 220, 260, glow);

        Paint t = new Paint(Paint.ANTI_ALIAS_FLAG);
        t.setColor(0xFFFFFFFF);
        Typeface bold = Typeface.create(Typeface.DEFAULT, Typeface.BOLD);
        float y = 210;
        t.setTypeface(bold);
        t.setTextSize(88);
        c.drawText(AlfaCyrillic.uiFormat("%s yakuni", AlfaCyrillic.ui(d.month)), 90, y, t);
        y += 70;
        t.setTypeface(Typeface.DEFAULT);
        t.setTextSize(40);
        t.setColor(0xB3FFFFFF);
        c.drawText("AlfaGram Wrapped", 90, y, t);

        y += 230;
        t.setTypeface(bold);
        t.setTextSize(200);
        t.setColor(ACCENT);
        c.drawText(String.valueOf(d.sent), 84, y, t);
        y += 80;
        t.setTextSize(54);
        t.setColor(0xFFFFFFFF);
        c.drawText(AlfaCyrillic.ui("ta xabar yubordim"), 90, y, t);

        if (podium != null && podium.getWidth() > 0 && podium.getHeight() > 0) {
            y += 140;
            t.setTextSize(48);
            c.drawText(AlfaCyrillic.ui("Eng ko'p yozishganlarim"), 90, y, t);
            float s = 820f / podium.getWidth();
            y += 40;
            c.save();
            c.translate((W - 820) / 2f, y);
            c.scale(s, s);
            podium.draw(c);
            c.restore();
            y += podium.getHeight() * s;
        }
        y += 120;
        if (d.activeHour >= 0) {
            t.setTypeface(Typeface.DEFAULT);
            t.setTextSize(40);
            t.setColor(0xB3FFFFFF);
            c.drawText(AlfaCyrillic.ui("Faol soatim"), 90, y, t);
            t.setTypeface(bold);
            t.setTextSize(90);
            t.setColor(ACCENT);
            c.drawText(String.format("%02d:00", d.activeHour), 90, y + 100, t);
        }
        if (!d.emojis.isEmpty()) {
            t.setTextAlign(Paint.Align.RIGHT);
            t.setTypeface(Typeface.DEFAULT);
            t.setTextSize(40);
            t.setColor(0xB3FFFFFF);
            c.drawText(AlfaCyrillic.ui("Sevimli emojilarim"), W - 90, y, t);
            t.setTextSize(80);
            t.setColor(0xFFFFFFFF);
            c.drawText(TextUtils.join(" ", d.emojis.subList(0, Math.min(3, d.emojis.size()))), W - 90, y + 100, t);
            t.setTextAlign(Paint.Align.LEFT);
        }
        t.setTypeface(bold);
        t.setTextSize(42);
        t.setColor(0xCCFFFFFF);
        t.setTextAlign(Paint.Align.CENTER);
        c.drawText("AlfaGram · alfagram.uz", W / 2f, H - 110, t);
        return b;
    }
}
