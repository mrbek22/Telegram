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
            "%%|%(\\d+\\$)?[-#+ 0,(]*\\d*(\\.\\d+)?[a-zA-Z]|<[^>]*>|\\{[^}]*\\}|(https?|tg)://\\S+|@\\w+|\\bun\\d\\b|AlfaGram|\\b(Ghost|PIN|TTL|ID|Premium|Stars|Business|Android|iOS|Wi-?Fi|Enter)\\b");

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
                    e.setValue(isDatePatternKey(e.getKey()) ? convertDatePattern(v) : toCyrillic(v));
                }
            }
        } catch (Throwable ignore) {
        }
    }

    /** SimpleDateFormat andozasi bo'lgan til kalitlari (HH:mm, EEE — harflari o'zgarmasligi shart). */
    private static boolean isDatePatternKey(String key) {
        if (key == null) return false;
        return key.startsWith("formatter") || key.startsWith("chatDate") || key.startsWith("chatFullDate")
                || key.startsWith("formatDateSchedule")
                || key.matches("(Send|Remind|Start|StartShort|Starts|Publish)(Today|Day|DayYear)At");
    }

    /** Andozada faqat '...' ichidagi oddiy matn o'giriladi. */
    private static String convertDatePattern(String v) {
        StringBuilder b = new StringBuilder(v.length());
        int i = 0;
        int n = v.length();
        while (i < n) {
            char c = v.charAt(i);
            if (c == '\'') {
                int end = v.indexOf('\'', i + 1);
                if (end < 0) {
                    b.append(v.substring(i));
                    break;
                }
                b.append('\'').append(toCyrillic(v.substring(i + 1, end))).append('\'');
                i = end + 1;
            } else {
                b.append(c);
                i++;
            }
        }
        return b.toString();
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

    /** Inglizcha so'zlarning o'zbekcha talaffuzi (o'girishdan oldin): Screenshot → Skrinshot → Скриншот. */
    private static final String[][] LOANWORDS = {
            {"Screenshot", "Skrinshot"}, {"screenshot", "skrinshot"},
            {"Stories", "Storis"}, {"stories", "storis"}, {"Story", "Stori"}, {"story", "stori"},
            {"caption", "izoh"}, {"Caption", "Izoh"}, {"Recent apps", "So'nggi ilovalar"},
            {"Enter", "Enter"}, {"Debug", "Debag"}, {"compact", "kompakt"}, {"silent", "ovozsiz"},
    };
    private static final Pattern LOANWORD_PATTERN;

    static {
        StringBuilder p = new StringBuilder("\\b(");
        for (int i = 0; i < LOANWORDS.length; i++) {
            if (i > 0) p.append('|');
            p.append(Pattern.quote(LOANWORDS[i][0]));
        }
        p.append(')');
        LOANWORD_PATTERN = Pattern.compile(p.toString());
    }

    private static String replaceLoanwords(String s) {
        Matcher m = LOANWORD_PATTERN.matcher(s);
        if (!m.find()) return s;
        StringBuffer sb = new StringBuffer();
        do {
            String w = m.group(1);
            String r = w;
            for (String[] pair : LOANWORDS) {
                if (pair[0].equals(w)) {
                    r = pair[1];
                    break;
                }
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(r));
        } while (m.find());
        m.appendTail(sb);
        return sb.toString();
    }

    public static String toCyrillic(String s) {
        if (s == null || s.isEmpty()) return s;
        s = replaceLoanwords(s);
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
            // chet so'zlar: animatsiya → анимация, funksiya → функция
            if (lc == 't' && ln == 's' && s.regionMatches(true, i + 2, "iya", 0, 3)) {
                b.append(cs("ц", up));
                i++;
                continue;
            }
            if (lc == 's' && Character.toLowerCase(prev) == 'k' && s.regionMatches(true, i + 1, "iya", 0, 3)) {
                b.append(cs("ц", up));
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
                    if (isVowel(next) || isVowel(prev)) {
                        b.append('ъ');
                    }
                    // undosh oldida (Telegram'ning) — egalik qo'shimchasi, belgi tushib qoladi
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
