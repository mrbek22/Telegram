package org.telegram.messenger;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AlfaGram — o'zbek lotin ↔ kirill o'giruvchi.
 * Interfeys satrlari (til yuklanganda) va xabar matnlari uchun ishlatiladi.
 * Format belgilari (%1$s, %d), teglar, havolalar va @username o'zgarmaydi.
 */
public class AlfaCyrillic {

    /** O'girilmaydigan bo'laklar: format belgilari, teglar, {..}, havolalar, @nik, un1/un2, brend nomi. */
    private static final Pattern PROTECTED = Pattern.compile(
            "%%|%(\\d+\\$)?[-#+ 0,(]*\\d*(\\.\\d+)?[a-zA-Z]|<[^>]*>|\\{[^}]*\\}|(https?|tg)://\\S+|@\\w+|\\bun\\d\\b|AlfaGram");

    private static final String APOS = "'‘’ʻʼ`";

    /** Interfeys kirillda ko'rsatilsinmi (LocaleController AlfaFeatures.loadPrefs'dan oldin ham chaqiradi). */
    public static boolean isUiEnabled() {
        try {
            return ApplicationLoader.applicationContext
                    .getSharedPreferences("alfa_features", 0).getBoolean("cyrillicUi", false);
        } catch (Throwable ignore) {
            return false;
        }
    }

    /** AlfaGram'ning o'z (kodda yozilgan) matnlari uchun: kirill yoqilgan bo'lsa o'giradi. */
    public static String ui(String s) {
        return AlfaFeatures.cyrillicUi && s != null ? toCyrillic(s) : s;
    }

    /** Til jadvalidagi barcha qiymatlarni kirillga o'giradi (joyida). */
    public static void convertLocaleValues(Map<String, String> values) {
        if (values == null) return;
        try {
            for (Map.Entry<String, String> e : values.entrySet()) {
                String v = e.getValue();
                if (v != null && !v.isEmpty()) {
                    e.setValue(toCyrillic(v));
                }
            }
        } catch (Throwable ignore) {
        }
    }

    public static boolean hasCyrillic(CharSequence s) {
        if (s == null) return false;
        for (int i = 0; i < s.length(); i++) {
            Character.UnicodeBlock b = Character.UnicodeBlock.of(s.charAt(i));
            if (b == Character.UnicodeBlock.CYRILLIC) return true;
        }
        return false;
    }

