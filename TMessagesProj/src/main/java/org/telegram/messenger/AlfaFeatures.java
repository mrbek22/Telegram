package org.telegram.messenger;

import android.content.SharedPreferences;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

/**
 * AlfaGram — yashirin funksiyalar bayroqlari.
 * Bo'limlar bo'yicha guruhlangan, sozlamalar ekranidan boshqariladi.
 * loadPrefs() bir marta ilova ishga tushganda chaqirilishi kerak.
 *
 * Public static maydonlar — SAMARALI qiymat = foydalanuvchi tanlovi && server ruxsati.
 * Server (AlfaConfig) funksiyani faqat O'CHIRA oladi (kill-switch), majburan yoqa olmaydi.
 * Yangi bayroq: maydon + KEYS + getField() + setField().
 */
public class AlfaFeatures {

    private static final String PREFS = "alfa_features";
    private static boolean loaded;

    /**
     * Google Play versiyasi: Telegram'ning Play uchun cheklovlari (restriction_reason "android")
     * qo'llanadi va Telegram'ning o'z ichki yangilanish tekshiruvi o'chiriladi.
     */
    public static final boolean PLAY_BUILD = true;

    /** Brend gradienti (ko'k logotip): royal ko'k → okean ko'k. Oq matn kontrasti har nuqtada >= 5.1:1. */
    public static final int BRAND_START = 0xFF2563EB;
    public static final int BRAND_END = 0xFF0270B0;

    // ── Xabar himoyasi ─────────────────────────────
    public static boolean antiDelete = true;
    public static boolean editHistory = true;
    public static boolean typingLog = true;

    // ── Xabar yuborish ─────────────────────────────
    /** Xabar yuborishdan oldin tasdiq oynasi. */
    public static boolean confirmSend = false;
    /** Enter tugmasi bilan yuborish (send button emas). */
    public static boolean sendByEnter = false;
    /** Barcha xabarlar sukut bilan (silent) yuborilsin. */
    public static boolean silentByDefault = false;

    // ── Cheklovlarsiz ──────────────────────────────
    public static boolean unrestrictedSave = true;
    public static boolean allowScreenshots = true;
    public static boolean unlimitedPins = true;
    /** Muallif ko'rsatilmasdan forward. */
    public static boolean forwardWithoutAuthor = false;
    /** Reply'ni yashirib forward. */
    public static boolean forwardWithoutReply = false;

    // ── Chat ro'yxati ko'rinishi ───────────────────
    /** Storieslar qatorini yashirish. */
    public static boolean hideStories = false;
    /** Ovozsizga qo'yilgan chatlarni yashirish. */
    public static boolean hideMuted = false;
    /** Chatlarni ixcham (kichik) ko'rinishda ko'rsatish. */
    public static boolean compactChats = false;

    // ── Chat ichida ─────────────────────────────
    /** Xabar ID sini vaqt yonida ko'rsatish. */
    public static boolean showMessageIds = false;
    /** Chapga siljitib javob berish. */
    public static boolean swipeToReply = true;
    /** Emoji terganda stiker taklifini o'chirish. */
    public static boolean disableStickerSuggest = false;
    /** Xabarlarda "yozildi" indikatorini uzoq turishi. */
    public static boolean fastSendAnimation = false;

    // ── Maxfiylik ──────────────────────────────────
    public static boolean ghostMode = false;
    public static boolean stealthStories = false;
    /** Sozlamalarda telefon raqamni yashirish. */
    public static boolean hidePhoneInSettings = false;
    /** Recent apps ekranida ilovani xiralashtirish (blur). */
    public static boolean blurAppInRecents = false;
    /** O'qilgan xabarlarni "o'qilmagan" ko'rinishida qoldirish. */
    public static boolean noReadReceipts = false;

    // ── Ko'rinish (Appearance) ─────────────────────
    /** Profil / chatlarda ID ni ko'rsatish. */
    public static boolean showIdInProfile = false;
    /** Xabar vaqtidan tashqari "seen" tikini yashirish. */
    public static boolean hideSeenTicks = false;
    /** Animatsiyalarni butunlay o'chirish (tez ish). */
    public static boolean disableAnimations = false;

    // ── Media va yuklab olish ──────────────────────
    /** Barcha ovozli xabarlarni avtomatik saqlash. */
    public static boolean autoSaveVoice = false;
    /** Barcha video-notelar avtomatik saqlash. */
    public static boolean autoSaveVideoNotes = false;

