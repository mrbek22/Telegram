package org.telegram.ui;

import android.content.Context;
import android.graphics.PorterDuff;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AlfaFeatures;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;

/**
 * AlfaGram sozlamalari — bo'limlarga ajratilgan switch-list.
 */
public class AlfaSettingsActivity extends BaseFragment {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_SWITCH = 1;
    private static final int TYPE_INFO = 2;
    private static final int TYPE_ACTION = 3;
    private static final int TYPE_HERO = 4;
    private static final int TYPE_QUICK = 5;

    // Tezkor kafellar: kalit, nom, ikonka, gradient
    private static final String[] QUICK_KEYS = { "ghostMode", "stealthStories", "noReadReceipts" };
    private static final String[] QUICK_TITLES = { "Ghost", "Story", "O'qildi" };
    private static final int[] QUICK_ICONS = { R.drawable.alfa_ic_ghost, R.drawable.alfa_ic_stealth, R.drawable.alfa_ic_privacy };
    private static final int[][] QUICK_GRADIENTS = { { 0xFF4F46E5, 0xFF8B5CF6 }, { 0xFF7C3AED, 0xFFEC4899 }, { 0xFF06B6D4, 0xFF6366F1 } };

    private ListAdapter listAdapter;

    private static class Row {
        final int type;
        final String key;
        final String title;
        final String info;
        final Runnable action;

        static Row header(String title) { return new Row(TYPE_HEADER, null, title, null, null); }
        static Row info(String text)    { return new Row(TYPE_INFO, null, null, text, null); }
        static Row toggle(String key, String title) { return new Row(TYPE_SWITCH, key, title, null, null); }
        static Row action(String title, Runnable r) { return new Row(TYPE_ACTION, null, title, null, r); }

        private Row(int type, String key, String title, String info, Runnable action) {
            this.type = type;
            this.key = key;
            this.title = title;
            this.info = info;
            this.action = action;
        }
    }

    private final ArrayList<Row> rows = new ArrayList<>();

