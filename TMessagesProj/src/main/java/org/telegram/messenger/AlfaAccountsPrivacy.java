package org.telegram.messenger;

import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * AlfaGram — maxfiy va vaqtincha to'xtatilgan akkauntlar (iOS bilan bir xil).
 * Maxfiy: Sozlamalardagi «Hisoblar» va almashtirishda ko'rinmaydi, bildirishnomasi chiqmaydi;
 * unga faqat AlfaGram → «Maxfiy akkauntlar» orqali kiriladi.
 * To'xtatilgan: qayta yoqilmaguncha bildirishnoma umuman kelmaydi.
 * Kalit — akkauntning foydalanuvchi ID'si (akkaunt raqami o'zgarsa ham saqlanadi).
 */
public final class AlfaAccountsPrivacy {

    private static final String HIDDEN = "alfaHiddenAccounts";
    private static final String PAUSED = "alfaPausedAccounts";

    private AlfaAccountsPrivacy() {
    }

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("alfa_features", 0);
    }

    private static String id(int account) {
        return String.valueOf(UserConfig.getInstance(account).getClientUserId());
    }

    private static boolean contains(String key, int account) {
        Set<String> set = prefs().getStringSet(key, null);
        return set != null && set.contains(id(account));
    }

    private static void set(String key, int account, boolean value) {
        HashSet<String> set = new HashSet<>(prefs().getStringSet(key, new HashSet<>()));
        if (value) {
            set.add(id(account));
        } else {
            set.remove(id(account));
        }
        prefs().edit().putStringSet(key, set).apply();
    }

    public static boolean isHidden(int account) {
        return contains(HIDDEN, account);
    }

    public static void setHidden(int account, boolean hidden) {
        set(HIDDEN, account, hidden);
    }

    public static boolean isPaused(int account) {
        return contains(PAUSED, account);
    }

    public static void setPaused(int account, boolean paused) {
        set(PAUSED, account, paused);
    }

    /** Bildirishnomasi chiqmaydigan akkaunt (maxfiy yoki to'xtatilgan). */
    public static boolean isSilenced(int account) {
        return isHidden(account) || isPaused(account);
    }
}