    // ── Reklama ────────────────────────────────────
    public static boolean blockTelegramAds = true;
    /** Kanalarda "Homiylik" (sponsored) xabarlarni yashirish. */
    public static boolean hideSponsoredMessages = true;

    // ── Ilova ──────────────────────────────────────
    /** Ilova ochilganda so'nggi chat avtomatik ochilsin. */
    public static boolean openLastChat = false;
    /** Debug: Loglarni ko'rsatish. */
    public static boolean debugLogs = false;

    // ── Til ────────────────────────────────────────
    /** O'zbek tilidagi interfeysni kirill alifbosida ko'rsatish. */
    public static boolean cyrillicUi = false;
    /** Lotincha yozilgan xabarni kirillda yuborish. */
    public static boolean sendCyrillic = false;

    // ── Kuzatuv ────────────────────────────────────
    /** Kontaktlar onlayn tarixini yozib borish. */
    public static boolean onlineTracker = true;

    /** Barcha bayroq kalitlari (SharedPreferences kaliti = maydon nomi). */
    private static final String[] KEYS = {
            "antiDelete", "editHistory", "typingLog",
            "confirmSend", "sendByEnter", "silentByDefault",
            "unrestrictedSave", "allowScreenshots", "unlimitedPins", "forwardWithoutAuthor", "forwardWithoutReply",
            "hideStories", "hideMuted", "compactChats",
            "showMessageIds", "swipeToReply", "disableStickerSuggest", "fastSendAnimation",
            "ghostMode", "stealthStories", "hidePhoneInSettings", "blurAppInRecents", "noReadReceipts",
            "showIdInProfile", "hideSeenTicks", "disableAnimations",
            "autoSaveVoice", "autoSaveVideoNotes",
            "blockTelegramAds", "hideSponsoredMessages",
            "openLastChat", "debugLogs",
            "cyrillicUi", "sendCyrillic", "onlineTracker",
    };

    /** Foydalanuvchi tanlovi (server ta'sirisiz) — sozlamalar switch'i shuni ko'rsatadi. */
    private static final HashMap<String, Boolean> userPrefs = new HashMap<>();
    /** Server o'chirib qo'ygan (kill-switch) kalitlar. */
    private static final HashSet<String> remoteDisabled = new HashSet<>();
    /** loadPrefs tugadi — endi yon ta'sirlar ishlashi mumkin. */
    private static boolean ready;

