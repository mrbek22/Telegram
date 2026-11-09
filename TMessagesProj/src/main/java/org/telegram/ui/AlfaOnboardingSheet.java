package org.telegram.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AlfaCyrillic;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.SharedConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieImageView;
import org.telegram.ui.Stories.recorder.ButtonWithCounterView;

import static org.telegram.messenger.AndroidUtilities.dp;

/**
 * AlfaGram — birinchi ochilishda bir marta chiqadigan 3 ta slayd: Ghost rejim, o'chirilgan xabarlar,
 * APK himoyasi. Har slaydda Telegram'ning animatsiyali (qimirlaydigan) emojisi; emoji to'plami hali
 * yuklanmagan bo'lsa — oddiy katta emoji. "Keyingi" tugmasi yoki barmoq bilan surib almashtiriladi.
 */
public class AlfaOnboardingSheet extends BottomSheet {

    private static final String PREF_KEY = "alfaOnboardingV1";
    private static boolean checkedThisSession;

    /** emoji nomzodlari ("|" bilan, birinchi animatsiyalisi olinadi), sarlavha, matn */
    private static final String[][] SLIDES = {
            {"👻|🙈|🫥", "Ghost rejim",
                    "Onlayn ko'rinmasdan o'qing: «yozyapti» va «o'qildi» belgilari yuborilmaydi. Chatlar ro'yxati tepasidagi 👻 tugmasi bilan bir bosishda yoqiladi."},
            {"🗑|👀|🤫", "Hech narsa yo'qolmaydi",
                    "Suhbatdoshingiz xabarni o'chirsa ham, u sizda 🗑 belgisi bilan qoladi. Tahrirlangan xabarning eski matnini ko'rish uchun xabarni bosib turing."},
            {"👮|🛡|🔒", "APK himoyasi",
                    "Chatlarda tarqalayotgan virus dasturlar (APK) o'rnatilmaydi, shubhali havolalar haqida ogohlantiramiz. Hammasi faqat telefoningizda ishlaydi."},
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

    private final LinearLayout content;
    private final RLottieImageView emojiView;
    private final TextView emojiFallbackView;
    private final TextView titleView;
    private final TextView textView;
    private final View[] dots = new View[SLIDES.length];
    private final ButtonWithCounterView button;
    private int page;
    private boolean switching;

    @SuppressLint("ClickableViewAccessibility")
    public AlfaOnboardingSheet(BaseFragment fragment) {
        super(fragment.getParentActivity(), false, fragment.getResourceProvider());
        Context context = fragment.getParentActivity();
        fixNavigationBar();

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(12));

        content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(content, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        FrameLayout emojiFrame = new FrameLayout(context);
        emojiView = new RLottieImageView(context);
        emojiFrame.addView(emojiView, LayoutHelper.createFrame(140, 140, Gravity.CENTER));
        emojiFallbackView = new TextView(context);
        emojiFallbackView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 84);
        emojiFallbackView.setGravity(Gravity.CENTER);
        emojiFrame.addView(emojiFallbackView, LayoutHelper.createFrame(140, 140, Gravity.CENTER));
        content.addView(emojiFrame, LayoutHelper.createLinear(140, 140, Gravity.CENTER_HORIZONTAL));

        titleView = new TextView(context);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setGravity(Gravity.CENTER);
        titleView.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        content.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 14, 0, 0));

        textView = new TextView(context);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        textView.setGravity(Gravity.CENTER);
        textView.setLineSpacing(dp(2), 1f);
        textView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText));
        textView.setMinLines(4);
        content.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 8, 10, 8, 0));

        LinearLayout dotsLayout = new LinearLayout(context);
        dotsLayout.setOrientation(LinearLayout.HORIZONTAL);
        dotsLayout.setGravity(Gravity.CENTER);
        for (int i = 0; i < dots.length; i++) {
            View dot = new View(context);
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(getThemedColor(Theme.key_featuredStickers_addButton));
            dot.setBackground(circle);
            dots[i] = dot;
            dotsLayout.addView(dot, LayoutHelper.createLinear(7, 7, 4, 0, 4, 0));
        }
        root.addView(dotsLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 18, 0, 0));

        button = new ButtonWithCounterView(context, resourcesProvider);
        button.setOnClickListener(v -> next());
        root.addView(button, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, 0, 18, 0, 4));

        // barmoq bilan chapga/o'ngga surish
        GestureDetector detector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (Math.abs(velocityX) > Math.abs(velocityY) && Math.abs(velocityX) > dp(300)) {
                    if (velocityX < 0) {
                        next();
                    } else {
                        previous();
                    }
                    return true;
                }
                return false;
            }
        });
        content.setOnTouchListener((v, event) -> detector.onTouchEvent(event));

        setCustomView(root);
        bind(0, 0);
    }

    private void next() {
        if (switching) {
            return;
        }
        if (page >= SLIDES.length - 1) {
            dismiss();
            return;
        }
        bind(page + 1, 1);
    }

    private void previous() {
        if (switching || page <= 0) {
            return;
        }
        bind(page - 1, -1);
    }

    /** direction: 0 — animatsiyasiz, 1 — oldinga, -1 — orqaga. */
    private void bind(int newPage, int direction) {
        if (direction == 0) {
            page = newPage;
            applyContent();
            return;
        }
        switching = true;
        content.animate().alpha(0f).translationX(-dp(36) * direction).setDuration(140).withEndAction(() -> {
            page = newPage;
            applyContent();
            content.setTranslationX(dp(36) * direction);
            content.animate().alpha(1f).translationX(0).setDuration(220).withEndAction(() -> switching = false).start();
        }).start();
    }

    private void applyContent() {
        String[] slide = SLIDES[page];
        String[] candidates = slide[0].split("\\|");
        TLRPC.Document document = null;
        try {
            MediaDataController mdc = MediaDataController.getInstance(currentAccount);
            for (String emoji : candidates) {
                document = mdc.getEmojiAnimatedSticker(emoji);
                if (document != null) {
                    break;
                }
            }
        } catch (Throwable ignore) {
        }
        if (document != null) {
            emojiView.setVisibility(View.VISIBLE);
            emojiFallbackView.setVisibility(View.GONE);
            emojiView.setAutoRepeat(true);
            emojiView.setAnimation(document, 140, 140);
            emojiView.playAnimation();
        } else {
            emojiView.setVisibility(View.GONE);
            emojiFallbackView.setVisibility(View.VISIBLE);
            emojiFallbackView.setText(candidates[0]);
        }
        titleView.setText(AlfaCyrillic.ui(slide[1]));
        textView.setText(AlfaCyrillic.ui(slide[2]));
        for (int i = 0; i < dots.length; i++) {
            dots[i].setAlpha(i == page ? 1f : 0.3f);
        }
        button.setText(AlfaCyrillic.ui(page >= SLIDES.length - 1 ? "Boshlash" : "Keyingi"), page > 0);
    }
}