    public static boolean hasLatinLetters(CharSequence s) {
        if (s == null) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) return true;
        }
        return false;
    }

    // ── Lotin → Kirill ─────────────────────────────

    public static String toCyrillic(String s) {
        if (s == null || s.isEmpty()) return s;
        StringBuilder out = new StringBuilder(s.length());
        Matcher m = PROTECTED.matcher(s);
        int last = 0;
        while (m.find()) {
            out.append(latinChunkToCyrillic(s.substring(last, m.start())));
            out.append(m.group());
            last = m.end();
        }
        out.append(latinChunkToCyrillic(s.substring(last)));
        return out.toString();
    }

    private static boolean isApos(char c) {
        return APOS.indexOf(c) >= 0;
    }

    private static boolean isLatinLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean isVowel(char c) {
        return "aeiouAEIOU".indexOf(c) >= 0;
    }

    private static String latinChunkToCyrillic(String s) {
        StringBuilder b = new StringBuilder(s.length());
        int n = s.length();
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            char next = i + 1 < n ? s.charAt(i + 1) : 0;
            char prev = i > 0 ? s.charAt(i - 1) : 0;
            boolean up = Character.isUpperCase(c);
            // ikkinchi harf ham katta bo'lsa (SH, CH) — butun so'z katta deb olinadi
            boolean wordUp = up && (next == 0 || !isLatinLetter(next) || Character.isUpperCase(next));
            char lc = Character.toLowerCase(c);
            char ln = Character.toLowerCase(next);

            if ((lc == 'o' || lc == 'g') && isApos(next)) {
                b.append(cs(lc == 'o' ? "ў" : "ғ", up));
                i++;
                continue;
            }
            if (lc == 's' && ln == 'h') {
                b.append(cs("ш", up));
                i++;
                continue;
            }
            if (lc == 'c' && ln == 'h') {
                b.append(cs("ч", up));
                i++;
                continue;
            }
            // "yo'" — й + ў (Yo'q → Йўқ), "yo" birikmasi emas
            boolean yoApos = lc == 'y' && ln == 'o' && i + 2 < n && isApos(s.charAt(i + 2));
            if (lc == 'y' && !yoApos && (ln == 'o' || ln == 'u' || ln == 'a' || ln == 'e')) {
                String r = ln == 'o' ? "ё" : ln == 'u' ? "ю" : ln == 'a' ? "я" : "е";
                b.append(cs(r, up));
                i++;
                continue;
            }
            if (isApos(c)) {
                // tutuq belgisi: harflar orasida — ъ, aks holda o'zgarmaydi
                if (isLatinLetter(prev) && isLatinLetter(next)) {
                    b.append('ъ');
                } else {
                    b.append(c);
                }
                continue;
            }
            if (lc == 'e') {
                boolean wordStart = !isLatinLetter(prev) && !isApos(prev);
                b.append(cs(wordStart || isVowel(prev) ? "э" : "е", up));
                continue;
            }
            String r = single(lc);
            if (r == null) {
                b.append(c);
            } else {
                b.append(cs(r, up || wordUp && up));
            }
        }
        return b.toString();
    }

    private static String single(char c) {
        switch (c) {
            case 'a': return "а";
            case 'b': return "б";
            case 'c': return "ц";
            case 'd': return "д";
            case 'f': return "ф";
            case 'g': return "г";
            case 'h': return "ҳ";
            case 'i': return "и";
            case 'j': return "ж";
            case 'k': return "к";
            case 'l': return "л";
            case 'm': return "м";
            case 'n': return "н";
            case 'o': return "о";
            case 'p': return "п";
            case 'q': return "қ";
            case 'r': return "р";
            case 's': return "с";
            case 't': return "т";
            case 'u': return "у";
            case 'v': return "в";
            case 'w': return "в";
            case 'x': return "х";
            case 'y': return "й";
            case 'z': return "з";
            default: return null;
        }
    }

    private static String cs(String r, boolean upper) {
        return upper ? r.toUpperCase() : r;
    }

    // ── Kirill → Lotin ─────────────────────────────

    private static final HashMap<Character, String> C2L = new HashMap<>();

    static {
        String[][] map = {
                {"а", "a"}, {"б", "b"}, {"в", "v"}, {"г", "g"}, {"д", "d"}, {"е", "e"}, {"ё", "yo"},
                {"ж", "j"}, {"з", "z"}, {"и", "i"}, {"й", "y"}, {"к", "k"}, {"л", "l"}, {"м", "m"},
                {"н", "n"}, {"о", "o"}, {"п", "p"}, {"р", "r"}, {"с", "s"}, {"т", "t"}, {"у", "u"},
                {"ф", "f"}, {"х", "x"}, {"ц", "ts"}, {"ч", "ch"}, {"ш", "sh"}, {"щ", "sh"}, {"ъ", "’"},
                {"ы", "i"}, {"ь", ""}, {"э", "e"}, {"ю", "yu"}, {"я", "ya"}, {"ў", "o‘"}, {"қ", "q"},
                {"ғ", "g‘"}, {"ҳ", "h"},
        };
        for (String[] p : map) {
            C2L.put(p[0].charAt(0), p[1]);
        }
    }

    public static String toLatin(String s) {
        if (s == null || s.isEmpty()) return s;
        StringBuilder b = new StringBuilder(s.length() + 8);
        int n = s.length();
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            char lc = Character.toLowerCase(c);
            String r = C2L.get(lc);
            if (r == null) {
                b.append(c);
                continue;
            }
            // so'z boshidagi "е" — "ye"
            if (lc == 'е') {
                char prev = i > 0 ? s.charAt(i - 1) : 0;
                if (prev == 0 || !Character.isLetter(prev)) {
                    r = "ye";
                }
            }
            if (Character.isUpperCase(c) && !r.isEmpty()) {
                char next = i + 1 < n ? s.charAt(i + 1) : 0;
                boolean wordUp = next != 0 && Character.isLetter(next) && Character.isUpperCase(next);
                r = wordUp ? r.toUpperCase() : Character.toUpperCase(r.charAt(0)) + r.substring(1);
            }
            b.append(r);
        }
        return b.toString();
    }
}