    static {
        // Boshlang'ich tanlov = maydonlardagi sukut qiymatlari
        for (String k : KEYS) {
            userPrefs.put(k, getField(k));
        }
    }

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS, 0);
    }

    // ── Ikki marta bosish amali ────────────────────
    public static final int DOUBLE_TAP_REACTION = 0;
    public static final int DOUBLE_TAP_REPLY = 1;
    public static final int DOUBLE_TAP_COPY = 2;
    public static final int DOUBLE_TAP_OFF = 3;
    public static final String[] DOUBLE_TAP_NAMES = { "Reaksiya (standart)", "Javob berish", "Matnni nusxalash", "O'chiq" };
    private static int doubleTapAction = -1;

    public static int getDoubleTapAction() {
        if (doubleTapAction < 0) {
            try {
                doubleTapAction = prefs().getInt("doubleTapAction", DOUBLE_TAP_REACTION);
            } catch (Throwable ignore) {
                doubleTapAction = DOUBLE_TAP_REACTION;
            }
            if (doubleTapAction < 0 || doubleTapAction >= DOUBLE_TAP_NAMES.length) {
                doubleTapAction = DOUBLE_TAP_REACTION;
            }
        }
        return doubleTapAction;
    }

    public static void setDoubleTapAction(int action) {
        if (action < 0 || action >= DOUBLE_TAP_NAMES.length) return;
        doubleTapAction = action;
        try {
            prefs().edit().putInt("doubleTapAction", action).apply();
        } catch (Throwable ignore) {
        }
    }

    /** Sozlamalarni SharedPreferences'dan yuklash (bir marta). */
    public static void loadPrefs() {
        synchronized (AlfaFeatures.class) {
            if (loaded) return;
            loaded = true;
            try {
                SharedPreferences p = prefs();
                for (String k : KEYS) {
                    try {
                        if (p.contains(k)) {
                            userPrefs.put(k, p.getBoolean(k, getUserPref(k)));
                        }
                    } catch (Throwable ignore) {
                    }
                }
            } catch (Throwable ignore) {
            }
            recomputeAll(null);
        }
        // Keshdagi server cheklovlari ilova boshidanoq qo'llansin
        try {
            AlfaConfig.loadCached();
        } catch (Throwable ignore) {
        }
        synchronized (AlfaFeatures.class) {
            ready = true;
        }
    }

    /** Bitta bayroqni yozib qo'yish (sozlamalar ekranidan): foydalanuvchi tanlovi saqlanadi. */
    public static void setFlag(String key, boolean value) {
        if (key == null) return;
        boolean changed = false;
        boolean effective;
        synchronized (AlfaFeatures.class) {
            userPrefs.put(key, value);
            try {
                prefs().edit().putBoolean(key, value).apply();
            } catch (Throwable ignore) {
            }
            effective = value && !remoteDisabled.contains(key);
            if (getField(key) != effective) {
                setField(key, effective);
                changed = true;
            }
        }
        if (changed) {
            onFlagChanged(key, effective);
        }
    }

    /**
     * Server cheklovlari (AlfaConfig.parse, UI oqimida). disabled — server false bergan kalitlar;
     * null yoki bo'sh = hammasiga ruxsat. loadPrefs'dan oldin ham, keyin ham chaqirilishi mumkin.
     */
    public static void applyRemote(Collection<String> disabled) {
        HashSet<String> changed = new HashSet<>();
        boolean notify;
        synchronized (AlfaFeatures.class) {
            remoteDisabled.clear();
            if (disabled != null) {
                for (String k : disabled) {
                    if (k != null) remoteDisabled.add(k);
                }
            }
            recomputeAll(changed);
            notify = ready;
        }
        if (notify) {
            for (String k : changed) {
                onFlagChanged(k, getField(k));
            }
        }
    }

    /** Foydalanuvchi tanlovi (server cheklovisiz). */
    public static synchronized boolean getUserPref(String key) {
        if (key == null) return false;
        Boolean v = userPrefs.get(key);
        if (v == null) {
            // KEYS'da yo'q kalit — saqlangan qiymatdan o'qiladi
            try {
                v = prefs().getBoolean(key, false);
            } catch (Throwable ignore) {
                v = false;
            }
            userPrefs.put(key, v);
        }
        return v;
    }

    /** Server bu funksiyaga ruxsat beradimi (false = server o'chirgan). */
    public static synchronized boolean isRemoteAllowed(String key) {
        return key == null || !remoteDisabled.contains(key);
    }

    /** Samarali qiymatlarni qayta hisoblash; o'zgarganlar changed'ga yoziladi. */
    private static void recomputeAll(Collection<String> changed) {
        for (String k : KEYS) {
            boolean effective = getUserPref(k) && !remoteDisabled.contains(k);
            if (getField(k) != effective) {
                setField(k, effective);
                if (changed != null) changed.add(k);
            }
        }
    }

    /**
     * Bayroqning samarali qiymati o'zgarganda chaqiriladi (setFlag yoki server cheklovi, UI oqimida).
     * Toggle paytidagi yon ta'sirlar shu switch'ga: 'value' — yangi samarali qiymat.
     */
    private static void onFlagChanged(String key, boolean value) {
        try {
            switch (key) {
                case "allowScreenshots":
                    AndroidUtilities.runOnUIThread(FlagSecureReason::alfaInvalidateAll);
                    break;
                case "blurAppInRecents":
                    AndroidUtilities.runOnUIThread(() -> {
                        if (org.telegram.ui.LaunchActivity.instance != null) {
                            org.telegram.ui.LaunchActivity.instance.alfaUpdateRecentsBlur();
                        }
                    });
                    break;
                case "disableAnimations":
                    AndroidUtilities.runOnUIThread(() -> LiteMode.alfaOnAnimationsToggled(value));
                    break;
                case "cyrillicUi":
                    // til satrlari qayta yuklanadi va ekranlar qayta chiziladi (reloadInterface)
                    AndroidUtilities.runOnUIThread(() -> LocaleController.getInstance().alfaReloadCurrentLanguage(UserConfig.selectedAccount));
                    break;
                case "debugLogs":
                    BuildVars.LOGS_ENABLED = value;
                    ApplicationLoader.applicationContext.getSharedPreferences("systemConfig", android.content.Context.MODE_PRIVATE)
                            .edit().putBoolean("logsEnabled", value).apply();
                    break;
                default:
                    break;
            }
        } catch (Throwable ignore) {
        }
    }

    private static boolean getField(String key) {
        switch (key) {
            case "antiDelete": return antiDelete;
            case "editHistory": return editHistory;
            case "typingLog": return typingLog;

            case "confirmSend": return confirmSend;
            case "sendByEnter": return sendByEnter;
            case "silentByDefault": return silentByDefault;

            case "unrestrictedSave": return unrestrictedSave;
            case "allowScreenshots": return allowScreenshots;
            case "unlimitedPins": return unlimitedPins;
            case "forwardWithoutAuthor": return forwardWithoutAuthor;
            case "forwardWithoutReply": return forwardWithoutReply;

            case "hideStories": return hideStories;
            case "hideMuted": return hideMuted;
            case "compactChats": return compactChats;

            case "showMessageIds": return showMessageIds;
            case "swipeToReply": return swipeToReply;
            case "disableStickerSuggest": return disableStickerSuggest;
            case "fastSendAnimation": return fastSendAnimation;

            case "ghostMode": return ghostMode;
            case "stealthStories": return stealthStories;
            case "hidePhoneInSettings": return hidePhoneInSettings;
            case "blurAppInRecents": return blurAppInRecents;
            case "noReadReceipts": return noReadReceipts;

            case "showIdInProfile": return showIdInProfile;
            case "hideSeenTicks": return hideSeenTicks;
            case "disableAnimations": return disableAnimations;

            case "autoSaveVoice": return autoSaveVoice;
            case "autoSaveVideoNotes": return autoSaveVideoNotes;

            case "blockTelegramAds": return blockTelegramAds;
            case "hideSponsoredMessages": return hideSponsoredMessages;

            case "openLastChat": return openLastChat;
            case "debugLogs": return debugLogs;
            case "cyrillicUi": return cyrillicUi;
            case "sendCyrillic": return sendCyrillic;
            case "onlineTracker": return onlineTracker;
            default: return false;
        }
    }

    private static void setField(String key, boolean value) {
        switch (key) {
            case "antiDelete": antiDelete = value; break;
            case "editHistory": editHistory = value; break;
            case "typingLog": typingLog = value; break;

            case "confirmSend": confirmSend = value; break;
            case "sendByEnter": sendByEnter = value; break;
            case "silentByDefault": silentByDefault = value; break;

            case "unrestrictedSave": unrestrictedSave = value; break;
            case "allowScreenshots": allowScreenshots = value; break;
            case "unlimitedPins": unlimitedPins = value; break;
            case "forwardWithoutAuthor": forwardWithoutAuthor = value; break;
            case "forwardWithoutReply": forwardWithoutReply = value; break;

            case "hideStories": hideStories = value; break;
            case "hideMuted": hideMuted = value; break;
            case "compactChats": compactChats = value; break;

            case "showMessageIds": showMessageIds = value; break;
            case "swipeToReply": swipeToReply = value; break;
            case "disableStickerSuggest": disableStickerSuggest = value; break;
            case "fastSendAnimation": fastSendAnimation = value; break;

            case "ghostMode": ghostMode = value; break;
            case "stealthStories": stealthStories = value; break;
            case "hidePhoneInSettings": hidePhoneInSettings = value; break;
            case "blurAppInRecents": blurAppInRecents = value; break;
            case "noReadReceipts": noReadReceipts = value; break;

            case "showIdInProfile": showIdInProfile = value; break;
            case "hideSeenTicks": hideSeenTicks = value; break;
            case "disableAnimations": disableAnimations = value; break;

            case "autoSaveVoice": autoSaveVoice = value; break;
            case "autoSaveVideoNotes": autoSaveVideoNotes = value; break;

            case "blockTelegramAds": blockTelegramAds = value; break;
            case "hideSponsoredMessages": hideSponsoredMessages = value; break;

            case "openLastChat": openLastChat = value; break;
            case "debugLogs": debugLogs = value; break;
            case "cyrillicUi": cyrillicUi = value; break;
            case "sendCyrillic": sendCyrillic = value; break;
            case "onlineTracker": onlineTracker = value; break;
        }
    }
}
