package org.telegram.messenger;

import android.content.SharedPreferences;

/**
 * AlfaGram — yashirin funksiyalar bayroqlari.
 * Sozlamalar ekranidan (AlfaSettingsActivity) yoki server config'idan boshqariladi.
 * loadPrefs() bir marta ilova ishga tushganda chaqirilishi kerak.
 */
public class AlfaFeatures {

    private static final String PREFS = "alfa_features";
    private static boolean loaded;

    /** Telegram'ning "sponsored" (reklama) xabarlarini bloklash. */
    public static boolean blockTelegramAds = true;

    /** Nusxa/forward taqiqlangan kanallardan ham saqlash/forward qilishga ruxsat. */
    public static boolean unrestrictedSave = true;

    /** Ghost mode — o'qildi/yozayotgan/online yubormaslik. */
    public static boolean ghostMode = false;

    /** Anonim story — story'ni ko'rsangiz egasi bilmaydi (ko'ruvchilarda chiqmaysiz). */
    public static boolean stealthStories = false;

    /** Screenshot cheklovini olib tashlash. */
    public static boolean allowScreenshots = true;

    /** Cheksiz pin (pin limitini o'chirish). */
    public static boolean unlimitedPins = true;

    /** Anti-delete — o'chirilgan xabarlarni saqlash. */
    public static boolean antiDelete = true;

    /** Tahrir tarixi — xabarning eski matnlarini saqlash. */
    public static boolean editHistory = true;

    /** "Yozdi, yubormadi" — yozayotgan holatidan chiqib xabar yubormaganlarni yozib borish. */
    public static boolean typingLog = true;

    /** Sozlamalarni SharedPreferences'dan yuklash (bir marta). */
    public static void loadPrefs() {
        if (loaded) return;
        loaded = true;
        try {
            SharedPreferences p = ApplicationLoader.applicationContext.getSharedPreferences(PREFS, 0);
            blockTelegramAds = p.getBoolean("blockTelegramAds", blockTelegramAds);
            unrestrictedSave = p.getBoolean("unrestrictedSave", unrestrictedSave);
            ghostMode = p.getBoolean("ghostMode", ghostMode);
            stealthStories = p.getBoolean("stealthStories", stealthStories);
            allowScreenshots = p.getBoolean("allowScreenshots", allowScreenshots);
            unlimitedPins = p.getBoolean("unlimitedPins", unlimitedPins);
            antiDelete = p.getBoolean("antiDelete", antiDelete);
            editHistory = p.getBoolean("editHistory", editHistory);
            typingLog = p.getBoolean("typingLog", typingLog);
        } catch (Throwable ignore) {
        }
    }

    /** Bitta bayroqni yozib qo'yish (sozlamalar ekranidan). */
    public static void setFlag(String key, boolean value) {
        try {
            SharedPreferences p = ApplicationLoader.applicationContext.getSharedPreferences(PREFS, 0);
            p.edit().putBoolean(key, value).apply();
            switch (key) {
                case "blockTelegramAds": blockTelegramAds = value; break;
                case "unrestrictedSave": unrestrictedSave = value; break;
                case "ghostMode": ghostMode = value; break;
                case "stealthStories": stealthStories = value; break;
                case "allowScreenshots": allowScreenshots = value; break;
                case "unlimitedPins": unlimitedPins = value; break;
                case "antiDelete": antiDelete = value; break;
                case "editHistory": editHistory = value; break;
                case "typingLog": typingLog = value; break;
            }
        } catch (Throwable ignore) {
        }
    }
}
