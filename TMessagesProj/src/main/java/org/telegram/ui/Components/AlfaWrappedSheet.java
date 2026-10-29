package org.telegram.ui.Components;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.FileProvider;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.messenger.AlfaCyrillic;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.NativeByteBuffer;
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
 * AlfaGram Wrapped — oy yakuni: nechta xabar, eng ko'p yozishgan odamlar, faol soat, sevimli emojilar.
 * Ma'lumot faqat shu qurilmadagi xabarlar bazasidan olinadi (serverga hech narsa yuborilmaydi).
 * Oyning 1–5 kunlari o'tgan oy, qolgan kunlarda joriy oy (hozirgacha) ko'rsatiladi.
 */
public class AlfaWrappedSheet extends BottomSheet {

    private static final String[] MONTHS = {"Yanvar", "Fevral", "Mart", "Aprel", "May", "Iyun",
            "Iyul", "Avgust", "Sentabr", "Oktabr", "Noyabr", "Dekabr"};
    private static final int BG1 = 0xFF0B1B4D, BG2 = 0xFF1D4ED8, ACCENT = 0xFF7DD3FC;

    public static class Data {
        public String month;
        public int sent, received, chats;
        public final int[] hours = new int[24];
        public final ArrayList<long[]> top = new ArrayList<>(); // {userId, count}
        public final ArrayList<String> emojis = new ArrayList<>();
        public int activeHour = -1;
    }

    private final int account;
    private final LinearLayout content;
    private Data data;

