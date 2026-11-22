package org.telegram.messenger;

import java.util.HashMap;
import java.util.Locale;

/**
 * AlfaGram — o'z matnlarini ilova tiliga moslash.
 * Manba matnlar kodda o'zbekcha (lotin) yozilgan; ilova tili o'zbekcha bo'lsa ular o'zgarmaydi
 * (kirill / yangi lotin AlfaCyrillic'da), rus tilida — ruscha, boshqa har qanday tilda — inglizcha.
 * Tarjimalar AlfaI18nDict'da: kalit — aynan o'zbekcha manba satr (format belgilari bilan birga).
 */
public final class AlfaI18n {

    public static final int UZ = 0;
    public static final int RU = 1;
    public static final int EN = 2;

    /** Kontekstli kalit ajratgichi: "kontekst::matn" (bir xil o'zbekcha so'z turli joyda turlicha tarjima qilinsa). */
    public static final String CTX_SEPARATOR = "::";

    private static volatile int mode = -1;
    private static volatile HashMap<String, String> ru;
    private static volatile HashMap<String, String> en;

    private AlfaI18n() {
    }

    /** LocaleController til qo'llanganda chaqiradi (ishga tushishda ham, til almashganda ham). */
    public static void onLocaleChanged(LocaleController.LocaleInfo info) {
        if (info == null) {
            return;
        }
        mode = detect(info.shortName, info.baseLangCode, info.pluralLangCode);
    }

    /** Joriy rejim: UZ, RU yoki EN. */
    public static int mode() {
        int m = mode;
        if (m >= 0) {
            return m;
        }
        // Til hali qo'llanmagan (juda erta) — tizim tilidan taxmin, keshlanmaydi
        try {
            return detect(Locale.getDefault().getLanguage(), null, null);
        } catch (Throwable ignore) {
            return UZ;
        }
    }

    public static boolean isUzbek() {
        return mode() == UZ;
    }

    static int detect(String shortName, String baseLangCode, String pluralLangCode) {
        String[] codes = {shortName, baseLangCode, pluralLangCode};
        for (String c : codes) {
            if (c == null || c.isEmpty()) {
                continue;
            }
            String l = c.toLowerCase(Locale.US);
            if (l.startsWith("uz")) {
                return UZ;
            }
            if (l.equals("ru") || l.startsWith("ru_") || l.startsWith("ru-")) {
                return RU;
            }
            // birinchi aniq kod (shortName) hal qiladi; bo'sh bo'lsa keyingisiga o'tiladi
            return EN;
        }
        return EN;
    }

    /** O'zbekcha manba satrni joriy tilga o'giradi (rejim UZ bo'lmasa). Tarjima topilmasa — manbaning o'zi. */
    public static String translate(String s, String ctx, int m) {
        if (s == null || m == UZ) {
            return s;
        }
        HashMap<String, String> d = dict(m);
        if (d == null) {
            return s;
        }
        if (ctx != null) {
            String v = d.get(ctx + CTX_SEPARATOR + s);
            if (v != null) {
                return v;
            }
        }
        String v = d.get(s);
        return v != null ? v : s;
    }

    private static HashMap<String, String> dict(int m) {
        if (ru == null || en == null) {
            synchronized (AlfaI18n.class) {
                if (ru == null || en == null) {
                    HashMap<String, String> r = new HashMap<>(2048);
                    HashMap<String, String> e = new HashMap<>(2048);
                    try {
                        AlfaI18nDict.fill(r, e);
                    } catch (Throwable t) {
                        FileLog.e(t);
                    }
                    ru = r;
                    en = e;
                }
            }
        }
        return m == RU ? ru : en;
    }
}
