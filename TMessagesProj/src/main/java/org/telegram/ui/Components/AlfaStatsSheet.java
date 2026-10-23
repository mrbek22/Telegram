package org.telegram.ui.Components;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AlfaCyrillic;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.Vector;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;

import java.util.HashMap;

/**
 * AlfaGram — chat statistikasi. Media sonlari serverdan (getSearchCounters),
 * kim ko'p yozgani / faol vaqt / emoji esa xabarlarni ko'rib chiqib hisoblanadi.
 * Hammasi telefonda, hech narsa tashqariga yuborilmaydi.
 */
public class AlfaStatsSheet extends BottomSheet {

    private static final int MAX_SCAN = 4000; // tahlil qilinadigan xabarlar chegarasi

    private final int currentAccount;
    private final long dialogId;
    private final Theme.ResourcesProvider rp;

    private LinearLayout column;
    private TextView progressView;

    // hisoblar
    private int scanned;
    private int outCount, inCount;
    private int firstDate;
    private final int[] hourHist = new int[24];
    private final int[] dowHist = new int[7];
    private final HashMap<String, Integer> dayHist = new HashMap<>();
    private final HashMap<String, Integer> emojiHist = new HashMap<>();
    private final HashMap<Long, Integer> senderHist = new HashMap<>();
    private boolean finished;

    public AlfaStatsSheet(Context context, int currentAccount, long dialogId, Theme.ResourcesProvider resourcesProvider) {
        super(context, false, resourcesProvider);
        this.currentAccount = currentAccount;
        this.dialogId = dialogId;
        this.rp = resourcesProvider;

        fixNavigationBar(getThemedColor(Theme.key_dialogBackground));
        setApplyBottomPadding(false);

        ScrollView scroll = new ScrollView(context);
        column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(16), dp(14), dp(16), dp(20));
        scroll.addView(column, LayoutHelper.createScroll(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));

        TextView title = new TextView(context);
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        title.setText(AlfaCyrillic.ui("Chat statistikasi"));
        column.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        progressView = new TextView(context);
        progressView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        progressView.setTextColor(getThemedColor(Theme.key_dialogTextGray3));
        progressView.setText(AlfaCyrillic.ui("Hisoblanmoqda…"));
        column.addView(progressView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        setCustomView(scroll);

        loadMediaCounts();
        scanHistory(0);
    }

