package org.telegram.messenger;

import android.content.SharedPreferences;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * AlfaGram — crash-reporter.
 * Ilova yiqilsa stack trace'ni saqlaydi va serverga (panelga) yuboradi.
 * Endpoint: https://alfagram.anonimbot.uz/api/crash
 */
public class AlfaCrash {

    private static final String CRASH_URL = "https://alfagram.anonimbot.uz/api/crash";
    private static final String PREFS = "alfa_crash";
    private static boolean installed;

    public static void install() {
        if (installed) {
            return;
        }
        installed = true;
        try {
            final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
                try {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    pw.println("thread=" + thread.getName());
                    pw.println("version=" + AlfaConfig.currentVersionCode());
                    pw.println("android=" + android.os.Build.VERSION.SDK_INT + " " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL);
                    throwable.printStackTrace(pw);
                    pw.flush();
                    String trace = sw.toString();
                    ApplicationLoader.applicationContext.getSharedPreferences(PREFS, 0)
                            .edit().putString("pending", trace).apply();
                } catch (Throwable ignore) {
                }
                if (prev != null) {
                    prev.uncaughtException(thread, throwable);
                }
            });
        } catch (Throwable ignore) {
        }
    }

    /** Saqlangan (oldingi) crash'ni serverga yuboradi. Startupda chaqiriladi. */
    public static void sendPending() {
        try {
            final SharedPreferences p = ApplicationLoader.applicationContext.getSharedPreferences(PREFS, 0);
            final String trace = p.getString("pending", null);
            if (trace == null || trace.length() == 0) {
                return;
            }
            Utilities.globalQueue.postRunnable(() -> {
                try {
                    URL url = new URL(CRASH_URL);
                    HttpURLConnection c = (HttpURLConnection) url.openConnection();
                    c.setRequestMethod("POST");
                    c.setConnectTimeout(10000);
                    c.setReadTimeout(10000);
                    c.setDoOutput(true);
                    c.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
                    byte[] body = trace.getBytes("UTF-8");
                    OutputStream os = c.getOutputStream();
                    os.write(body);
                    os.close();
                    int code = c.getResponseCode();
                    c.disconnect();
                    if (code == 200) {
                        p.edit().remove("pending").apply();
                    }
                } catch (Throwable ignore) {
                }
            });
        } catch (Throwable ignore) {
        }
    }
}
