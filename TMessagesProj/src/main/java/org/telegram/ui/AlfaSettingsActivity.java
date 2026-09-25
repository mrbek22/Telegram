package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AlfaFeatures;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

/**
 * AlfaGram — yashirin funksiyalar sozlamalari ekrani.
 * Har bir bayroq alohida switch bo'lib chiqadi; o'zgartirishlar darhol saqlanadi.
 */
public class AlfaSettingsActivity extends BaseFragment {

    private ListAdapter adapter;

    // Item ID'lari (bayroq nomlariga mos)
    private static final int ROW_PROTECTION_HEADER = 100;
    private static final int ROW_ANTI_DELETE = 1;
    private static final int ROW_EDIT_HISTORY = 2;
    private static final int ROW_TYPING_LOG = 3;
    private static final int ROW_PROTECTION_INFO = 101;

    private static final int ROW_LIMITS_HEADER = 200;
    private static final int ROW_UNRESTRICTED_SAVE = 4;
    private static final int ROW_UNLIMITED_PINS = 5;
    private static final int ROW_ALLOW_SCREENSHOTS = 6;
    private static final int ROW_LIMITS_INFO = 201;

    private static final int ROW_PRIVACY_HEADER = 300;
    private static final int ROW_GHOST_MODE = 7;
    private static final int ROW_STEALTH_STORIES = 8;
    private static final int ROW_PRIVACY_INFO = 301;

    private static final int ROW_ADS_HEADER = 400;
    private static final int ROW_BLOCK_ADS = 9;
    private static final int ROW_ADS_INFO = 401;

    private final int[] rows = new int[]{
            ROW_PROTECTION_HEADER,
            ROW_ANTI_DELETE,
            ROW_EDIT_HISTORY,
            ROW_TYPING_LOG,
            ROW_PROTECTION_INFO,
            ROW_LIMITS_HEADER,
            ROW_UNRESTRICTED_SAVE,
            ROW_UNLIMITED_PINS,
            ROW_ALLOW_SCREENSHOTS,
            ROW_LIMITS_INFO,
            ROW_PRIVACY_HEADER,
            ROW_GHOST_MODE,
            ROW_STEALTH_STORIES,
            ROW_PRIVACY_INFO,
            ROW_ADS_HEADER,
            ROW_BLOCK_ADS,
            ROW_ADS_INFO,
    };

    @Override
    public View createView(Context context) {
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
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setAdapter(adapter = new ListAdapter(context));
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setOnItemClickListener((view, position) -> {
            if (position < 0 || position >= rows.length) return;
            int row = rows[position];
            String key = keyForRow(row);
            if (key == null) return;
            boolean newValue = !currentValue(row);
            AlfaFeatures.setFlag(key, newValue);
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(newValue);
            }
        });

