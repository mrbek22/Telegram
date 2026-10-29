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
import org.telegram.messenger.ImageReceiver;
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
    private PodiumView podium;

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
                    LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));
            podium = new PodiumView(ctx, account, d.top, true);
            c.addView(podium, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, PodiumView.HEIGHT_DP));
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

    /**
     * Shohsupa: o'rtada 1-o'rin (oltin, toj), chapda 2-o'rin (kumush), o'ngda 3-o'rin (bronza).
     * Ilova ichida ham, ulashiladigan rasmda ham shu View chiziladi.
     */
    private static class PodiumView extends View {
        static final int HEIGHT_DP = 262;
        private static final int[] RING = {0xFFFACC15, 0xFFCBD5E1, 0xFFD97706};
        private static final int[][] STEP = {{0xFFFDE68A, 0xFFEAB308}, {0xFFCBD5E1, 0xFF94A3B8}, {0xFFFBBF77, 0xFFC2410C}};
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
            // joylashuv: 2-o'rin chapda, 1-o'rin o'rtada, 3-o'rin o'ngda
            final int[] slotOf = {1, 0, 2};
            for (int rank = 0; rank < n; rank++) {
                float x = left + slotOf[rank] * (colW + gap);
                float cx = x + colW / 2f;
                // pog'ona
                float stepTop = bottom - dp(STEP_H[rank]);
                r.set(x, stepTop, x + colW, bottom);
                p.setShader(new LinearGradient(0, stepTop, 0, bottom, STEP[rank][0], STEP[rank][1], Shader.TileMode.CLAMP));
                canvas.drawRoundRect(r, dp(12), dp(12), p);
                p.setShader(null);
                tp.setTypeface(AndroidUtilities.bold());
                tp.setTextAlign(Paint.Align.CENTER);
                tp.setTextSize(dp(22));
                tp.setColor(STEP_TEXT[rank]);
                canvas.drawText(String.valueOf(rank + 1), cx, stepTop + dp(28), tp);
                // son va ism
                float y = stepTop - dp(8);
                if (showCounts) {
                    tp.setTypeface(Typeface.DEFAULT);
                    tp.setTextSize(dp(11.5f));
                    tp.setColor(ACCENT);
                    canvas.drawText(AlfaCyrillic.ui(counts[rank] + " ta"), cx, y, tp);
                    y -= dp(16);
                }
                tp.setTypeface(AndroidUtilities.bold());
                tp.setTextSize(dp(13));
                tp.setColor(0xFFFFFFFF);
                CharSequence nm = TextUtils.ellipsize(names[rank], tp, colW - dp(4), TextUtils.TruncateAt.END);
                canvas.drawText(nm, 0, nm.length(), cx, y, tp);
                // avatar + halqa
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

        if (podium != null && podium.getWidth() > 0 && podium.getHeight() > 0) {
            y += 140;
            t.setTextSize(48);
            c.drawText(AlfaCyrillic.ui("Eng ko'p yozishganlarim"), 90, y, t);
            // ilovadagi shohsupani o'z avatarlari bilan kattalashtirib chizamiz
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
