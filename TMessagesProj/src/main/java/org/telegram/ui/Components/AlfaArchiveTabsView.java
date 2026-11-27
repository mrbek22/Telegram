package org.telegram.ui.Components;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AlfaCyrillic;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.Theme;

/**
 * AlfaGram — arxiv ro'yxati tepasidagi tur tugmalari: Hammasi, Shaxsiy, Guruhlar, Kanallar, Botlar, O'qilmagan.
 * Tanlangan tur faqat shu ekranda amal qiladi (papka yaratilmaydi).
 */
public class AlfaArchiveTabsView extends HorizontalScrollView {

    private final TextView[] chips;
    private final String[] titles;
    private final Utilities.Callback<Integer> onSelect;
    private int selected = -1;

    /**
     * @param titles birinchisi — "Hammasi" (tur = -1), qolganlari tur indeksi 0..n-1
     */
    public AlfaArchiveTabsView(Context context, String[] titles, Utilities.Callback<Integer> onSelect) {
        super(context);
        this.titles = titles;
        this.onSelect = onSelect;
        setHorizontalScrollBarEnabled(false);
        setOverScrollMode(OVER_SCROLL_NEVER);

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(12), dp(8), dp(12), dp(8));
        addView(row, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        chips = new TextView[titles.length];
        for (int i = 0; i < titles.length; i++) {
            final int category = i - 1;
            TextView chip = new TextView(context);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            chip.setTypeface(AndroidUtilities.bold());
            chip.setGravity(Gravity.CENTER);
            chip.setSingleLine();
            chip.setPadding(dp(14), 0, dp(14), 0);
            chip.setOnClickListener(v -> {
                if (selected != category) {
                    onSelect.run(category);
                }
            });
            chips[i] = chip;
            row.addView(chip, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 32, 0, 0, i == titles.length - 1 ? 0 : 8, 0));
        }
        update(-1, null);
    }

    /** @param unread har tur uchun o'qilmagan chatlar soni (null — ko'rsatilmaydi) */
    public void update(int category, int[] unread) {
        selected = category;
        final int accent = Theme.getColor(Theme.key_featuredStickers_addButton);
        final int text = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText);
        for (int i = 0; i < chips.length; i++) {
            final boolean on = i - 1 == category;
            String title = AlfaCyrillic.ui(titles[i]);
            if (unread != null && i > 0 && i - 1 < unread.length && unread[i - 1] > 0) {
                title += "  " + unread[i - 1];
            }
            chips[i].setText(title);
            chips[i].setTextColor(on ? 0xFFFFFFFF : text);
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(16));
            bg.setColor(on ? accent : ColorUtils.setAlphaComponent(text, 0x14));
            chips[i].setBackground(bg);
        }
    }
}
