package org.telegram.messenger;

import android.content.SharedPreferences;

/**
 * AlfaGram — yashirin funksiyalar bayroqlari.
 * Bo'limlar bo'yicha guruhlangan, sozlamalar ekranidan boshqariladi.
 * loadPrefs() bir marta ilova ishga tushganda chaqirilishi kerak.
 */
public class AlfaFeatures {

    private static final String PREFS = "alfa_features";
    private static boolean loaded;

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

    /** Sozlamalarni SharedPreferences'dan yuklash (bir marta). */
    public static void loadPrefs() {
        if (loaded) return;
        loaded = true;
        try {
            SharedPreferences p = ApplicationLoader.applicationContext.getSharedPreferences(PREFS, 0);
            antiDelete = p.getBoolean("antiDelete", antiDelete);
            editHistory = p.getBoolean("editHistory", editHistory);
            typingLog = p.getBoolean("typingLog", typingLog);

            confirmSend = p.getBoolean("confirmSend", confirmSend);
            sendByEnter = p.getBoolean("sendByEnter", sendByEnter);
            silentByDefault = p.getBoolean("silentByDefault", silentByDefault);

            unrestrictedSave = p.getBoolean("unrestrictedSave", unrestrictedSave);
            allowScreenshots = p.getBoolean("allowScreenshots", allowScreenshots);
            unlimitedPins = p.getBoolean("unlimitedPins", unlimitedPins);
            forwardWithoutAuthor = p.getBoolean("forwardWithoutAuthor", forwardWithoutAuthor);
            forwardWithoutReply = p.getBoolean("forwardWithoutReply", forwardWithoutReply);

            hideStories = p.getBoolean("hideStories", hideStories);
            hideMuted = p.getBoolean("hideMuted", hideMuted);
            compactChats = p.getBoolean("compactChats", compactChats);

            showMessageIds = p.getBoolean("showMessageIds", showMessageIds);
            swipeToReply = p.getBoolean("swipeToReply", swipeToReply);
            disableStickerSuggest = p.getBoolean("disableStickerSuggest", disableStickerSuggest);
            fastSendAnimation = p.getBoolean("fastSendAnimation", fastSendAnimation);

            ghostMode = p.getBoolean("ghostMode", ghostMode);
            stealthStories = p.getBoolean("stealthStories", stealthStories);
            hidePhoneInSettings = p.getBoolean("hidePhoneInSettings", hidePhoneInSettings);
            blurAppInRecents = p.getBoolean("blurAppInRecents", blurAppInRecents);
            noReadReceipts = p.getBoolean("noReadReceipts", noReadReceipts);

            showIdInProfile = p.getBoolean("showIdInProfile", showIdInProfile);
            hideSeenTicks = p.getBoolean("hideSeenTicks", hideSeenTicks);
            disableAnimations = p.getBoolean("disableAnimations", disableAnimations);

            autoSaveVoice = p.getBoolean("autoSaveVoice", autoSaveVoice);
            autoSaveVideoNotes = p.getBoolean("autoSaveVideoNotes", autoSaveVideoNotes);

            blockTelegramAds = p.getBoolean("blockTelegramAds", blockTelegramAds);
            hideSponsoredMessages = p.getBoolean("hideSponsoredMessages", hideSponsoredMessages);

            openLastChat = p.getBoolean("openLastChat", openLastChat);
            debugLogs = p.getBoolean("debugLogs", debugLogs);
        } catch (Throwable ignore) {
        }
    }

    /** Bitta bayroqni yozib qo'yish (sozlamalar ekranidan). */
    public static void setFlag(String key, boolean value) {
        try {
            SharedPreferences p = ApplicationLoader.applicationContext.getSharedPreferences(PREFS, 0);
            p.edit().putBoolean(key, value).apply();
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
            }
        } catch (Throwable ignore) {
        }
    }
}
