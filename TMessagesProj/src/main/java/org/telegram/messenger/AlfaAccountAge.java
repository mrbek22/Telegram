package org.telegram.messenger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * AlfaGram — foydalanuvchi ID'si bo'yicha akkaunt ochilgan vaqtini TAXMINAN hisoblaydi.
 * ID'lar vaqt o'tishi bilan o'sib boradi; ma'lum nuqtalar orasida chiziqli interpolyatsiya qilinadi.
 * Natija aniq emas — faqat oy/yil darajasida ko'rsatiladi.
 */
public class AlfaAccountAge {

    // {user_id, unix vaqt (sekund)} — o'sish tartibida, ochiq manbalardagi taxminiy nuqtalar
    private static final long[][] POINTS = {
            {2768409L, 1383264000L},     // 2013-11
            {7679610L, 1388448000L},     // 2013-12
            {11538514L, 1391212000L},    // 2014-02
            {44634663L, 1399334000L},    // 2014-05
            {54845238L, 1411257000L},    // 2014-09
            {63263518L, 1414454000L},    // 2014-10
            {101260938L, 1425600000L},   // 2015-03
            {130029930L, 1441324000L},   // 2015-09
            {171295414L, 1457481000L},   // 2016-03
            {222021233L, 1465344000L},   // 2016-06
            {278941742L, 1473465000L},   // 2016-09
            {337808429L, 1487707000L},   // 2017-02
            {400169472L, 1501459000L},   // 2017-07
            {805158066L, 1563208000L},   // 2019-07
            {1974255900L, 1634000000L},  // 2021-10
            {5000000000L, 1640995200L},  // 2022-01
            {6000000000L, 1677628800L},  // 2023-03
            {7000000000L, 1706745600L},  // 2024-02
            {7500000000L, 1727740800L},  // 2024-10
            {8000000000L, 1748736000L},  // 2025-06
    };

    /** Taxminiy unix vaqt (sekund) yoki 0. */
    public static long estimate(long userId) {
        if (userId <= 0) return 0;
        if (userId <= POINTS[0][0]) return POINTS[0][1];
        for (int i = 1; i < POINTS.length; i++) {
            if (userId <= POINTS[i][0]) {
                long[] a = POINTS[i - 1];
                long[] b = POINTS[i];
                double f = (double) (userId - a[0]) / (double) (b[0] - a[0]);
                return a[1] + (long) ((b[1] - a[1]) * f);
            }
        }
        // oxirgi nuqtadan keyin — oxirgi ikki nuqta tezligi bilan davom ettiramiz
        long[] a = POINTS[POINTS.length - 2];
        long[] b = POINTS[POINTS.length - 1];
        double rate = (double) (b[1] - a[1]) / (double) (b[0] - a[0]);
        long t = b[1] + (long) ((userId - b[0]) * rate);
        long now = System.currentTimeMillis() / 1000L;
        return Math.min(t, now);
    }

    /** Masalan "~ 2022-yil oktabr" (kirill yoqilsa kirillda). */
    public static String format(long userId) {
        long t = estimate(userId);
        if (t <= 0) return null;
        String[] months = {"yanvar", "fevral", "mart", "aprel", "may", "iyun", "iyul", "avgust", "sentabr", "oktabr", "noyabr", "dekabr"};
        Date d = new Date(t * 1000L);
        int month = Integer.parseInt(new SimpleDateFormat("M", Locale.US).format(d)) - 1;
        String year = new SimpleDateFormat("yyyy", Locale.US).format(d);
        return AlfaCyrillic.ui("~ " + year + "-yil " + months[Math.max(0, Math.min(11, month))]);
    }
}
