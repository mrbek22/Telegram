package org.telegram.messenger;

/**
 * AlfaGram — yashirin funksiyalar bayroqlari.
 * Keyinchalik yashirin sozlamalar ekranidan boshqarilishi mumkin.
 */
public class AlfaFeatures {

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
}
