package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

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

    private static class Row {
        final int type;
        final String key;
        final String title;
        final String info;

        static Row header(String title) { return new Row(TYPE_HEADER, null, title, null); }
        static Row info(String text)    { return new Row(TYPE_INFO, null, null, text); }
        static Row toggle(String key, String title) { return new Row(TYPE_SWITCH, key, title, null); }

        private Row(int type, String key, String title, String info) {
            this.type = type;
            this.key = key;
            this.title = title;
            this.info = info;
        }
    }

    private final ArrayList<Row> rows = new ArrayList<>();

    private void buildRows() {
        rows.clear();

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
        rows.add(Row.info("Versiya: AlfaGram alfa11"));
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
        listView.setAdapter(new ListAdapter(context));
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setOnItemClickListener((view, position) -> {
            if (position < 0 || position >= rows.size()) return;
            Row row = rows.get(position);
            if (row.type != TYPE_SWITCH) return;
            boolean newVal = !currentValue(row.key);
            AlfaFeatures.setFlag(row.key, newVal);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(newVal);
            }
        });

        return fragmentView;
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
            return holder.getItemViewType() == TYPE_SWITCH;
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
                case TYPE_SWITCH:
                    TextCheckCell c = (TextCheckCell) holder.itemView;
                    boolean nextIsSwitch = position + 1 < rows.size() && rows.get(position + 1).type == TYPE_SWITCH;
                    c.setTextAndCheck(row.title, currentValue(row.key), nextIsSwitch);
                    break;
            }
        }
    }
}
