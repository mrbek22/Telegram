package org.telegram.messenger;

import android.os.SystemClock;

import org.telegram.tgnet.TLRPC;

import java.util.Calendar;

/**
 * AlfaGram — chatlar ro'yxati sarlavhasida salomlashuv ("Xayrli tong, Davron ☀️"), 3 soniya, keyin yana
 * AlfaGram logotipi. Faqat ilovaga KIRILGANDA chiqadi: ilova yangidan ochilganda yoki kamida 30 soniya
 * boshqa ilovada turib qaytilganda, va o'sha paytda chatlar ro'yxati ochiq bo'lsa. Chatdan ro'yxatga
 * qaytganda chiqmaydi.
 */
public class AlfaGreeting {

    /** Shuncha vaqt boshqa ilovada turilsa, qaytish "ilovaga kirish" hisoblanadi (galereya/ruxsat oynalari emas). */
    private static final long MIN_BACKGROUND_MS = 30_000;
    /** Ilovaga kirgandan keyin ro'yxat shuncha vaqt ichida ochilishi kerak. */
    private static final long PENDING_WINDOW_MS = 2_500;
    /** Salomlashuv ko'rinib turadigan vaqt. */
    public static final long SHOW_MS = 3_000;

    private static long pausedAt;
    private static boolean everResumed;
    private static long pendingAt;

    /** LaunchActivity.onPause */
    public static void onAppPaused() {
        pausedAt = SystemClock.elapsedRealtime();
    }

    /** LaunchActivity.onResume (fragmentlar onResume'idan oldin) */
    public static void onAppResumed() {
        long now = SystemClock.elapsedRealtime();
        if (!everResumed || (pausedAt > 0 && now - pausedAt >= MIN_BACKGROUND_MS)) {
            pendingAt = now;
        }
        everResumed = true;
    }

    /** Chatlar ro'yxati ochilganda: salomlashuv kerakmi (bir marta qaytaradi). */
    public static boolean consume() {
        if (pendingAt == 0) {
            return false;
        }
        boolean fresh = SystemClock.elapsedRealtime() - pendingAt <= PENDING_WINDOW_MS;
        pendingAt = 0;
        return fresh;
    }

    public static CharSequence text(int account) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting;
        String emoji;
        if (hour >= 5 && hour < 11) {
            greeting = "Xayrli tong";
            emoji = "☀️";
        } else if (hour >= 11 && hour < 17) {
            greeting = "Xayrli kun";
            emoji = "🌤";
        } else if (hour >= 17 && hour < 22) {
            greeting = "Xayrli kech";
            emoji = "🌆";
        } else {
            greeting = "Xayrli tun";
            emoji = "🌙";
        }
        String name = null;
        try {
            TLRPC.User user = UserConfig.getInstance(account).getCurrentUser();
            if (user != null && user.first_name != null) {
                name = user.first_name.trim();
                int space = name.indexOf(' ');
                if (space > 0) {
                    name = name.substring(0, space);
                }
                if (name.length() > 14) {
                    name = name.substring(0, 13) + "…";
                }
            }
        } catch (Throwable ignore) {
        }
        String result = AlfaCyrillic.ui(greeting);
        if (name != null && name.length() > 0) {
            result += ", " + name;
        }
        return result + " " + emoji;
    }
}