        return fragmentView;
    }

    private String keyForRow(int row) {
        switch (row) {
            case ROW_ANTI_DELETE: return "antiDelete";
            case ROW_EDIT_HISTORY: return "editHistory";
            case ROW_TYPING_LOG: return "typingLog";
            case ROW_UNRESTRICTED_SAVE: return "unrestrictedSave";
            case ROW_UNLIMITED_PINS: return "unlimitedPins";
            case ROW_ALLOW_SCREENSHOTS: return "allowScreenshots";
            case ROW_GHOST_MODE: return "ghostMode";
            case ROW_STEALTH_STORIES: return "stealthStories";
            case ROW_BLOCK_ADS: return "blockTelegramAds";
            default: return null;
        }
    }

    private boolean currentValue(int row) {
        switch (row) {
            case ROW_ANTI_DELETE: return AlfaFeatures.antiDelete;
            case ROW_EDIT_HISTORY: return AlfaFeatures.editHistory;
            case ROW_TYPING_LOG: return AlfaFeatures.typingLog;
            case ROW_UNRESTRICTED_SAVE: return AlfaFeatures.unrestrictedSave;
            case ROW_UNLIMITED_PINS: return AlfaFeatures.unlimitedPins;
            case ROW_ALLOW_SCREENSHOTS: return AlfaFeatures.allowScreenshots;
            case ROW_GHOST_MODE: return AlfaFeatures.ghostMode;
            case ROW_STEALTH_STORIES: return AlfaFeatures.stealthStories;
            case ROW_BLOCK_ADS: return AlfaFeatures.blockTelegramAds;
            default: return false;
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private final Context mContext;

        ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return rows.length;
        }

        @Override
        public int getItemViewType(int position) {
            int row = rows[position];
            if (row >= 100 && row < 200 && row % 100 == 0) return 0; // header
            if (row >= 100 && row < 500 && row % 100 == 1) return 2; // info
            return 1; // check
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return getItemViewType(holder.getAdapterPosition()) == 1;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v;
            switch (viewType) {
                case 0:
                    v = new HeaderCell(mContext);
                    v.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 2:
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
            int row = rows[position];
            int viewType = holder.getItemViewType();
            if (viewType == 0) {
                HeaderCell h = (HeaderCell) holder.itemView;
                switch (row) {
                    case ROW_PROTECTION_HEADER: h.setText("Xabar himoyasi"); break;
                    case ROW_LIMITS_HEADER: h.setText("Cheklovlar"); break;
                    case ROW_PRIVACY_HEADER: h.setText("Maxfiylik"); break;
                    case ROW_ADS_HEADER: h.setText("Reklama"); break;
                }
            } else if (viewType == 2) {
                TextInfoPrivacyCell t = (TextInfoPrivacyCell) holder.itemView;
                switch (row) {
                    case ROW_PROTECTION_INFO:
                        t.setText("O'chirilgan xabarlar chatingizda qoladi, tahrirlangan xabarlarning eski matnini saqlaymiz, kim yozdi-yubormaganini bilib turasiz.");
                        break;
                    case ROW_LIMITS_INFO:
                        t.setText("Standart Telegram cheklovlarini olib tashlash — audio/rasm saqlash, pin cheki, screenshot.");
                        break;
                    case ROW_PRIVACY_INFO:
                        t.setText("Ghost mode — o'qilgan/yozayotgan/onlayn signallarini yubormaslik. Anonim story — story ko'ruvchilar ro'yxatiga tushmaslik.");
                        break;
                    case ROW_ADS_INFO:
                        t.setText("Telegram'ning kanalarda ko'rsatiladigan reklama xabarlarini yashirish.");
                        break;
                }
            } else {
                TextCheckCell c = (TextCheckCell) holder.itemView;
                boolean divider = position + 1 < rows.length && (rows[position + 1] % 100 != 0);
                switch (row) {
                    case ROW_ANTI_DELETE:
                        c.setTextAndCheck("O'chirilgan xabarlarni saqlash", AlfaFeatures.antiDelete, divider);
                        break;
                    case ROW_EDIT_HISTORY:
                        c.setTextAndCheck("Tahrir tarixi", AlfaFeatures.editHistory, divider);
                        break;
                    case ROW_TYPING_LOG:
                        c.setTextAndCheck("Yozdi, yubormadi jurnali", AlfaFeatures.typingLog, divider);
                        break;
                    case ROW_UNRESTRICTED_SAVE:
                        c.setTextAndCheck("Cheklovlarsiz saqlash/forward", AlfaFeatures.unrestrictedSave, divider);
                        break;
                    case ROW_UNLIMITED_PINS:
                        c.setTextAndCheck("Cheksiz pin", AlfaFeatures.unlimitedPins, divider);
                        break;
                    case ROW_ALLOW_SCREENSHOTS:
                        c.setTextAndCheck("Screenshot cheklovini olib tashlash", AlfaFeatures.allowScreenshots, divider);
                        break;
                    case ROW_GHOST_MODE:
                        c.setTextAndCheck("Ghost mode (o'qilgan/yozyapman signalisiz)", AlfaFeatures.ghostMode, divider);
                        break;
                    case ROW_STEALTH_STORIES:
                        c.setTextAndCheck("Anonim story ko'rish", AlfaFeatures.stealthStories, divider);
                        break;
                    case ROW_BLOCK_ADS:
                        c.setTextAndCheck("Telegram reklamalarini bloklash", AlfaFeatures.blockTelegramAds, divider);
                        break;
                }
            }
        }
    }
}
