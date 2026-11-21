package org.telegram.messenger;

import android.content.SharedPreferences;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;

/**
 * AlfaGram — yashirin chatlar (iOS bilan bir xil): ro'yxatlardan (va uzatish oynasidan) yo'qoladi, ovozsiz bo'ladi.
 * O'z kodi (4 raqamli PIN yoki parol) kiritilgach ko'rinadi — ilova fonga o'tguncha.
 */
public class AlfaHiddenChats {

    private static final String PREFS = "alfa_hidden_chats";
    private static final String KEY_LIST = "list";
    private static final String KEY_CODE = "code";
    private static final String KEY_NUMERIC = "numeric";

    private static volatile Set<String> cache;
    private static volatile boolean revealed;

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS, 0);
    }

    private static String key(int account, long dialogId) {
        return UserConfig.getInstance(account).getClientUserId() + "_" + dialogId;
    }

    private static String hash(String code) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(("alfagram.hidden." + code).getBytes(StandardCharsets.UTF_8));
            StringBuilder b = new StringBuilder();
            for (byte x : digest) {
                b.append(String.format("%02x", x));
            }
            return b.toString();
        } catch (Throwable e) {
            return code;
        }
    }

    // ── kod ──

    public static boolean isEnabled() {
        return prefs().getString(KEY_CODE, null) != null;
    }

    public static boolean isNumericCode() {
        return prefs().getBoolean(KEY_NUMERIC, true);
    }

    public static void setCode(String code, boolean numeric) {
        prefs().edit().putString(KEY_CODE, hash(code)).putBoolean(KEY_NUMERIC, numeric).apply();
        notifyChanged();
    }

    public static boolean checkCode(String code) {
        String stored = prefs().getString(KEY_CODE, null);
        return stored != null && stored.equals(hash(code));
    }

    /** O'chirish: kod o'chadi, hamma chat yana ko'rinadi. */
    public static void disable() {
        prefs().edit().remove(KEY_CODE).remove(KEY_NUMERIC).remove(KEY_LIST).apply();
        cache = new HashSet<>();
        revealed = false;
        notifyChanged();
    }

    // ── ko'rsatilgan holat (faqat shu sessiya) ──

    public static boolean isRevealed() {
        return revealed;
    }

    public static void setRevealed(boolean value) {
        if (revealed != value) {
            revealed = value;
            notifyChanged();
        }
    }

    // ── ro'yxat ──

    private static Set<String> list() {
        Set<String> c = cache;
        if (c == null) {
            c = new HashSet<>(prefs().getStringSet(KEY_LIST, new HashSet<>()));
            cache = c;
        }
        return c;
    }

    public static boolean isHidden(int account, long dialogId) {
        Set<String> c = list();
        return !c.isEmpty() && c.contains(key(account, dialogId));
    }

    /** Yashirin va hozir ko'rsatilmagan — ro'yxatlar uni chiqarmaydi. */
    public static boolean isHiddenNow(int account, long dialogId) {
        return !revealed && isHidden(account, dialogId);
    }

    public static void setHidden(int account, long dialogId, boolean hidden) {
        HashSet<String> c = new HashSet<>(list());
        if (hidden) {
            c.add(key(account, dialogId));
        } else {
            c.remove(key(account, dialogId));
        }
        prefs().edit().putStringSet(KEY_LIST, c).apply();
        cache = c;
        if (hidden) {
            // yashirin chat ovozsiz
            try {
                NotificationsController.getInstance(account).muteDialog(dialogId, 0, true);
            } catch (Throwable ignore) {
            }
        }
        notifyChanged();
    }

    /** Ro'yxatlar qayta yig'iladi (sortDialogs) va chizilad. */
    private static void notifyChanged() {
        AndroidUtilities.runOnUIThread(() -> {
            for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                if (!UserConfig.isValidAccount(a) || !UserConfig.getInstance(a).isClientActivated()) {
                    continue;
                }
                try {
                    MessagesController.getInstance(a).sortDialogs(null);
                    NotificationCenter.getInstance(a).postNotificationName(NotificationCenter.dialogsNeedReload);
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            }
        });
    }
}
