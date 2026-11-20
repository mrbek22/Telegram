package org.telegram.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;
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
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Stories.recorder.ButtonWithCounterView;

import static org.telegram.messenger.AndroidUtilities.dp;

/**
 * AlfaGram — birinchi ochilishda bir marta chiqadigan «AlfaGram imkoniyatlari» ro'yxati (iOS bilan bir xil):
 * har qatorda ko'k plitkadagi oq ikonka, sarlavha va izoh, pastda «Tushunarli!».
 */
public class AlfaOnboardingSheet extends BottomSheet {

    private static final String PREF_KEY = "alfaOnboardingV2";
    private static boolean checkedThisSession;

    /** ikonka, sarlavha, izoh */
    private static final Object[][] FEATURES = {
            {R.drawable.alfa_ic_ghost, "Ghost rejim", "Onlayn ko'rinmasdan o'qing: «yozyapti» va «o'qildi» yuborilmaydi. Chatlar ro'yxati tepasidagi 👻 tugmasi bilan bir bosishda yoqing."},
            {R.drawable.alfa_ic_deleted, "O'chirilgan xabarlar qoladi", "Suhbatdoshingiz xabarni o'chirsa ham, u sizda 🗑 belgisi bilan saqlanib qoladi."},
            {R.drawable.alfa_ic_edited, "Tahrir tarixi", "Tahrirlangan xabarni bosib turing → «Tahrir tarixi» — barcha oldingi matnlarni ko'rasiz."},
            {R.drawable.alfa_ic_lock, "Chat qulfi", "Chat ichida ⋮ → «Chatni qulflash». U barmoq izi yoki PIN bilan ochiladi, xabarlari ro'yxatda va bildirishnomada ko'rinmaydi."},
            {R.drawable.alfa_ic_stealth, "Story'larni anonim ko'rish", "Story'larni muallifga bildirmasdan ko'ring."},
            {R.drawable.alfa_ic_contacts, "10 ta akkaunt", "10 tagacha akkauntni bitta ilovada ishlating. Keraklisini maxfiy qilib yashiring yoki vaqtincha to'xtating."},
            {R.drawable.alfa_ic_calls, "Qo'ng'iroqdan oldin so'rash", "Tasodifan bosib, qo'ng'iroq qilib yubormaysiz — avval «Qo'ng'iroq qilinsinmi?» deb so'raladi."},
            {R.drawable.alfa_ic_privacy, "Havola himoyasi", "Soxta bank, Click, Payme yoki Telegram saytlari va «sovg'a» va'da qiluvchi havolalarni ochishdan oldin ogohlantiramiz."},
            {R.drawable.alfa_ic_language, "O'zbekcha va kirillcha", "Ilova o'zbek tilida, xohlasangiz kirillda. Lotincha yozganingizni kirillda yuborish ham mumkin."},
            {R.drawable.alfa_ic_settings, "Hammasi bir joyda", "Barcha imkoniyatlarni Sozlamalar → AlfaGram bo'limida yoqib-o'chirasiz."},
    };

    /** Chatlar ro'yxati ochilganda (DialogsActivity.onResume): hali ko'rilmagan bo'lsa bir marta. */
    public static void maybeShow(BaseFragment fragment) {
        if (checkedThisSession || fragment == null) {
            return;
        }
        checkedThisSession = true;
        final SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences("alfa_features", 0);
        if (prefs.getBoolean(PREF_KEY, false)) {
            return;
        }
        AndroidUtilities.runOnUIThread(() -> {
            if (fragment.getParentActivity() == null || fragment.isPaused() || fragment.getVisibleDialog() != null || SharedConfig.appLocked) {
                // keyingi safar ro'yxat ochilganda yana urinib ko'riladi
                checkedThisSession = false;
                return;
            }
            prefs.edit().putBoolean(PREF_KEY, true).apply();
            try {
                fragment.showDialog(new AlfaOnboardingSheet(fragment));
            } catch (Throwable ignore) {
            }
        }, 900);
    }

    public AlfaOnboardingSheet(BaseFragment fragment) {
        super(fragment.getParentActivity(), false, fragment.getResourceProvider());
        Context context = fragment.getParentActivity();
        fixNavigationBar();

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(12));

        TextView titleView = new TextView(context);
        titleView.setText(AlfaCyrillic.ui("AlfaGram imkoniyatlari"));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        root.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 4, 0, 4, 10));

        ScrollView scrollView = new ScrollView(context);
        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(list, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        for (int i = 0; i < FEATURES.length; i++) {
            list.addView(makeRow(context, (Integer) FEATURES[i][0], (String) FEATURES[i][1], (String) FEATURES[i][2], i != FEATURES.length - 1));
        }
        int heightDp = (int) (AndroidUtilities.displaySize.y * 0.62f / AndroidUtilities.density);
        root.addView(scrollView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, Math.max(240, heightDp)));

        ButtonWithCounterView button = new ButtonWithCounterView(context, resourcesProvider);
        button.setText(AlfaCyrillic.ui("Tushunarli!"), false);
        button.setOnClickListener(v -> dismiss());
        root.addView(button, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, 0, 14, 0, 4));

        setCustomView(root);
    }

    /** Bitta funksiya: ko'k plitkada oq ikonka, sarlavha, izoh va pastida ingichka chiziq. */
    private View makeRow(Context context, int icon, String title, String text, boolean divider) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.VERTICAL);

        LinearLayout line = new LinearLayout(context);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);

        FrameLayout tile = new FrameLayout(context);
        GradientDrawable tileBackground = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFF2563EB, 0xFF0034B6});
        tileBackground.setCornerRadius(dp(14));
        tile.setBackground(tileBackground);
        ImageView iconView = new ImageView(context);
        iconView.setImageResource(icon);
        iconView.setColorFilter(new PorterDuffColorFilter(0xFFFFFFFF, PorterDuff.Mode.SRC_IN));
        iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        tile.addView(iconView, LayoutHelper.createFrame(28, 28, Gravity.CENTER));
        line.addView(tile, LayoutHelper.createLinear(52, 52));

        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView titleView = new TextView(context);
        titleView.setText(AlfaCyrillic.ui(title));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        texts.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        TextView textView = new TextView(context);
        textView.setText(AlfaCyrillic.ui(text));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        textView.setLineSpacing(dp(1), 1f);
        textView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText));
        texts.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 3, 0, 0));
        line.addView(texts, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 14, 0, 0, 0));

        row.addView(line, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 12, 0, 12));
        if (divider) {
            View separator = new View(context);
            separator.setBackgroundColor(getThemedColor(Theme.key_divider));
            row.addView(separator, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 66, 0, 0, 0));
        }
        return row;
    }
}