    private void buildRows() {
        rows.clear();

        rows.add(new Row(TYPE_HERO, null, null, null, null));
        rows.add(Row.header("Tezkor"));
        rows.add(new Row(TYPE_QUICK, null, null, null, null));
        rows.add(Row.info("Ghost — onlayn/yozyapman signali ketmaydi. Story — anonim ko'rish. O'qildi — o'qish tasdig'i yuborilmaydi."));

        rows.add(Row.header("Ilova qulfi"));
        rows.add(Row.action("PIN kod sozlash", () -> {
            try {
                presentFragment(new PasscodeActivity(PasscodeActivity.TYPE_SETUP_CODE));
            } catch (Throwable ignore) {
            }
        }));
        rows.add(Row.info("Ilova ochilganda PIN so'raladi. Kimдир ilovangizni ochsa, xabarlaringizni ko'ra olmaydi."));

        rows.add(Row.header("Akkaunt boshqaruvi"));
        rows.add(Row.action("Maxfiylik va akkauntni o'chirish", () -> {
            try {
                presentFragment(new PrivacySettingsActivity());
            } catch (Throwable ignore) {
            }
        }));
        rows.add(Row.info("Telegram'ning maxfiylik va akkauntni o'chirish (TTL) sozlamalari."));

        rows.add(Row.header("Xabar himoyasi"));
        rows.add(Row.toggle("antiDelete", "O'chirilgan xabarlarni saqlash"));
        rows.add(Row.toggle("editHistory", "Tahrir tarixini yozish"));
        rows.add(Row.toggle("typingLog", "Yozdi-yubormadi jurnali"));
        rows.add(Row.info("Kim yozib, o'chirsa yoki tahrirlagan bo'lsa — hammasi sizda qoladi. Long-press bilan tarixni ko'ring."));

        rows.add(Row.header("Xabar yuborish"));
        rows.add(Row.toggle("confirmSend", "Yuborishdan oldin tasdiq"));
        rows.add(Row.toggle("sendByEnter", "Enter tugmasi bilan yuborish"));
        rows.add(Row.toggle("silentByDefault", "Sukut (silent) rejim'da"));
        rows.add(Row.info("Yuborishni tezlashtiring yoki tasodifiy yuborishning oldini oling."));

        rows.add(Row.header("Cheklovlarsiz"));
        rows.add(Row.toggle("unrestrictedSave", "Cheklovlarsiz saqlash/forward"));
        rows.add(Row.toggle("allowScreenshots", "Screenshotga ruxsat"));
        rows.add(Row.toggle("unlimitedPins", "Cheksiz pin"));
        rows.add(Row.toggle("forwardWithoutAuthor", "Forward — muallifsiz"));
        rows.add(Row.toggle("forwardWithoutReply", "Forward — javobsiz"));
        rows.add(Row.info("Telegram'ning standart cheklovlarini olib tashlash."));

        rows.add(Row.header("Chat ro'yxati"));
        rows.add(Row.toggle("hideStories", "Storieslar qatorini yashirish"));
        rows.add(Row.toggle("hideMuted", "Ovozsiz chatlarni yashirish"));
        rows.add(Row.toggle("compactChats", "Kichik (compact) chat qatori"));
        rows.add(Row.info("Chatlar sahifasini xohlaganingizday sozlang."));

        rows.add(Row.header("Chat ichida"));
        rows.add(Row.toggle("showMessageIds", "Xabar ID sini ko'rsatish"));
        rows.add(Row.toggle("swipeToReply", "Chapga siljitib javob"));
        rows.add(Row.toggle("disableStickerSuggest", "Stiker taklifini o'chirish"));
        rows.add(Row.toggle("fastSendAnimation", "Tez yuborish animatsiyasi"));

        rows.add(Row.header("Maxfiylik"));
        rows.add(Row.toggle("ghostMode", "Ghost mode (o'qilgan/yozyapman signalisiz)"));
        rows.add(Row.toggle("stealthStories", "Anonim story ko'rish"));
        rows.add(Row.toggle("noReadReceipts", "O'qish tasdig'ini yubormaslik"));
        rows.add(Row.toggle("hidePhoneInSettings", "Sozlamalarda telefonni yashirish"));
        rows.add(Row.toggle("blurAppInRecents", "Recent apps'da blur (xiralik)"));
        rows.add(Row.info("Sizdan hech qanday signal ketmaydi va hech kim sizni kuzatolmaydi."));

        rows.add(Row.header("Ko'rinish"));
        rows.add(Row.toggle("showIdInProfile", "Profilda ID ko'rinsin"));
        rows.add(Row.toggle("hideSeenTicks", "Ko'rildi belgisini yashirish"));
        rows.add(Row.toggle("disableAnimations", "Barcha animatsiyalarni o'chirish"));

        rows.add(Row.header("Media"));
        rows.add(Row.toggle("autoSaveVoice", "Barcha ovozli xabarlarni saqlash"));
        rows.add(Row.toggle("autoSaveVideoNotes", "Barcha video-notelarni saqlash"));
        rows.add(Row.info("Kelgan media avtomatik telefoningizga tushiriladi."));

        rows.add(Row.header("Reklama"));
        rows.add(Row.toggle("blockTelegramAds", "Telegram reklamalarini bloklash"));
        rows.add(Row.toggle("hideSponsoredMessages", "\"Homiylik\" xabarlarni yashirish"));

        rows.add(Row.header("Ilova"));
        rows.add(Row.toggle("openLastChat", "Ochilganda so'nggi chatga o'tish"));
        rows.add(Row.toggle("debugLogs", "Debug loglar"));

        rows.add(Row.header("Ilova haqida"));
        rows.add(Row.action("Maxfiylik siyosati", () -> openLink("https://alfagram.uz/privacy")));
        rows.add(Row.action("Hisob va ma'lumotlarni o'chirish", () -> openLink("https://alfagram.uz/delete-account")));
        rows.add(Row.action("Sayt: alfagram.uz", () -> openLink("https://alfagram.uz")));
        String version = "";
        try {
            android.content.Context c = org.telegram.messenger.ApplicationLoader.applicationContext;
            version = c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
        } catch (Throwable ignore) {
        }
        rows.add(Row.info("AlfaGram " + version + " — Telegram API asosidagi norasmiy klient. Telegram bilan bog'liq emas."));
    }

    private void openLink(String url) {
        try {
            org.telegram.messenger.browser.Browser.openUrl(getParentActivity(), url);
        } catch (Throwable ignore) {
        }
    }