    // ── Media sonlari (serverdan, aniq) ───────────────
    private void loadMediaCounts() {
        TLRPC.TL_messages_getSearchCounters req = new TLRPC.TL_messages_getSearchCounters();
        req.peer = MessagesController.getInstance(currentAccount).getInputPeer(dialogId);
        if (req.peer == null) return;
        req.filters.add(new TLRPC.TL_inputMessagesFilterPhotos());
        req.filters.add(new TLRPC.TL_inputMessagesFilterVideo());
        req.filters.add(new TLRPC.TL_inputMessagesFilterRoundVoice());
        req.filters.add(new TLRPC.TL_inputMessagesFilterDocument());
        req.filters.add(new TLRPC.TL_inputMessagesFilterMusic());
        req.filters.add(new TLRPC.TL_inputMessagesFilterUrl());
        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (!(response instanceof Vector)) return;
            int photos = 0, videos = 0, voice = 0, files = 0, music = 0, links = 0;
            Vector v = (Vector) response;
            for (int i = 0; i < v.objects.size(); i++) {
                Object o = v.objects.get(i);
                if (!(o instanceof TLRPC.TL_messages_searchCounter)) continue;
                TLRPC.TL_messages_searchCounter c = (TLRPC.TL_messages_searchCounter) o;
                if (c.filter instanceof TLRPC.TL_inputMessagesFilterPhotos) photos = c.count;
                else if (c.filter instanceof TLRPC.TL_inputMessagesFilterVideo) videos = c.count;
                else if (c.filter instanceof TLRPC.TL_inputMessagesFilterRoundVoice) voice = c.count;
                else if (c.filter instanceof TLRPC.TL_inputMessagesFilterDocument) files = c.count;
                else if (c.filter instanceof TLRPC.TL_inputMessagesFilterMusic) music = c.count;
                else if (c.filter instanceof TLRPC.TL_inputMessagesFilterUrl) links = c.count;
            }
            showMediaRows(photos, videos, voice, files, music, links);
        }));
    }

    private boolean mediaShown;

    private void showMediaRows(int photos, int videos, int voice, int files, int music, int links) {
        if (mediaShown) return;
        mediaShown = true;
        addHeader(AlfaCyrillic.ui("Media"));
        addRow(R.drawable.msg_filled_data_photos, 0xFF4F9CF9, AlfaCyrillic.ui("Rasmlar"), format(photos));
        addRow(R.drawable.msg_filled_data_videos, 0xFF8E6BE6, AlfaCyrillic.ui("Videolar"), format(videos));
        addRow(R.drawable.msg_filled_data_voice, 0xFFE6894F, AlfaCyrillic.ui("Ovozli xabarlar"), format(voice));
        addRow(R.drawable.msg_filled_data_files, 0xFF4FB06B, AlfaCyrillic.ui("Fayllar"), format(files));
        addRow(R.drawable.msg_filled_data_music, 0xFFE64F93, AlfaCyrillic.ui("Musiqa"), format(music));
        addRow(R.drawable.msg_link2, 0xFF4FB0C6, AlfaCyrillic.ui("Havolalar"), format(links));
    }

    // ── Xabarlarni tahlil qilish (sahifa-sahifa) ─────
    private void scanHistory(int offsetId) {
        TLRPC.TL_messages_getHistory req = new TLRPC.TL_messages_getHistory();
        req.peer = MessagesController.getInstance(currentAccount).getInputPeer(dialogId);
        if (req.peer == null) {
            finishScan();
            return;
        }
        req.offset_id = offsetId;
        req.limit = 100;
        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (!(response instanceof TLRPC.messages_Messages)) {
                finishScan();
                return;
            }
            TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
            if (res.messages.isEmpty()) {
                finishScan();
                return;
            }
            long selfId = UserConfig.getInstance(currentAccount).getClientUserId();
            int lastId = offsetId;
            for (int i = 0; i < res.messages.size(); i++) {
                TLRPC.Message m = res.messages.get(i);
                if (m == null) continue;
                lastId = m.id;
                if (m instanceof TLRPC.TL_messageEmpty || m.action != null) continue;
                scanned++;
                if (firstDate == 0 || m.date < firstDate) firstDate = m.date;
                long from = m.from_id != null && m.from_id.user_id != 0 ? m.from_id.user_id : (m.out ? selfId : (dialogId > 0 ? dialogId : 0));
                if (m.out || from == selfId) outCount++; else inCount++;
                if (from != 0) senderHist.merge(from, 1, Integer::sum);
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.setTimeInMillis(m.date * 1000L);
                hourHist[cal.get(java.util.Calendar.HOUR_OF_DAY)]++;
                dowHist[(cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7]++;
                String dk = cal.get(java.util.Calendar.YEAR) + "-" + cal.get(java.util.Calendar.DAY_OF_YEAR);
                dayHist.merge(dk, 1, Integer::sum);
                countEmojis(m.message);
            }
            progressView.setText(AlfaCyrillic.ui("Hisoblanmoqda… ") + format(scanned));
            if (scanned < MAX_SCAN && res.messages.size() >= 90 && lastId != offsetId) {
                scanHistory(lastId);
            } else {
                finishScan();
            }
        }));
    }

    private void countEmojis(String text) {
        if (TextUtils.isEmpty(text)) return;
        try {
            java.util.ArrayList<Emoji.EmojiSpanRange> ranges = Emoji.parseEmojis(text);
            for (int i = 0; i < ranges.size() && i < 20; i++) {
                String e = ranges.get(i).code.toString();
                emojiHist.merge(e, 1, Integer::sum);
            }
        } catch (Throwable ignore) {
        }
    }

    private void finishScan() {
        if (finished) return;
        finished = true;
        column.removeView(progressView);

        int total = outCount + inCount;
        addHeader(AlfaCyrillic.ui("Xabarlar"));
        addRow(R.drawable.msg_filled_data_messages, 0xFF4F9CF9, AlfaCyrillic.ui("Tahlil qilingan xabarlar"), format(total) + (scanned >= MAX_SCAN ? "+" : ""));
        if (total > 0) {
            addBar(AlfaCyrillic.ui("Siz"), outCount, total, 0xFF4F9CF9);
            addBar(AlfaCyrillic.ui("Suhbatdosh"), inCount, total, 0xFFE6894F);
        }

        addHeader(AlfaCyrillic.ui("Faollik"));
        if (firstDate > 0) {
            addRow(R.drawable.msg_calendar2, 0xFF8E6BE6, AlfaCyrillic.ui("Birinchi xabar (taxminan)"), LocaleController.getInstance().getFormatterYear().format(firstDate * 1000L));
        }
        int topHour = maxIndex(hourHist);
        if (topHour >= 0 && hourHist[topHour] > 0) {
            String range = String.format(java.util.Locale.US, "%02d:00–%02d:00", topHour, (topHour + 1) % 24);
            addRow(R.drawable.msg_views, 0xFF4FB06B, AlfaCyrillic.ui("Eng faol vaqt"), range);
        }
        int topDow = maxIndex(dowHist);
        if (topDow >= 0 && dowHist[topDow] > 0) {
            String[] days = {AlfaCyrillic.ui("Dushanba"), AlfaCyrillic.ui("Seshanba"), AlfaCyrillic.ui("Chorshanba"), AlfaCyrillic.ui("Payshanba"), AlfaCyrillic.ui("Juma"), AlfaCyrillic.ui("Shanba"), AlfaCyrillic.ui("Yakshanba")};
            addRow(R.drawable.msg_calendar2, 0xFFE64F93, AlfaCyrillic.ui("Eng faol kun"), days[topDow]);
        }
        int maxDay = 0;
        for (Integer vv : dayHist.values()) maxDay = Math.max(maxDay, vv);
        if (maxDay > 0) {
            addRow(R.drawable.msg_stats, 0xFFE6894F, AlfaCyrillic.ui("Bir kundagi rekord"), format(maxDay) + AlfaCyrillic.ui(" ta xabar"));
        }

        String topEmoji = maxKey(emojiHist);
        if (topEmoji != null) {
            addRow(R.drawable.msg_message, 0xFF4FB0C6, AlfaCyrillic.ui("Eng ko'p ishlatilgan"), topEmoji + "  ×" + format(emojiHist.get(topEmoji)));
        }

        // guruh bo'lsa — eng faol a'zolar
        if (dialogId < 0 && senderHist.size() > 1) {
            addHeader(AlfaCyrillic.ui("Eng faol a'zolar"));
            java.util.ArrayList<java.util.Map.Entry<Long, Integer>> list = new java.util.ArrayList<>(senderHist.entrySet());
            list.sort((a, b) -> b.getValue() - a.getValue());
            for (int i = 0; i < Math.min(5, list.size()); i++) {
                TLRPC.User u = MessagesController.getInstance(currentAccount).getUser(list.get(i).getKey());
                String name = u != null ? UserObject.getUserName(u) : "#" + list.get(i).getKey();
                addRow(0, 0xFF4F9CF9, (i + 1) + ". " + name, format(list.get(i).getValue()));
            }
        }

        addNote(AlfaCyrillic.ui("Media sonlari aniq. Qolgan ko'rsatkichlar oxirgi ") + format(scanned) + AlfaCyrillic.ui(" ta xabar bo'yicha. Hammasi telefoningizda hisoblandi."));
    }

    // ── UI yordamchilari ──────────────────────────────
    private void addHeader(String text) {
        TextView t = new TextView(getContext());
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        t.setTypeface(AndroidUtilities.bold());
        t.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueText));
        t.setText(text.toUpperCase());
        column.addView(t, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 14, 0, 6));
    }

    private void addRow(int icon, int color, String label, String value) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(7), 0, dp(7));

        FrameLayout iconWrap = new FrameLayout(getContext());
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(9));
        iconWrap.setBackground(bg);
        if (icon != 0) {
            ImageView iv = new ImageView(getContext());
            Drawable d = getContext().getResources().getDrawable(icon).mutate();
            d.setColorFilter(new PorterDuffColorFilter(0xFFFFFFFF, PorterDuff.Mode.SRC_IN));
            iv.setImageDrawable(d);
            iv.setScaleType(ImageView.ScaleType.CENTER);
            iconWrap.addView(iv, LayoutHelper.createFrame(20, 20, Gravity.CENTER));
        }
        row.addView(iconWrap, LayoutHelper.createLinear(34, 34, 0, 0, 12, 0));

        TextView lbl = new TextView(getContext());
        lbl.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        lbl.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        lbl.setText(label);
        lbl.setMaxLines(1);
        lbl.setEllipsize(TextUtils.TruncateAt.END);
        row.addView(lbl, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL));

        TextView val = new TextView(getContext());
        val.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        val.setTypeface(AndroidUtilities.bold());
        val.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        val.setText(value);
        row.addView(val, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 0, 0));

        column.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void addBar(String label, int value, int total, int color) {
        int percent = total > 0 ? Math.round(value * 100f / total) : 0;
        LinearLayout wrap = new LinearLayout(getContext());
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(0, dp(6), 0, dp(6));

        LinearLayout top = new LinearLayout(getContext());
        top.setOrientation(LinearLayout.HORIZONTAL);
        TextView lbl = new TextView(getContext());
        lbl.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        lbl.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        lbl.setText(label);
        top.addView(lbl, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));
        TextView val = new TextView(getContext());
        val.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        val.setTypeface(AndroidUtilities.bold());
        val.setTextColor(color);
        val.setText(format(value) + "  (" + percent + "%)");
        top.addView(val);
        wrap.addView(top, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        FrameLayout track = new FrameLayout(getContext());
        GradientDrawable tb = new GradientDrawable();
        tb.setColor(getThemedColor(Theme.key_windowBackgroundGray));
        tb.setCornerRadius(dp(4));
        track.setBackground(tb);
        View fill = new View(getContext());
        GradientDrawable fb = new GradientDrawable();
        fb.setColor(color);
        fb.setCornerRadius(dp(4));
        fill.setBackground(fb);
        track.addView(fill, LayoutHelper.createFrame(Math.max(2, percent), 8, Gravity.LEFT | Gravity.CENTER_VERTICAL)); // foiz kenglik (weight o'rniga)
        // foizni haqiqiy kenglikka aylantirish
        track.post(() -> {
            View f = track.getChildAt(0);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) f.getLayoutParams();
            lp.width = (int) (track.getWidth() * percent / 100f);
            lp.height = dp(8);
            f.setLayoutParams(lp);
        });
        wrap.addView(track, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 8, 0, 6, 0, 0));
        column.addView(wrap, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void addNote(String text) {
        TextView t = new TextView(getContext());
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        t.setTextColor(getThemedColor(Theme.key_dialogTextGray3));
        t.setText(text);
        column.addView(t, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 16, 0, 0));
    }

    private static String format(int n) {
        return String.format(java.util.Locale.US, "%,d", n).replace(',', ' ');
    }

    private static int maxIndex(int[] arr) {
        int idx = -1, max = 0;
        for (int i = 0; i < arr.length; i++) if (arr[i] > max) { max = arr[i]; idx = i; }
        return idx;
    }

    private static String maxKey(HashMap<String, Integer> map) {
        String key = null; int max = 0;
        for (java.util.Map.Entry<String, Integer> e : map.entrySet()) if (e.getValue() > max) { max = e.getValue(); key = e.getKey(); }
        return key;
    }
}