    public AlfaWrappedSheet(Context context, int account) {
        super(context, false);
        this.account = account;
        fixNavigationBar(BG1);
        ScrollView scroll = new ScrollView(context);
        content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(20), dp(16), dp(16));
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{BG2, BG1});
        bg.setCornerRadii(new float[]{dp(18), dp(18), dp(18), dp(18), 0, 0, 0, 0});
        content.setBackground(bg);
        scroll.addView(content);
        setCustomView(scroll);
        TextView loading = text(context, AlfaCyrillic.ui("Hisoblanmoqda…"), 16, false, 0xFFFFFFFF);
        loading.setGravity(Gravity.CENTER);
        content.addView(loading, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 200));
        load(account, d -> {
            data = d;
            build(context);
        });
    }

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

    public static void load(int account, Utilities.Callback<Data> done) {
        final Data d = new Data();
        final String[] month = new String[1];
        final long[] range = period(month);
        d.month = month[0];
        final long selfId = UserConfig.getInstance(account).getClientUserId();
        MessagesStorage storage = MessagesStorage.getInstance(account);
        storage.getStorageQueue().postRunnable(() -> {
            HashMap<Long, Integer> perUser = new HashMap<>();
            HashMap<String, Integer> emojiCount = new HashMap<>();
            Calendar cal = Calendar.getInstance();
            int parsed = 0;
            SQLiteCursor cursor = null;
            try {
                cursor = storage.getDatabase().queryFinalized(
                        "SELECT out, uid, date, data FROM messages_v2 WHERE uid > 0 AND date >= ? AND date < ?",
                        range[0], range[1]);
                while (cursor.next()) {
                    long uid = cursor.longValue(1);
                    if (uid == selfId || uid == 777000) {
                        continue;
                    }
                    boolean out = cursor.intValue(0) != 0;
                    if (!out) {
                        d.received++;
                        continue;
                    }
                    d.sent++;
                    Integer n = perUser.get(uid);
                    perUser.put(uid, n == null ? 1 : n + 1);
                    cal.setTimeInMillis(cursor.intValue(2) * 1000L);
                    d.hours[cal.get(Calendar.HOUR_OF_DAY)]++;
                    if (parsed < 6000) {
                        NativeByteBuffer buf = cursor.byteBufferValue(3);
                        if (buf != null) {
                            parsed++;
                            try {
                                TLRPC.Message m = TLRPC.Message.TLdeserialize(buf, buf.readInt32(false), false);
                                if (m != null && !TextUtils.isEmpty(m.message)) {
                                    countEmojis(m.message, emojiCount);
                                }
                            } catch (Throwable ignore) {
                            }
                            buf.reuse();
                        }
                    }
                }
            } catch (Throwable e) {
                FileLog.e(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
            }
            d.chats = perUser.size();
            ArrayList<Map.Entry<Long, Integer>> people = new ArrayList<>(perUser.entrySet());
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
                if (d.hours[h] > 0 && (best < 0 || d.hours[h] > d.hours[best])) {
                    best = h;
                }
            }
            d.activeHour = best;
            AndroidUtilities.runOnUIThread(() -> done.run(d));
        });
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

    private LinearLayout card(Context ctx) {
        LinearLayout l = new LinearLayout(ctx);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(18), dp(16), dp(18), dp(16));
        GradientDrawable g = new GradientDrawable();
        g.setColor(0x1FFFFFFF);
        g.setCornerRadius(dp(18));
        l.setBackground(g);
        content.addView(l, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));
        return l;
    }

    private void build(Context ctx) {
        content.removeAllViews();
        Data d = data;
        content.addView(text(ctx, AlfaCyrillic.ui(d.month + " yakuni"), 26, true, 0xFFFFFFFF));
        content.addView(text(ctx, AlfaCyrillic.ui("AlfaGram Wrapped · shu qurilmadagi xabarlar bo'yicha"), 13, false, 0xB3FFFFFF),
                LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 16));

        if (d.sent == 0 && d.received == 0) {
            LinearLayout c = card(ctx);
            c.addView(text(ctx, AlfaCyrillic.ui("Bu oy uchun hali ma'lumot yo'q. Ko'proq yozishing — oy oxirida yakun shu yerda chiqadi."), 15, false, 0xFFFFFFFF));
            return;
        }

        LinearLayout hero = card(ctx);
        hero.addView(text(ctx, String.valueOf(d.sent), 48, true, ACCENT));
        hero.addView(text(ctx, AlfaCyrillic.ui("ta xabar yubordingiz"), 16, true, 0xFFFFFFFF));
        hero.addView(text(ctx, AlfaCyrillic.ui(d.received + " ta xabar oldingiz · " + d.chats + " kishi bilan yozishdingiz"), 13, false, 0xB3FFFFFF),
                LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 6, 0, 0));

        if (!d.top.isEmpty()) {
            LinearLayout c = card(ctx);
            c.addView(text(ctx, AlfaCyrillic.ui("Eng ko'p yozishganlar"), 15, true, 0xFFFFFFFF),
                    LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));
            MessagesController mc = MessagesController.getInstance(account);
            for (int i = 0; i < d.top.size(); i++) {
                TLRPC.User u = mc.getUser(d.top.get(i)[0]);
                FrameLayout row = new FrameLayout(ctx);
                BackupImageView av = new BackupImageView(ctx);
                av.setRoundRadius(dp(22));
                if (u != null) {
                    av.setForUserOrChat(u, new AvatarDrawable(u));
                }
                row.addView(av, LayoutHelper.createFrame(44, 44, Gravity.LEFT | Gravity.CENTER_VERTICAL));
                TextView name = text(ctx, (i + 1) + ". " + (u != null ? UserObject.getUserName(u) : "—"), 15, true, 0xFFFFFFFF);
                name.setSingleLine(true);
                name.setEllipsize(TextUtils.TruncateAt.END);
                row.addView(name, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL, 56, 0, 90, 0));
                TextView cnt = text(ctx, AlfaCyrillic.ui(d.top.get(i)[1] + " ta"), 14, false, ACCENT);
                row.addView(cnt, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.RIGHT | Gravity.CENTER_VERTICAL));
                c.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 54));
            }
        }

        if (d.activeHour >= 0) {
            LinearLayout c = card(ctx);
            c.addView(text(ctx, AlfaCyrillic.ui("Eng faol soatingiz"), 15, true, 0xFFFFFFFF));
            c.addView(text(ctx, String.format("%02d:00 – %02d:00", d.activeHour, (d.activeHour + 1) % 24), 30, true, ACCENT),
                    LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 10));
            c.addView(new HoursView(ctx, d.hours, d.activeHour), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 70));
        }

        if (!d.emojis.isEmpty()) {
            LinearLayout c = card(ctx);
            c.addView(text(ctx, AlfaCyrillic.ui("Sevimli emojilaringiz"), 15, true, 0xFFFFFFFF));
            c.addView(text(ctx, TextUtils.join("  ", d.emojis), 34, false, 0xFFFFFFFF),
                    LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 0));
        }

        TextView share = text(ctx, AlfaCyrillic.ui("Ulashish"), 16, true, 0xFF0B1B4D);
        share.setGravity(Gravity.CENTER);
        GradientDrawable sb = new GradientDrawable();
        sb.setColor(0xFFFFFFFF);
        sb.setCornerRadius(dp(14));
        share.setBackground(sb);
        share.setOnClickListener(v -> shareImage());
        content.addView(share, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 52, 0, 6, 0, 0));
    }

    private static class HoursView extends View {
        private final int[] hours;
        private final int best;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();

        HoursView(Context c, int[] hours, int best) {
            super(c);
            this.hours = hours;
            this.best = best;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int mx = 1;
            for (int h : hours) mx = Math.max(mx, h);
            float w = getWidth() / 24f;
            for (int i = 0; i < 24; i++) {
                float bh = Math.max(dp(2), (getHeight() - dp(4)) * hours[i] / (float) mx);
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
        bg.setShader(new LinearGradient(0, 0, W, H, BG2, BG1, Shader.TileMode.CLAMP));
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
        c.drawText(AlfaCyrillic.ui(d.month + " yakuni"), 90, y, t);
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

        if (!d.top.isEmpty()) {
            y += 150;
            t.setTextSize(46);
            c.drawText(AlfaCyrillic.ui("Eng ko'p yozishganlarim"), 90, y, t);
            MessagesController mc = MessagesController.getInstance(account);
            t.setTypeface(Typeface.DEFAULT);
            for (int i = 0; i < d.top.size(); i++) {
                TLRPC.User u = mc.getUser(d.top.get(i)[0]);
                String name = u != null ? UserObject.getForcedFirstName(u) : "—";
                y += 80;
                t.setTextSize(44);
                t.setColor(0xFFFFFFFF);
                c.drawText((i + 1) + ".  " + name, 110, y, t);
            }
        }
        if (d.activeHour >= 0) {
            y += 140;
            t.setTypeface(bold);
            t.setTextSize(46);
            t.setColor(0xFFFFFFFF);
            c.drawText(AlfaCyrillic.ui("Eng faol soatim"), 90, y, t);
            y += 90;
            t.setTextSize(80);
            t.setColor(ACCENT);
            c.drawText(String.format("%02d:00", d.activeHour), 90, y, t);
        }
        if (!d.emojis.isEmpty()) {
            y += 150;
            t.setTypeface(Typeface.DEFAULT);
            t.setTextSize(100);
            t.setColor(0xFFFFFFFF);
            c.drawText(TextUtils.join(" ", d.emojis), 90, y, t);
        }
        t.setTypeface(bold);
        t.setTextSize(42);
        t.setColor(0xCCFFFFFF);
        t.setTextAlign(Paint.Align.CENTER);
        c.drawText("AlfaGram · alfagram.uz", W / 2f, H - 110, t);
        return b;
    }
}
