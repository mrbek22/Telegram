package org.telegram.messenger;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * AlfaGram — serverdagi admin paneldan "remote config" oladi.
 * Endpoint: https://alfagram.anonimbot.uz/api/config
 * Beradi: yangilanish banneri, maxsus tugmalar, Yandex reklama sozlamalari.
 * Config SharedPreferences'ga keshlanadi — internet bo'lmasa oxirgi holat ishlatiladi.
 */
public class AlfaConfig {

    public static final String CONFIG_URL = "https://alfagram.anonimbot.uz/api/config";
    private static final String PREFS = "alfa_config";

    public static int latestVersionCode;
    public static String latestVersionName = "";
    public static String updateMessage = "";
    public static String updateUrl = "";
    public static boolean forceUpdate;
    public static boolean adsEnabled;
    public static String yandexBlockId = "";
    public static final ArrayList<String[]> buttons = new ArrayList<>(); // {title, url}

    private static boolean cacheLoaded;
    private static boolean fetching;

    /** Keshdagi (oxirgi) configni o'qiydi — tezkor, internet kerak emas. */
    public static void loadCached() {
        if (cacheLoaded) {
            return;
        }
        cacheLoaded = true;
        try {
            SharedPreferences p = ApplicationLoader.applicationContext.getSharedPreferences(PREFS, 0);
            String json = p.getString("json", null);
            if (json != null) {
                parse(json);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    /** Serverdan yangi config oladi (fon oqimida). Tugagach onDone UI oqimida chaqiriladi. */
    public static void fetch(final Runnable onDone) {
        if (fetching) {
            return;
        }
        fetching = true;
        Utilities.globalQueue.postRunnable(() -> {
            String json = null;
            try {
                URL url = new URL(CONFIG_URL);
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setConnectTimeout(10000);
                c.setReadTimeout(10000);
                c.setRequestProperty("User-Agent", "AlfaGram");
                if (c.getResponseCode() == 200) {
                    BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) {
                        sb.append(line);
                    }
                    r.close();
                    json = sb.toString();
                    ApplicationLoader.applicationContext.getSharedPreferences(PREFS, 0)
                            .edit().putString("json", json).apply();
                }
                c.disconnect();
            } catch (Exception e) {
                FileLog.e(e);
            }
            final String result = json;
            AndroidUtilities.runOnUIThread(() -> {
                fetching = false;
                if (result != null) {
                    parse(result);
                }
                if (onDone != null) {
                    onDone.run();
                }
            });
        });
    }

    private static void parse(String json) {
        try {
            JSONObject o = new JSONObject(json);
            latestVersionCode = o.optInt("latest_version_code", 0);
            latestVersionName = o.optString("latest_version_name", "");
            updateMessage = o.optString("update_message", "");
            updateUrl = o.optString("update_url", "");
            forceUpdate = o.optBoolean("force_update", false);
            JSONObject ads = o.optJSONObject("ads");
            if (ads != null) {
                adsEnabled = ads.optBoolean("enabled", false);
                yandexBlockId = ads.optString("yandex_block_id", "");
            }
            buttons.clear();
            JSONArray arr = o.optJSONArray("buttons");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject b = arr.optJSONObject(i);
                    if (b != null) {
                        String title = b.optString("title", "");
                        String url = b.optString("url", "");
                        if (title.length() > 0 && url.length() > 0) {
                            buttons.add(new String[]{title, url});
                        }
                    }
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    /** Hozirgi o'rnatilgan APK versiya kodi (runtime, PackageManager orqali). */
    public static int currentVersionCode() {
        try {
            android.content.Context c = ApplicationLoader.applicationContext;
            return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionCode;
        } catch (Exception e) {
            return 0;
        }
    }

    /** Yangi versiya bormi? (config versiyasi > hozirgi APK versiyasi va xabar bor). */
    public static boolean hasUpdate() {
        return latestVersionCode > currentVersionCode()
                && updateMessage != null && updateMessage.length() > 0;
    }
}
