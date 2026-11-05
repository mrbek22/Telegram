package org.telegram.messenger;

import android.content.SharedPreferences;

import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.SwipeGestureSettingsView;

/**
 * AlfaGram — chatlar ro'yxatida ikki tomonlama surish.
 * Chapga surish (o'ng chetdan) va o'ngga surish (chap chetdan) — har biriga alohida amal.
 * Papkalar bor bo'lsa, amal faqat ekran chetidan tortilganda ishlaydi; o'rtadan surish papkani almashtiradi.
 */
public final class AlfaSwipe {

    public static final int NONE = -1;
    /** Sozlamalarda ko'rsatiladigan tartib. */
    public static final int[] OPTIONS = {
            SwipeGestureSettingsView.SWIPE_GESTURE_ARCHIVE,
            SwipeGestureSettingsView.SWIPE_GESTURE_READ,
            SwipeGestureSettingsView.SWIPE_GESTURE_MUTE,
            SwipeGestureSettingsView.SWIPE_GESTURE_DELETE,
            SwipeGestureSettingsView.SWIPE_GESTURE_PIN,
            NONE,
    };
    private static final String[] NAMES = {"Arxivlash", "O'qildi / o'qilmagan", "Ovozsiz qilish", "O'chirish", "Qadash", "O'chiq"};

    /** Papkalar bor bo'lsa amal ishlaydigan chet zonasi (ekran kengligidan ulush). */
    public static final float EDGE = 0.2f;

    /** Joriy surish yo'nalishi: true — o'ngga (chap chetdan tortilgan). Bir vaqtda bitta surish bo'ladi. */
    public static volatile boolean rightDir;

    private static int left = Integer.MIN_VALUE;
    private static int right = Integer.MIN_VALUE;

    private AlfaSwipe() {
    }

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("alfa_features", 0);
    }

    /** Chapga surish amali (sukut — arxivlash). */
    public static int leftAction() {
        if (left == Integer.MIN_VALUE) {
            left = prefs().getInt("swipeLeftAction", SwipeGestureSettingsView.SWIPE_GESTURE_ARCHIVE);
        }
        return left;
    }

    /** O'ngga surish amali (sukut — o'qildi/o'qilmagan). */
    public static int rightAction() {
        if (right == Integer.MIN_VALUE) {
            right = prefs().getInt("swipeRightAction", SwipeGestureSettingsView.SWIPE_GESTURE_READ);
        }
        return right;
    }

    public static void setAction(boolean isRight, int action) {
        if (isRight) {
            right = action;
        } else {
            left = action;
        }
        prefs().edit().putInt(isRight ? "swipeRightAction" : "swipeLeftAction", action).apply();
    }

    /** Joriy yo'nalishdagi amal. */
    public static int current() {
        return rightDir ? rightAction() : leftAction();
    }

    public static String name(int action) {
        for (int i = 0; i < OPTIONS.length; i++) {
            if (OPTIONS[i] == action) {
                return AlfaCyrillic.ui(NAMES[i]);
            }
        }
        return AlfaCyrillic.ui(NAMES[0]);
    }

    public static String title(boolean isRight) {
        return AlfaCyrillic.ui(isRight ? "O'ngga surish" : "Chapga surish");
    }

    /** Amal tanlash oynasi. */
    public static void showPicker(BaseFragment fragment, boolean isRight, Runnable onChanged) {
        if (fragment == null || fragment.getParentActivity() == null) {
            return;
        }
        CharSequence[] items = new CharSequence[OPTIONS.length];
        for (int i = 0; i < OPTIONS.length; i++) {
            items[i] = AlfaCyrillic.ui(NAMES[i]);
        }
        AlertDialog.Builder b = new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider());
        b.setTitle(title(isRight));
        b.setItems(items, (d, which) -> {
            setAction(isRight, OPTIONS[which]);
            if (onChanged != null) {
                onChanged.run();
            }
        });
        fragment.showDialog(b.create());
    }
}