    @Override
    public View createView(Context context) {
        buildRows();

        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("AlfaGram sozlamalari");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        RecyclerListView listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context));
        listView.setAdapter(listAdapter = new ListAdapter(context));
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setOnItemClickListener((view, position) -> {
            if (position < 0 || position >= rows.size()) return;
            Row row = rows.get(position);
            if (row.type == TYPE_ACTION) {
                if (row.action != null) row.action.run();
                return;
            }
            if (row.type != TYPE_SWITCH) return;
            boolean newVal = !currentValue(row.key);
            AlfaFeatures.setFlag(row.key, newVal);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(newVal);
            }
            if (isQuickKey(row.key)) {
                // tezkor kafellar ham yangilansin
                int quickPos = indexOfType(TYPE_QUICK);
                if (quickPos >= 0) listAdapter.notifyItemChanged(quickPos);
            }
        });

        return fragmentView;
    }

    private static boolean isQuickKey(String key) {
        for (String k : QUICK_KEYS) if (k.equals(key)) return true;
        return false;
    }

    private int indexOfType(int type) {
        for (int i = 0; i < rows.size(); i++) if (rows.get(i).type == type) return i;
        return -1;
    }

    private static android.graphics.drawable.GradientDrawable gradient(int[] colors, float radiusDp) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TL_BR, colors);
        d.setCornerRadius(AndroidUtilities.dp(radiusDp));
        return d;
    }

    /** Gradient bosh karta: belgi + "AlfaGram <versiya>" + izoh. */
    private View createHero(Context context) {
        FrameLayout wrap = new FrameLayout(context);
        wrap.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(14), AndroidUtilities.dp(14), AndroidUtilities.dp(6));

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(18));
        card.setBackground(gradient(new int[] { AlfaFeatures.BRAND_START, AlfaFeatures.BRAND_END }, 22));
        wrap.addView(card, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        ImageView icon = new ImageView(context);
        icon.setScaleType(ImageView.ScaleType.CENTER);
        icon.setImageResource(R.drawable.alfa_ic_alfagram);
        icon.setColorFilter(0xFFFFFFFF, PorterDuff.Mode.SRC_IN);
        android.graphics.drawable.GradientDrawable iconBg = new android.graphics.drawable.GradientDrawable();
        iconBg.setColor(0x38FFFFFF);
        iconBg.setCornerRadius(AndroidUtilities.dp(16));
        icon.setBackground(iconBg);
        card.addView(icon, LayoutHelper.createLinear(48, 48));

        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(LinearLayout.VERTICAL);
        card.addView(texts, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL, 14, 0, 0, 0));

        String version = "";
        try {
            version = " " + context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Throwable ignore) {
        }
        TextView title = new TextView(context);
        title.setText("AlfaGram" + version);
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        title.setTypeface(AndroidUtilities.bold());
        texts.addView(title);

        TextView subtitle = new TextView(context);
        subtitle.setText("Yashirin funksiyalar · bepul");
        subtitle.setTextColor(0xFFF3E8FF);
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        texts.addView(subtitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 3, 0, 0));
        return wrap;
    }

    /** Tezkor kafellar qatori: bosilganda bayroq almashadi. */
    private View createQuickRow(Context context) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(AndroidUtilities.dp(9), AndroidUtilities.dp(4), AndroidUtilities.dp(9), AndroidUtilities.dp(12));
        for (int i = 0; i < QUICK_KEYS.length; i++) {
            LinearLayout tile = new LinearLayout(context);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER_HORIZONTAL);
            tile.setPadding(0, AndroidUtilities.dp(14), 0, AndroidUtilities.dp(12));
            ImageView icon = new ImageView(context);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            tile.addView(icon, LayoutHelper.createLinear(24, 24));
            TextView name = new TextView(context);
            name.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            name.setTypeface(AndroidUtilities.bold());
            name.setGravity(Gravity.CENTER);
            tile.addView(name, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 0));
            TextView state = new TextView(context);
            state.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            state.setGravity(Gravity.CENTER);
            tile.addView(state, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
            final int index = i;
            tile.setOnClickListener(v -> {
                String key = QUICK_KEYS[index];
                AlfaFeatures.setFlag(key, !currentValue(key));
                listAdapter.notifyDataSetChanged();
            });
            row.addView(tile, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 5, 0, 5, 0));
        }
        return row;
    }

    private void bindQuickRow(LinearLayout row) {
        for (int i = 0; i < row.getChildCount() && i < QUICK_KEYS.length; i++) {
            LinearLayout tile = (LinearLayout) row.getChildAt(i);
            boolean on = currentValue(QUICK_KEYS[i]);
            if (on) {
                tile.setBackground(gradient(QUICK_GRADIENTS[i], 18));
            } else {
                android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
                bg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                bg.setCornerRadius(AndroidUtilities.dp(18));
                tile.setBackground(bg);
            }
            ImageView icon = (ImageView) tile.getChildAt(0);
            icon.setImageResource(QUICK_ICONS[i]);
            icon.setColorFilter(on ? 0xFFFFFFFF : Theme.getColor(Theme.key_windowBackgroundWhiteGrayText), PorterDuff.Mode.SRC_IN);
            TextView name = (TextView) tile.getChildAt(1);
            name.setText(QUICK_TITLES[i]);
            name.setTextColor(on ? 0xFFFFFFFF : Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            TextView state = (TextView) tile.getChildAt(2);
            state.setText(on ? "Yoqilgan" : "O'chiq");
            state.setTextColor(on ? 0xFFF3E8FF : Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        }
    }

    private boolean currentValue(String key) {
        switch (key) {
            case "antiDelete": return AlfaFeatures.antiDelete;
            case "editHistory": return AlfaFeatures.editHistory;
            case "typingLog": return AlfaFeatures.typingLog;
            case "confirmSend": return AlfaFeatures.confirmSend;
            case "sendByEnter": return AlfaFeatures.sendByEnter;
            case "silentByDefault": return AlfaFeatures.silentByDefault;
            case "unrestrictedSave": return AlfaFeatures.unrestrictedSave;
            case "allowScreenshots": return AlfaFeatures.allowScreenshots;
            case "unlimitedPins": return AlfaFeatures.unlimitedPins;
            case "forwardWithoutAuthor": return AlfaFeatures.forwardWithoutAuthor;
            case "forwardWithoutReply": return AlfaFeatures.forwardWithoutReply;
            case "hideStories": return AlfaFeatures.hideStories;
            case "hideMuted": return AlfaFeatures.hideMuted;
            case "compactChats": return AlfaFeatures.compactChats;
            case "showMessageIds": return AlfaFeatures.showMessageIds;
            case "swipeToReply": return AlfaFeatures.swipeToReply;
            case "disableStickerSuggest": return AlfaFeatures.disableStickerSuggest;
            case "fastSendAnimation": return AlfaFeatures.fastSendAnimation;
            case "ghostMode": return AlfaFeatures.ghostMode;
            case "stealthStories": return AlfaFeatures.stealthStories;
            case "noReadReceipts": return AlfaFeatures.noReadReceipts;
            case "hidePhoneInSettings": return AlfaFeatures.hidePhoneInSettings;
            case "blurAppInRecents": return AlfaFeatures.blurAppInRecents;
            case "showIdInProfile": return AlfaFeatures.showIdInProfile;
            case "hideSeenTicks": return AlfaFeatures.hideSeenTicks;
            case "disableAnimations": return AlfaFeatures.disableAnimations;
            case "autoSaveVoice": return AlfaFeatures.autoSaveVoice;
            case "autoSaveVideoNotes": return AlfaFeatures.autoSaveVideoNotes;
            case "blockTelegramAds": return AlfaFeatures.blockTelegramAds;
            case "hideSponsoredMessages": return AlfaFeatures.hideSponsoredMessages;
            case "openLastChat": return AlfaFeatures.openLastChat;
            case "debugLogs": return AlfaFeatures.debugLogs;
            default: return false;
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        private final Context mContext;
        ListAdapter(Context context) { mContext = context; }

        @Override public int getItemCount() { return rows.size(); }

        @Override public int getItemViewType(int position) { return rows.get(position).type; }

        @Override public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int t = holder.getItemViewType();
            return t == TYPE_SWITCH || t == TYPE_ACTION;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v;
            switch (viewType) {
                case TYPE_HEADER:
                    v = new HeaderCell(mContext);
                    v.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case TYPE_INFO:
                    v = new TextInfoPrivacyCell(mContext);
                    v.setBackground(Theme.getThemedDrawableByKey(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                    break;
                case TYPE_ACTION:
                    v = new org.telegram.ui.Cells.TextCell(mContext);
                    v.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case TYPE_HERO:
                    v = createHero(mContext);
                    break;
                case TYPE_QUICK:
                    v = createQuickRow(mContext);
                    break;
                default:
                    v = new TextCheckCell(mContext);
                    v.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
            }
            v.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(v);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            Row row = rows.get(position);
            switch (row.type) {
                case TYPE_HEADER:
                    ((HeaderCell) holder.itemView).setText(row.title);
                    break;
                case TYPE_INFO:
                    ((TextInfoPrivacyCell) holder.itemView).setText(row.info);
                    break;
                case TYPE_ACTION:
                    ((org.telegram.ui.Cells.TextCell) holder.itemView).setText(row.title, false);
                    break;
                case TYPE_QUICK:
                    bindQuickRow((LinearLayout) holder.itemView);
                    break;
                case TYPE_SWITCH:
                    TextCheckCell c = (TextCheckCell) holder.itemView;
                    boolean nextIsSwitch = position + 1 < rows.size() && rows.get(position + 1).type == TYPE_SWITCH;
                    c.setTextAndCheck(row.title, currentValue(row.key), nextIsSwitch);
                    break;
            }
        }
    }
}
